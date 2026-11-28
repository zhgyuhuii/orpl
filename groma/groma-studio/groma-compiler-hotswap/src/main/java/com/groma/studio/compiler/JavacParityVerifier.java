package com.groma.studio.compiler;

import com.groma.studio.codegen.AstSourceMap;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 标准 JDK javac 交叉编译验证器（落实 CK-NP-BLD-001-2 & BE-CFG-002：自动读取当前进程 classpath，零硬编码路径）
 */
public final class JavacParityVerifier {

    public static final String JAVAC_COMPILER_TAG = "jdk-system-javac-21";

    public boolean isSystemJavacAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    public CompilationResult verifyWithJavac(
            String moduleName,
            Map<String, String> javaFilesByRelPath,
            AstSourceMap sourceMap) {

        long startNs = System.nanoTime();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("当前运行环境为精简 JRE（不含系统 javac），请使用 EcjInMemoryCompiler 或切换至完整 JDK 21 执行流水线复验");
        }

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager stdManager = compiler.getStandardFileManager(collector, Locale.SIMPLIFIED_CHINESE, StandardCharsets.UTF_8);
        Map<String, ByteArrayOutputStream> outputBuffers = new TreeMap<>();

        JavaFileManager memManager = new ForwardingJavaFileManager<>(stdManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(
                    Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
                outputBuffers.put(className, baos);
                return new SimpleJavaFileObject(
                        URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        return baos;
                    }
                };
            }
        };

        List<SourceMemObject> compilationUnits = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(javaFilesByRelPath).entrySet()) {
            String relPath = entry.getKey().replace('\\', '/');
            if (!relPath.endsWith(".java")) {
                continue;
            }
            String fqcn = EcjInMemoryCompiler.toBinaryClassName(relPath);
            compilationUnits.add(new SourceMemObject(relPath, fqcn, entry.getValue()));
        }

        List<String> options = List.of(
                "--release", "21",
                "-parameters",
                "-g:lines,vars,source",
                "-classpath", verificationClasspath()
        );

        Boolean ok = compiler.getTask(null, memManager, collector, options, null, compilationUnits).call();

        List<CanvasCompileDiagnostic> diagnostics = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
            boolean isErr = d.getKind() == Diagnostic.Kind.ERROR;
            String relPath = (d.getSource() instanceof SourceMemObject smo) ? smo.relativePath() : "";
            int line = (int) Math.max(1L, d.getLineNumber());
            int col = (int) Math.max(1L, d.getColumnNumber());
            String mappedId = "";
            int offsetLine = 0;
            if (sourceMap != null && !relPath.isEmpty()) {
                Optional<String> eidOpt = sourceMap.findElementIdByFileAndLine(relPath, line);
                if (eidOpt.isPresent()) {
                    mappedId = eidOpt.get();
                    offsetLine = sourceMap.findSourceByElementId(mappedId)
                            .map(sr -> Math.max(1, line - sr.beginLine() + 1))
                            .orElse(line);
                }
            }
            diagnostics.add(new CanvasCompileDiagnostic(
                    isErr,
                    moduleName,
                    relPath,
                    line,
                    col,
                    mappedId,
                    offsetLine,
                    d.getMessage(Locale.SIMPLIFIED_CHINESE)
            ));
        }

        Map<String, byte[]> classes = new TreeMap<>();
        if (Boolean.TRUE.equals(ok)) {
            outputBuffers.forEach((k, v) -> classes.put(k, v.toByteArray()));
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        return new CompilationResult(
                Boolean.TRUE.equals(ok),
                moduleName,
                JAVAC_COMPILER_TAG,
                elapsedMs,
                Map.copyOf(classes),
                EcjInMemoryCompiler.extractResources(javaFilesByRelPath),
                List.copyOf(diagnostics)
        );
    }

    /**
     * javac 对拍 classpath（code5.html 3.5 收割，闭环 13.4 挂账）：
     * 源码树/CI 形态 = 进程 {@code java.class.path}（零写死，BE-CMP-002 原语义）；
     * Spring Boot fat jar 形态 = 追加本 jar 的 {@code BOOT-INF/classes} 与 {@code BOOT-INF/lib/*.jar}
     * （嵌套依赖不在 java.class.path，原实现对拍恒失败——真机轮 13.4 第 3 项实证）。
     * jar 内条目经 {@code jar:file:…!/…} URL 直接喂 javac，不解包落盘。
     */
    static String verificationClasspath() {
        String base = System.getProperty("java.class.path", "");
        java.nio.file.Path selfJar = selfExecutableJar();
        if (selfJar == null) {
            return base;
        }
        StringBuilder cp = new StringBuilder(base);
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(selfJar.toFile())) {
            if (jar.getJarEntry("BOOT-INF/classes/") != null) {
                cp.append(java.io.File.pathSeparatorChar)
                        .append(selfJar.toUri() + "!/BOOT-INF/classes");
            }
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                    cp.append(java.io.File.pathSeparatorChar)
                            .append(selfJar.toUri() + "!/" + name);
                }
            }
        } catch (java.io.IOException ex) {
            // 非可执行 jar 或读取失败：退回原语义（java.class.path）
            return base;
        }
        return cp.toString();
    }

    /** 定位当前进程的可执行 fat jar（IDE/exploded 形态返回 null 走原语义） */
    private static java.nio.file.Path selfExecutableJar() {
        try {
            java.security.ProtectionDomain domain = JavacParityVerifier.class.getProtectionDomain();
            java.security.CodeSource source = domain.getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            java.nio.file.Path path = java.nio.file.Path.of(source.getLocation().toURI());
            if (java.nio.file.Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")
                    && path.toFile().length() > 1024 * 1024) {
                return path;
            }
            return null;
        } catch (RuntimeException | java.net.URISyntaxException ex) {
            return null;
        }
    }

    private static final class SourceMemObject extends SimpleJavaFileObject {
        private final String relativePath;
        private final String code;

        SourceMemObject(String relativePath, String fqcn, String code) {
            super(URI.create("string:///" + fqcn.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.relativePath = relativePath;
            this.code = code;
        }

        String relativePath() {
            return relativePath;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
