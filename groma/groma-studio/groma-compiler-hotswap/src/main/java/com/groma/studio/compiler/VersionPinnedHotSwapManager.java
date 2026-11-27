package com.groma.studio.compiler;

import com.groma.studio.codegen.GeneratedModuleBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 模块级增量编译与“版本随会话”热切换中心（落实 NP-HOT-001 & NP-HOT-002）
 */
public final class VersionPinnedHotSwapManager {

    private static final Logger log = LoggerFactory.getLogger(VersionPinnedHotSwapManager.class);

    /**
     * 单个模型特定版本的已加载句柄
     */
    public static final class MountedModelRevision {
        private final String modelCode;
        private final String modelVersion;
        private final String bundleSha256;
        private volatile ModelModuleClassLoader classLoader;
        private final AtomicInteger activeSessions = new AtomicInteger(0);
        private final AtomicBoolean retired = new AtomicBoolean(false);

        MountedModelRevision(String modelCode, String modelVersion, String bundleSha256, ModelModuleClassLoader classLoader) {
            this.modelCode = modelCode;
            this.modelVersion = modelVersion;
            this.bundleSha256 = bundleSha256;
            this.classLoader = classLoader;
        }

        public String modelCode() {
            return modelCode;
        }

        public String modelVersion() {
            return modelVersion;
        }

        public String bundleSha256() {
            return bundleSha256;
        }

        public Class<?> loadClass(String fqcn) throws ClassNotFoundException {
            ModelModuleClassLoader cl = this.classLoader;
            if (cl == null) {
                throw new IllegalStateException("模型版本 [" + modelCode + "@" + modelVersion + "] 已卸载回收");
            }
            return cl.loadClass(fqcn);
        }

        public int activeSessionCount() {
            return activeSessions.get();
        }

        /** 该修订的隔离类加载器（仅注册中心做清单读取用；可为 null 表示已回收） */
        public ClassLoader classLoaderOrNull() {
            return this.classLoader;
        }

        boolean isRetired() {
            return retired.get();
        }

        boolean tryAcquire() {
            while (true) {
                if (retired.get()) {
                    return false;
                }
                int cur = activeSessions.get();
                if (activeSessions.compareAndSet(cur, cur + 1)) {
                    if (retired.get()) {
                        releaseOne();
                        return false;
                    }
                    return true;
                }
            }
        }

        void retire() {
            if (retired.compareAndSet(false, true)) {
                cleanupIfDrained();
            }
        }

        void releaseOne() {
            int left = activeSessions.decrementAndGet();
            if (left <= 0 && retired.get()) {
                cleanupIfDrained();
            }
        }

        private void cleanupIfDrained() {
            if (activeSessions.get() <= 0) {
                ModelModuleClassLoader cl = this.classLoader;
                this.classLoader = null;
                if (cl != null) {
                    cl.close();
                    log.info("[HotSwap] 旧模型版本 {}@{} 无活跃会话，ClassLoader 已释放待 GC 回收", modelCode, modelVersion);
                }
            }
        }
    }

    /**
     * 会话持有的版本租约（放在任务结束 finally 块中调用 close() 自动归还引用计数）
     */
    public static final class SessionModelLease implements AutoCloseable {
        private final MountedModelRevision revision;
        private final AtomicBoolean released = new AtomicBoolean(false);

        SessionModelLease(MountedModelRevision revision) {
            this.revision = revision;
        }

        public String modelCode() {
            return revision.modelCode();
        }

        public String modelVersion() {
            return revision.modelVersion();
        }

        public Class<?> loadClass(String fqcn) throws ClassNotFoundException {
            return revision.loadClass(fqcn);
        }

