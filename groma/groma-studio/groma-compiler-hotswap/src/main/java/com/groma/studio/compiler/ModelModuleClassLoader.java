package com.groma.studio.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 模块版本专属隔离类加载器（Child-First 优先加载本模块及全部内部类/匿名类，落实 BE-CMP-003 & NP-BLD-004）。
 * <p>
 * 除字节码外还承载本模块的资源（运行时 ServiceLoader 登记、模型索引等）：资源查找同样是本模块优先，
 * 这样内存流水线（不落盘）里 {@code ServiceLoader.load(GromaModuleRuntime.class, loader)} 也能发现模块入口。
 * </p>
 */
public final class ModelModuleClassLoader extends ClassLoader implements AutoCloseable {

    private final String modelCode;
    private final String modelVersion;
    private final String packagePrefix;
    private volatile Map<String, byte[]> bytecodeStore;
    private volatile Map<String, byte[]> resourceStore;
    private final Map<String, Class<?>> definedClasses = new ConcurrentHashMap<>();
    private final ReentrantLock defineLock = new ReentrantLock();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ModelModuleClassLoader(
            String modelCode,
            String modelVersion,
            String packagePrefix,
            Map<String, byte[]> compiledBytecodes,
            ClassLoader parent) {
        this(modelCode, modelVersion, packagePrefix, compiledBytecodes, Map.of(), parent);
    }

    public ModelModuleClassLoader(
            String modelCode,
            String modelVersion,
            String packagePrefix,
            Map<String, byte[]> compiledBytecodes,
            Map<String, byte[]> moduleResources,
            ClassLoader parent) {
        super("groma-cl-" + modelCode + "-v" + modelVersion, parent);
        this.modelCode = modelCode;
        this.modelVersion = modelVersion;
        this.packagePrefix = packagePrefix.endsWith(".") ? packagePrefix : packagePrefix + ".";
        this.bytecodeStore = Map.copyOf(compiledBytecodes);
        this.resourceStore = Map.copyOf(moduleResources);
    }

    @Override
    public URL getResource(String name) {
        byte[] owned = moduleResource(name);
        if (owned != null) {
            return resourceUrl(name, owned);
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        byte[] owned = moduleResource(name);
        List<URL> urls = new ArrayList<>();
        if (owned != null) {
            urls.add(resourceUrl(name, owned));
        }
        for (URL url : Collections.list(super.getResources(name))) {
            urls.add(url);
        }
        return Collections.enumeration(urls);
    }

    @Override
    protected URL findResource(String name) {
        byte[] owned = moduleResource(name);
        return owned == null ? null : resourceUrl(name, owned);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        byte[] owned = moduleResource(name);
        return owned == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(resourceUrl(name, owned)));
    }

    private byte[] moduleResource(String name) {
        Map<String, byte[]> store = this.resourceStore;
        return name == null || store == null ? null : store.get(name);
    }

    private static URL resourceUrl(String name, byte[] bytes) {
        try {
            return new URL("groma", null, 0, "/" + name, new ByteArrayUrlStreamHandler(bytes));
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("无法构造模块资源 URL: " + name, ex);
        }
    }

    /** 内存字节资源的流处理器（模块资源不落盘，不走文件系统） */
    private static final class ByteArrayUrlStreamHandler extends URLStreamHandler {
        private final byte[] bytes;

        ByteArrayUrlStreamHandler(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new ByteArrayUrlConnection(url, bytes);
        }
    }

    /** 内存字节资源的 URL 连接 */
    private static final class ByteArrayUrlConnection extends URLConnection {
        private final byte[] bytes;

        ByteArrayUrlConnection(URL url, byte[] bytes) {
            super(url);
            this.bytes = bytes;
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public InputStream getInputStream() {
            connect();
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long getContentLengthLong() {
            return bytes.length;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Map<String, byte[]> store = this.bytecodeStore;
        if (store != null && (name.startsWith(packagePrefix) || store.containsKey(name))) {
            Class<?> c = findOrDefineModuleClass(name, store);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
        return super.loadClass(name, resolve);
    }

    private Class<?> findOrDefineModuleClass(String name, Map<String, byte[]> store) throws ClassNotFoundException {
        Class<?> hit = definedClasses.get(name);
        if (hit != null) {
            return hit;
        }
        byte[] bytes = store.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException("模型模块 [" + modelCode + "@" + modelVersion + "] 内不存在类: " + name);
        }
        defineLock.lock();
        try {
            Class<?> secondCheck = definedClasses.get(name);
            if (secondCheck != null) {
                return secondCheck;
            }
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                String pkgName = name.substring(0, lastDot);
                if (getDefinedPackage(pkgName) == null) {
                    definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }
            Class<?> defined = defineClass(name, bytes, 0, bytes.length);
            definedClasses.put(name, defined);
            return defined;
        } finally {
            defineLock.unlock();
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] owned = moduleResource(name);
        if (owned != null) {
            return new ByteArrayInputStream(owned);
        }
        if (name != null && name.endsWith(".class")) {
            String fqcn = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            Map<String, byte[]> store = this.bytecodeStore;
            if (store != null && store.containsKey(fqcn)) {
                return new ByteArrayInputStream(store.get(fqcn));
            }
        }
        return super.getResourceAsStream(name);
    }

    public String getModelCode() {
        return modelCode;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    /**
     * 当所有绑定该版本的旧会话全部结束时调用，清空内部字节码与类映射表协助 GC 回收 Metaspace（CK-BE-CMP-003-2）
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            this.bytecodeStore = Map.of();
            this.resourceStore = Map.of();
            this.definedClasses.clear();
        }
    }
}
