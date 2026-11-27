package com.groma.studio.compiler;

import com.groma.studio.codegen.AstSourceMap;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 基于 Eclipse JDT Core (ECJ) 的纯内存 Java 21 编译器（落实 NP-BLD-001 & NP-HOT-001）
 */
public final class EcjInMemoryCompiler {

    public static final String ECJ_COMPILER_TAG = "eclipse-ecj-java21";

    /**
     * 编译单个模型的全部内存 Java 源码
     *
     * @param moduleName        模块名（如 model-emergency-deduction）
     * @param javaFilesByRelPath 相对路径（如 src/main/java/com/groma/.../Foo.java）-> 源码文本
     * @param sourceMap         双向源码映射（用于将编译错误精确定位回画布图元）
     * @param parentClassLoader 底座与依赖类加载器（用于解析 Spring / GromaBase / JDK 类签名）
     */
    public CompilationResult compile(
            String moduleName,
            Map<String, String> javaFilesByRelPath,
            AstSourceMap sourceMap,
            ClassLoader parentClassLoader) {

        long startNs = System.nanoTime();
        Map<String, InMemoryUnit> unitsByBinaryName = new LinkedHashMap<>();
        Map<String, byte[]> resources = extractResources(javaFilesByRelPath);

        for (Map.Entry<String, String> entry : new TreeMap<>(javaFilesByRelPath).entrySet()) {
            String relPath = entry.getKey().replace('\\', '/');
            if (!relPath.endsWith(".java")) {
                continue;
            }
            String binaryName = toBinaryClassName(relPath);
            unitsByBinaryName.put(binaryName, new InMemoryUnit(relPath, binaryName, entry.getValue()));
        }

        if (unitsByBinaryName.isEmpty()) {
            long costMs = (System.nanoTime() - startNs) / 1_000_000L;
            return new CompilationResult(true, moduleName, ECJ_COMPILER_TAG, costMs, Map.of(), resources, List.of());
        }

        Map<String, byte[]> compiledClasses = new TreeMap<>();
        List<CanvasCompileDiagnostic> diagnostics = new ArrayList<>();

        INameEnvironment nameEnv = new MemoryAndLoaderNameEnvironment(unitsByBinaryName, parentClassLoader);
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.SIMPLIFIED_CHINESE);
        CompilerOptions options = buildJava21CompilerOptions();

        ICompilerRequestor requestor = result -> {
            if (result.hasProblems()) {
                String unitRelPath = (result.getCompilationUnit() instanceof InMemoryUnit imu)
                        ? imu.relativePath()
                        : String.valueOf(result.getFileName());
                for (CategorizedProblem prob : result.getProblems()) {
                    if (!prob.isError() && !prob.isWarning()) {
                        continue;
                    }
                    int line = Math.max(1, prob.getSourceLineNumber());
                    String mappedId = "";
                    int offsetInElem = 0;
                    if (sourceMap != null) {
                        Optional<String> eidOpt = sourceMap.findElementIdByFileAndLine(unitRelPath, line);
                        if (eidOpt.isPresent()) {
                            mappedId = eidOpt.get();
                            offsetInElem = sourceMap.findSourceByElementId(mappedId)
                                    .map(sr -> Math.max(1, line - sr.beginLine() + 1))
                                    .orElse(line);
                        }
                    }
                    diagnostics.add(new CanvasCompileDiagnostic(
                            prob.isError(),
                            moduleName,
                            unitRelPath,
                            line,
                            1,
                            mappedId,
                            offsetInElem,
                            prob.getMessage()
                    ));
                }
            }
            if (!result.hasErrors()) {
                for (ClassFile cf : result.getClassFiles()) {
                    StringBuilder fqcn = new StringBuilder();
                    char[][] compound = cf.getCompoundName();
                    for (int i = 0; i < compound.length; i++) {
                        if (i > 0) {
                            fqcn.append('.');
                        }
                        fqcn.append(compound[i]);
                    }
                    compiledClasses.put(fqcn.toString(), cf.getBytes());
                }
            }
        };

        Compiler ecjCompiler = new Compiler(
                nameEnv,
                DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                options,
                requestor,
                problemFactory
        );
        ecjCompiler.compile(unitsByBinaryName.values().toArray(new ICompilationUnit[0]));