        /** 该修订的隔离类加载器（含模块资源；宿主注册中心用它跑 ServiceLoader 发现运行时入口） */
        public ClassLoader loader() {
            return revision.classLoader;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                revision.releaseOne();
            }
        }
    }

    public record HotSwapReport(
            String modelCode,
            String modelVersion,
            boolean recompiled,
            boolean cacheHit,
            long compileMillis,
            List<CanvasCompileDiagnostic> diagnostics
    ) {}

    private final EcjInMemoryCompiler ecjCompiler;
    private final AtomicBytecodeCache bytecodeCache;
    private final ClassLoader baseParentClassLoader;
    private final ConcurrentHashMap<String, MountedModelRevision> latestByModelCode = new ConcurrentHashMap<>();

    public VersionPinnedHotSwapManager(
            EcjInMemoryCompiler ecjCompiler,
            AtomicBytecodeCache bytecodeCache,
            ClassLoader baseParentClassLoader) {
        this.ecjCompiler = Objects.requireNonNull(ecjCompiler);
        this.bytecodeCache = bytecodeCache;
        this.baseParentClassLoader = baseParentClassLoader != null
                ? baseParentClassLoader
                : Thread.currentThread().getContextClassLoader();
    }

    /**
     * 增量编译并热挂载/热更新单个模型子模块：
     * 若该模型源码摘要与当前最新挂载版本完全一致，直接 O(1) 跳过重编（落实 CK-NP-HOT-001-1）。
     */
    public HotSwapReport compileAndMountIncremental(
            String modelCode,
            String modelVersion,
            GeneratedModuleBundle bundle) throws IOException {

        String bundleSha = bundle.computeBundleSha256();
        MountedModelRevision current = latestByModelCode.get(modelCode);
        if (current != null && current.bundleSha256().equals(bundleSha) && current.modelVersion().equals(modelVersion)) {
            return new HotSwapReport(modelCode, modelVersion, false, true, 0L, List.of());
        }

        Map<String, byte[]> bytecodes = null;
        Map<String, byte[]> moduleResources = null;
        boolean cacheHit = false;
        long elapsedMs = 0L;

        if (bytecodeCache != null) {
            String cacheKey = bytecodeCache.computeCacheKey(bundle.getModuleDirName(), bundleSha);
            ReentrantLock compileLock = bytecodeCache.lockForKey(cacheKey);
            compileLock.lock();
            try {
                Optional<Map<String, byte[]>> cached = bytecodeCache.tryLoadVerified(cacheKey);
                if (cached.isPresent()) {
                    bytecodes = cached.get();
                    cacheHit = true;
                } else {
                    CompilationResult res = ecjCompiler.compile(
                            bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap(), baseParentClassLoader);
                    elapsedMs = res.elapsedMillis();
                    if (!res.success()) {
                        return new HotSwapReport(modelCode, modelVersion, true, false, elapsedMs, res.diagnostics());
                    }
                    bytecodes = res.classBytecodes();
                    moduleResources = res.resources();
                    bytecodeCache.putAtomically(cacheKey, bytecodes);
                }
            } finally {
                compileLock.unlock();
            }
        } else {
            CompilationResult res = ecjCompiler.compile(
                    bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap(), baseParentClassLoader);
            elapsedMs = res.elapsedMillis();
            if (!res.success()) {
                return new HotSwapReport(modelCode, modelVersion, true, false, elapsedMs, res.diagnostics());
            }
            bytecodes = res.classBytecodes();
            moduleResources = res.resources();
        }
        // 字节码缓存命中时资源缺失：从生成包重新抽取（资源不参与编译，抽取无副作用）
        if (moduleResources == null || moduleResources.isEmpty()) {
            moduleResources = EcjInMemoryCompiler.extractResources(bundle.getFiles());
        }

        ModelModuleClassLoader newLoader = new ModelModuleClassLoader(
                modelCode, modelVersion, bundle.getBasePackage(), bytecodes, moduleResources, baseParentClassLoader);
        MountedModelRevision nextRevision = new MountedModelRevision(modelCode, modelVersion, bundleSha, newLoader);

        MountedModelRevision previous = latestByModelCode.put(modelCode, nextRevision);
        if (previous != null) {
            // 将旧版本标记为退役：运行中的旧会话继续使用旧版本跑完，新会话立即切换到 nextRevision
            previous.retire();
        }

        log.info("[HotSwap] 模型 {} 已切换至版本 {}（recompiled={}, cacheHit={}, cost={}ms）",
                modelCode, modelVersion, !cacheHit, cacheHit, elapsedMs);
        return new HotSwapReport(modelCode, modelVersion, !cacheHit, cacheHit, elapsedMs, List.of());
    }

    /**
     * 为新创建的推演会话获取当前最新模型版本的隔离租约
     */
    public SessionModelLease acquireLatestSessionLease(String modelCode) {
        for (int attempt = 0; attempt < 5; attempt++) {
            MountedModelRevision rev = latestByModelCode.get(modelCode);
            if (rev == null) {
                throw new IllegalArgumentException("MODEL_NOT_FOUND: 模型未挂载 -> " + modelCode);
            }
            if (rev.tryAcquire()) {
                return new SessionModelLease(rev);
            }
        }
        throw new IllegalStateException("模型 [" + modelCode + "] 正在切换版本，请重试获取会话租约");
    }

    /** 当前各模型最新修订快照（注册中心列举已挂载热模型用；返回值不可变） */
    public List<MountedModelRevision> latestRevisions() {
        return List.copyOf(latestByModelCode.values());
    }

    /**
     * 卸载指定模型并触发无活跃会话后的类加载器回收
     */
    public void unmountModel(String modelCode) {
        MountedModelRevision removed = latestByModelCode.remove(modelCode);
        if (removed != null) {
            removed.retire();
        }
    }
}
