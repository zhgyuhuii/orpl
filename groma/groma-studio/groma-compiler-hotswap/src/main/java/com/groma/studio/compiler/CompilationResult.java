package com.groma.studio.compiler;

import java.util.List;
import java.util.Map;

/**
 * 模块编译结果（包含全量 .class 字节码表，含 $1 匿名类与嵌套内部类）。
 *
 * @param resources 模块的非类资源（类路径相对路径 -> 字节），如 META-INF/services 运行时登记、
 *                 META-INF/groma/GROMA_MODEL_INDEX.md；内存流水线靠它让 ServiceLoader 能发现模块运行时入口
 */
public record CompilationResult(
        boolean success,
        String moduleName,
        String compilerEngine,
        long elapsedMillis,
        Map<String, byte[]> classBytecodes,
        Map<String, byte[]> resources,
        List<CanvasCompileDiagnostic> diagnostics
) {
    /** 兼容构造器：不带模块资源的编译结果 */
    public CompilationResult(boolean success, String moduleName, String compilerEngine, long elapsedMillis,
                             Map<String, byte[]> classBytecodes, List<CanvasCompileDiagnostic> diagnostics) {
        this(success, moduleName, compilerEngine, elapsedMillis, classBytecodes, Map.of(), diagnostics);
    }
}
