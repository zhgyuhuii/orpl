package com.groma.studio.codegen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 画布图元/实体 ID 与五层 Java 源码行列位置的双向映射表（落实 NP-GEN-003，支持格式化后重算行号）
 */
public final class AstSourceMap {

    static {
        // 生成的五层源码含 record / switch 表达式等新语法，解析器必须配置 JAVA_21 语言级别
        StaticJavaParser.setConfiguration(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    public record SourceRange(
            String elementId,
            String relativeFilePath,
            int beginLine,
            int beginColumn,
            int endLine,
            int endColumn
    ) {}

    private final Map<String, SourceRange> idToSource = new LinkedHashMap<>();
    private final Map<String, String> fileLineToId = new LinkedHashMap<>();
    /** DAP 探针行：fireLineProbe("图元id") 调用行 → 图元 id（与注解映射分开存储） */
    private final Map<String, String> probeLineToId = new LinkedHashMap<>();

    static {
        // 生成代码含 instanceof 模式匹配等 Java 16+ 特性，解析器语言级别需对齐（默认级别会抛 ParseProblemException）
        com.github.javaparser.StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
    }

    /** 文件内首个已映射源码行（无映射返回 -1）；供调试器断点默认落点 */
    public int firstLineOfFile(String relativeFilePath) {
        String prefix = relativeFilePath + ":";
        int best = -1;
        for (String key : fileLineToId.keySet()) {
            if (key.startsWith(prefix)) {
                int line = Integer.parseInt(key.substring(prefix.length()));
                if (best < 0 || line < best) {
                    best = line;
                }
            }
        }
        return best;
    }

    /** 文件内首个探针行（fireLineProbe 调用行；无返回 -1）——调试器断点的正确落点 */
    public int firstProbeLineOfFile(String relativeFilePath) {
        String prefix = relativeFilePath + ":";
        int best = -1;
        for (String key : probeLineToId.keySet()) {
            if (key.startsWith(prefix)) {
                int line = Integer.parseInt(key.substring(prefix.length()));
                if (best < 0 || line < best) {
                    best = line;
                }
            }
        }
        return best;
    }

    /** 按画布图元 id 查生成源码起始行（未登记返回 -1）；供 DAP 探针把图元映射到断点行 */
    public int lineOf(String elementId) {
        SourceRange range = idToSource.get(elementId);
        if (range != null) {
            return range.beginLine;
        }
        // 探针行（fireLineProbe 映射）回查
        for (var e : probeLineToId.entrySet()) {
            if (e.getValue().equals(elementId)) {
                int line = Integer.parseInt(e.getKey().substring(e.getKey().lastIndexOf(':') + 1));
                return line;
            }
        }
        return -1;
    }

    /** 按画布图元 id 查生成源码相对路径（未登记返回空串） */
    public String fileOf(String elementId) {
        return fileLineToId.entrySet().stream()
                .filter(e -> e.getValue().equals(elementId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    /** 静态索引失败的生成源文件（含失败原因首行）——索引是 DAP 探针映射的增强，失败不阻断出码 */
    private final java.util.List<String> unindexedFiles = new java.util.ArrayList<>();

    public java.util.List<String> unindexedFiles() {
        return java.util.List.copyOf(unindexedFiles);
    }

    public void indexJavaSource(String relativeFilePath, String javaSourceCode) {
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(javaSourceCode);
        } catch (RuntimeException ex) {
            // 个别生成文件含无法静态解析的内容（如画布代码框转译残留的全角标点，模型 2 实证）：
            // 该文件的探针映射缺失，但不阻断整条出码流水线——ECJ 编译仍会给出精确诊断
            unindexedFiles.add(relativeFilePath + " <- " + String.valueOf(ex.getMessage()).lines().findFirst().orElse(""));
            return;
        }
        // DAP 源码级探针映射：execNode 里 fireLineProbe("画布图元id") 调用行 → 图元 id
        cu.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).forEach(call -> {
            if ("fireLineProbe".equals(call.getNameAsString()) && !call.getArguments().isEmpty()
                    && call.getArgument(0).isStringLiteralExpr()) {
                int probeLine = call.getBegin().map(pos -> pos.line).orElse(-1);
                String elementId = call.getArgument(0).asStringLiteralExpr().asString();
                if (probeLine > 0 && !elementId.isEmpty()) {
                    probeLineToId.putIfAbsent(relativeFilePath + ":" + probeLine, elementId);
                }
            }
        });
        cu.findAll(AnnotationExpr.class).forEach(ann -> {
            if ("GromaMeta".equals(ann.getNameAsString()) && ann instanceof NormalAnnotationExpr normalAnn) {
                Optional<String> idOpt = normalAnn.getPairs().stream()
                        .filter(p -> "id".equals(p.getNameAsString()))
                        .map(MemberValuePair::getValue)
                        .map(v -> v.asStringLiteralExpr().asString())
                        .findFirst();
                idOpt.ifPresent(elementId -> ann.getParentNode().ifPresent(parent -> parent.getRange().ifPresent(r -> {
                    SourceRange sr = new SourceRange(
                            elementId,
                            relativeFilePath,
                            r.begin.line,
                            r.begin.column,
                            r.end.line,
                            r.end.column
                    );
                    idToSource.put(elementId, sr);
                    for (int line = r.begin.line; line <= r.end.line; line++) {
                        fileLineToId.put(relativeFilePath + ":" + line, elementId);
                    }
                })));
            }
        });
    }

    /**
     * 直接登记一条画布图元 -> 源码行区间的映射（供在线调试器与单测在无 @GromaMeta 注解的源码上手动建立映射）
     */
    public void recordMapping(String elementId, String zhName, String relativeFilePath, int beginLine, int endLine) {
        SourceRange sr = new SourceRange(elementId, relativeFilePath, beginLine, 1, Math.max(beginLine, endLine), 1);
        idToSource.put(elementId, sr);
        for (int line = beginLine; line <= Math.max(beginLine, endLine); line++) {
            fileLineToId.put(relativeFilePath + ":" + line, elementId);
        }
    }

    public Optional<SourceRange> findSourceByElementId(String elementId) {
        return Optional.ofNullable(idToSource.get(elementId));
    }

    public Optional<String> findElementIdByFileAndLine(String relativeFilePath, int line) {
        return Optional.ofNullable(fileLineToId.get(relativeFilePath + ":" + line));
    }

    public Map<String, SourceRange> allMappings() {
        return Collections.unmodifiableMap(idToSource);
    }
}
