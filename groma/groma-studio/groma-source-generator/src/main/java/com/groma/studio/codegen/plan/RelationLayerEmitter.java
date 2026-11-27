package com.groma.studio.codegen.plan;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.annotation.SemanticTag;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.studio.ir.CompilePlanIR;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 第 3 层关联关系生成器（收割外部设计稿并按真实 API 适配；落实说明书 3.1 / 7.1 R07~R10）：
 * 将 {@code CompilePlanIR.associationTypes} 生成为 engine/relation/*RelationManager.java，
 * 封装双端正反向索引（BidirectionalTie）、多重性基数上限校验，
 * 经 ctx.emitRelationLinked / emitRelationUnlinked 派发 ra/ar/rv/vr/rx/xr 双视角事件。
 */
public final class RelationLayerEmitter {

    private final PlanModel model;

    public RelationLayerEmitter(PlanModel model) {
        this.model = model;
    }

    public List<JavaFile> emitAll() {
        List<JavaFile> files = new ArrayList<>();
        String relPkg = model.basePkg + ".engine.relation";
        for (CompilePlanIR.AssocTypeIR assoc : model.plan.associationTypes()) {
            PlanModel.TypeInfo left = model.types.get(assoc.leftTypeId());
            PlanModel.TypeInfo right = model.types.get(assoc.rightTypeId());
            if (left == null || right == null) {
                continue;
            }
            PlanModel.TypeInfo assocEntityInfo = model.types.get(assoc.entityId());
            String baseCode = assocEntityInfo != null
                    ? assocEntityInfo.code()
                    : model.naming.resolveEntityClassName(assoc.entityId(), "", assoc.zhName(), "RelationManager");
            String mgrSimpleName = baseCode + "RelationManager";

            int maxRightPerLeft = parseMaxMultiplicity(assoc.constraintRight());
            int maxLeftPerRight = parseMaxMultiplicity(assoc.constraintLeft());

            ClassName mgrClass = ClassName.get(relPkg, mgrSimpleName);
            ParameterizedTypeName superTie = ParameterizedTypeName.get(
                    ClassName.get(BidirectionalTie.class), ClassName.get(String.class), ClassName.get(String.class));

            TypeSpec.Builder tb = TypeSpec.classBuilder(mgrClass)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(superTie)
                    .addAnnotation(AnnotationSpec.builder(GromaMeta.class)
                            .addMember("id", "$S", assoc.entityId())
                            .addMember("zhName", "$S", assoc.zhName())
                            .addMember("kind", "$S", "ASSOCIATION_MANAGER")
                            .addMember("relations", "$S",
                                    assoc.labelLeftToRight() + " / " + assoc.labelRightToLeft())
                            .build())
                    // R09/R10 语义标签（收割 code3.html G3 方案）：正反向标签保留至运行期，
                    // 供 MCP 资源与文档读取器反射读取，不再只存在于画布与 Markdown
                    .addAnnotation(AnnotationSpec.builder(SemanticTag.class)
                            .addMember("relationId", "$S", assoc.entityId())
                            .addMember("targetType", "$S", assoc.rightTypeId())
                            .addMember("forwardLabel", "$S", assoc.labelLeftToRight())
                            .addMember("reverseLabel", "$S", assoc.labelRightToLeft())
                            .addMember("bidirectional", "$L", true)
                            .build())
                    .addJavadoc("$L 强类型双向关联管理器（R08/R10）\n"
                                    + "<p>左端：$L（$L，约束 $L）--($L)--> 右端：$L（$L，约束 $L）</p>\n"
                                    + "<p>基数上限：左端每实例最多 $L 个右端、右端每实例最多 $L 个左端；"
                                    + "bind 时经 ctx 派发 ra/ar（重指向 rv/vr），unbind 派发 rx/xr。</p>\n",
                            assoc.zhName(), left.zhName(), left.code(), assoc.constraintLeft(),
                            assoc.labelLeftToRight(), right.zhName(), right.code(), assoc.constraintRight(),
                            maxRightPerLeft, maxLeftPerRight)
                    .addField(FieldSpec.builder(int.class, "MAX_RIGHT_PER_LEFT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxRightPerLeft).build())
                    .addField(FieldSpec.builder(int.class, "MAX_LEFT_PER_RIGHT",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", maxLeftPerRight).build());

            // 原子建立关联：1..1 时替换旧指向（派发 rv + vr），基数越界记问题并拒绝，成功派发 ra + ar
            MethodSpec bind = MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "left")
                    .addParameter(right.entity(), "right")
                    .addParameter(PlanModel.WORLD_CTX, "ctx")
                    .beginControlFlow("if (left == null || right == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("$T prevRights = super.getRights(left.getInstanceId())",
                            ParameterizedTypeName.get(ClassName.get(java.util.Set.class), ClassName.get(String.class)))
                    .addStatement("boolean retargeting = !prevRights.isEmpty() && MAX_RIGHT_PER_LEFT == 1"
                            + " && !prevRights.contains(right.getInstanceId())")
                    .beginControlFlow("if (retargeting)")
                    .beginControlFlow("for (String oldRightId : java.util.List.copyOf(prevRights))")
                    .addStatement("super.unbind(left.getInstanceId(), oldRightId)")
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("if (super.getRights(left.getInstanceId()).size() >= MAX_RIGHT_PER_LEFT)")
                    .addStatement("ctx.recordPublicProblem($S + left.getInstanceId())",
                            "[" + assoc.zhName() + "] 左端关联数量超出上限: ")
                    .addStatement("return")
                    .endControlFlow()
                    .beginControlFlow("if (super.getLefts(right.getInstanceId()).size() >= MAX_LEFT_PER_RIGHT)")
                    .addStatement("ctx.recordPublicProblem($S + right.getInstanceId())",
                            "[" + assoc.zhName() + "] 右端关联数量超出上限: ")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("super.bind(left.getInstanceId(), right.getInstanceId())")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitRelationLinked($S, left, $S, right, $S, retargeting)",
                            assoc.entityId(), assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build();

            // 原子解除关联并派发主动 rx + 被动 xr
            MethodSpec unbind = MethodSpec.methodBuilder("unbind")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(left.entity(), "left")
                    .addParameter(right.entity(), "right")
                    .addParameter(PlanModel.WORLD_CTX, "ctx")
                    .beginControlFlow("if (left == null || right == null)")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("super.unbind(left.getInstanceId(), right.getInstanceId())")
                    .beginControlFlow("if (ctx != null)")
                    .addStatement("ctx.emitRelationUnlinked($S, left, $S, right, $S)",
                            assoc.entityId(), assoc.leftMemberId(), assoc.rightMemberId())
                    .endControlFlow()
                    .build();

            tb.addMethod(bind).addMethod(unbind);
            files.add(JavaFile.builder(relPkg, tb.build()).indent("    ").build());
        }
        return files;
    }

    /** 约束串取多重性上限："1"、"0..1"、"1..*"、"*"、"n"（上限无穷取 Integer.MAX_VALUE） */
    private static int parseMaxMultiplicity(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("*") || raw.contains("n") || raw.contains("N")) {
            return Integer.MAX_VALUE;
        }
        String trimmed = raw.trim();
        int idx = trimmed.lastIndexOf("..");
        String upper = idx >= 0 ? trimmed.substring(idx + 2).trim() : trimmed;
        try {
            return Math.max(1, Integer.parseInt(upper));
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
