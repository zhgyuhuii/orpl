package com.groma.studio.compiler;

/**
 * 编译诊断信息（通过 AstSourceMap 将 Java 行号精准定位回画布上的模型过程/实体，落实 CK-NP-BLD-001-3 & CK-BE-CMP-001-2）
 */
public record CanvasCompileDiagnostic(
        boolean error,
        String moduleName,
        String relativeJavaFile,
        int javaLine,
        int javaColumn,
        String mappedElementId,
        int relativeOffsetLineInElement,
        String message
) {}