        boolean hasError = diagnostics.stream().anyMatch(CanvasCompileDiagnostic::error);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        return new CompilationResult(
                !hasError,
                moduleName,
                ECJ_COMPILER_TAG,
                elapsedMs,
                hasError ? Map.of() : Map.copyOf(compiledClasses),
                resources,
                List.copyOf(diagnostics)
        );
    }

    /** 资源根目录前缀：其下的非 Java 文件原样进入模块类路径 */
    static final String RESOURCE_ROOT = "src/main/resources/";

    /**
     * 从生成包里挑出模块资源（{@code src/main/resources/} 下的非 Java 文件），
     * 键换成类路径相对路径（去掉资源根前缀），供隔离类加载器直接提供。
     */
    public static Map<String, byte[]> extractResources(Map<String, String> filesByRelPath) {
        Map<String, byte[]> resources = new TreeMap<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(filesByRelPath).entrySet()) {
            String relPath = entry.getKey().replace('\\', '/');
            if (!relPath.startsWith(RESOURCE_ROOT) || relPath.endsWith(".java") || entry.getValue() == null) {
                continue;
            }
            String classpathKey = relPath.substring(RESOURCE_ROOT.length());
            if (!classpathKey.isEmpty()) {
                resources.put(classpathKey, entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return Map.copyOf(resources);
    }

    private CompilerOptions buildJava21CompilerOptions() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_21);
        settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_21);
        settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_21);
        settings.put(CompilerOptions.OPTION_Encoding, "UTF-8");
        // 开启完整行号表、局部变量表、源文件名与方法参数名保留（支撑第六章 DAP 源码断点调试与 MCP 反射）
        settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_MethodParametersAttribute, CompilerOptions.GENERATE);
        return new CompilerOptions(settings);
    }

    static String toBinaryClassName(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String marker = "src/main/java/";
        int idx = normalized.indexOf(marker);
        String sub = idx >= 0 ? normalized.substring(idx + marker.length()) : normalized;
        if (sub.endsWith(".java")) {
            sub = sub.substring(0, sub.length() - ".java".length());
        }
        return sub.replace('/', '.');
    }

    private static final class InMemoryUnit implements ICompilationUnit {
        private final String relativePath;
        private final String binaryName;
        private final char[] contents;
        private final char[] mainTypeName;
        private final char[][] packageName;

        InMemoryUnit(String relativePath, String binaryName, String sourceText) {
            this.relativePath = relativePath;
            this.binaryName = binaryName;
            this.contents = sourceText.toCharArray();
            int lastDot = binaryName.lastIndexOf('.');
            String simple = lastDot >= 0 ? binaryName.substring(lastDot + 1) : binaryName;
            this.mainTypeName = simple.toCharArray();
            if (lastDot > 0) {
                String[] parts = binaryName.substring(0, lastDot).split("\\.");
                this.packageName = new char[parts.length][];
                for (int i = 0; i < parts.length; i++) {
                    this.packageName[i] = parts[i].toCharArray();
                }
            } else {
                this.packageName = new char[0][];
            }
        }

        String relativePath() {
            return relativePath;
        }

        @Override
        public char[] getContents() {
            return contents;
        }

        @Override
        public char[] getMainTypeName() {
            return mainTypeName;
        }

        @Override
        public char[][] getPackageName() {
            return packageName;
        }

        @Override
        public char[] getFileName() {
            return relativePath.toCharArray();
        }

        @Override
        public boolean ignoreOptionalProblems() {
            return true;
        }
    }

    private static final class MemoryAndLoaderNameEnvironment implements INameEnvironment {
        private final Map<String, InMemoryUnit> sourceUnits;
        private final ClassLoader classLoader;

        MemoryAndLoaderNameEnvironment(Map<String, InMemoryUnit> sourceUnits, ClassLoader classLoader) {
            this.sourceUnits = sourceUnits;
            this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        }

        @Override
        public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            return findTypeByName(joinCompound(compoundTypeName));
        }

        @Override
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            String pkg = joinCompound(packageName);
            String fqcn = pkg.isEmpty() ? new String(typeName) : pkg + "." + new String(typeName);
            return findTypeByName(fqcn);
        }

        private NameEnvironmentAnswer findTypeByName(String fqcn) {
            InMemoryUnit unit = sourceUnits.get(fqcn);
            if (unit != null) {
                return new NameEnvironmentAnswer(unit, null);
            }
            String resourcePath = fqcn.replace('.', '/') + ".class";
            try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    return null;
                }
                byte[] classBytes = is.readAllBytes();
                ClassFileReader reader = new ClassFileReader(classBytes, fqcn.toCharArray(), true);
                return new NameEnvironmentAnswer(reader, null);
            } catch (IOException | ClassFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            String parent = joinCompound(parentPackageName);
            String candidate = parent.isEmpty() ? new String(packageName) : parent + "." + new String(packageName);
            if (sourceUnits.containsKey(candidate)) {
                return false;
            }
            for (String unitFqcn : sourceUnits.keySet()) {
                if (unitFqcn.startsWith(candidate + ".")) {
                    return true;
                }
            }
            String clsResource = candidate.replace('.', '/') + ".class";
            if (classLoader.getResource(clsResource) != null) {
                return false;
            }
            return true;
        }

        @Override
        public void cleanup() {
            // 纯内存环境无需额外清理
        }

        private static String joinCompound(char[][] parts) {
            if (parts == null || parts.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append('.');
                }
                sb.append(parts[i]);
            }
            return sb.toString();
        }
    }
}
