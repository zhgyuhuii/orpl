package com.groma.studio.codegen.plan;

import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.CompilePlanIR.DefaultStartIR;
import com.groma.studio.ir.CompilePlanIR.InstanceIR;
import com.groma.studio.ir.CompilePlanIR.LocalVarIR;
import com.groma.studio.ir.CompilePlanIR.MemberIR;
import com.groma.studio.ir.CompilePlanIR.ObjectSpec;
import com.groma.studio.ir.CompilePlanIR.PortIR;
import com.groma.studio.ir.CompilePlanIR.ProcessDefIR;
import com.groma.studio.ir.CompilePlanIR.ScalarSpec;
import com.groma.studio.ir.CompilePlanIR.TriggerIR;
import com.groma.studio.ir.CompilePlanIR.TypeDefIR;
import com.groma.studio.ir.CompilePlanIR.ValueSpec;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.ir.GromaTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧版 12 分量 IR → 编译计划的适配器。
 * <p>
 * 手写或早期工具产出的 GromaModuleIR（没有第 13 分量 compilePlan）在这里被转成统一编译计划：
 * 领域类型的字段成为成员、ProcessIR 成为以 javaCode 为方法体的叶过程、场景成员成为场景对象成员，
 * 这样五层生成器只有一条生成路径。
 * </p>
 */
public final class LegacyPlanAdapter {

    private LegacyPlanAdapter() {
    }

    public static CompilePlanIR adapt(GromaModuleIR ir) {
        long stepMs = ir.finalStepMs() > 0L ? ir.finalStepMs() : CompilePlanIR.EMPTY.configuredStepMs();
        List<TypeDefIR> types = new ArrayList<>();
        for (GromaModuleIR.TypeIR type : safe(ir.domainTypes())) {
            List<MemberIR> members = new ArrayList<>();
            for (GromaModuleIR.FieldIR field : safe(type.fields())) {
                String placeholder = PlanModel.placeholderOf(field.fieldType());
                if ("ENUM".equals(field.typeCategory())) {
                    placeholder = GromaTypes.enumType(field.fieldType());
                } else if ("DEFINE".equals(field.typeCategory())) {
                    placeholder = GromaTypes.entity(field.fieldType());
                }
                if (field.collection()) {
                    placeholder = GromaTypes.list(placeholder);
                }
                String refTypeId = "ENUM".equals(field.typeCategory()) || "DEFINE".equals(field.typeCategory())
                        ? field.fieldType() : "";
                members.add(new MemberIR(field.entityId(), field.zhName(), "", field.fieldType(), field.typeCategory(),
                        refTypeId, field.collection(), "", nvl(field.defaultValue()), nvl(field.unit()),
                        nvl(field.minValue()), nvl(field.maxValue()),
                        "ENUM".equals(field.typeCategory()) ? nvl(field.defaultValue()) : "", "", placeholder));
            }
            types.add(new TypeDefIR(type.entityId(), type.zhName(), nvl(type.description()), nvl(type.essence()), "TYPE",
                    nvl(type.primaryParentEntityId()), members));
        }
        for (GromaModuleIR.SceneIR scene : safe(ir.scenes())) {
            List<MemberIR> members = new ArrayList<>();
            for (GromaModuleIR.SceneMemberIR member : safe(scene.initMembers())) {
                members.add(new MemberIR(member.memberEntityId(), member.zhName(), "", "string", "DEFINE",
                        member.typeEntityId(), false, "", "", "", "", "", "", "",
                        GromaTypes.entity(member.typeEntityId())));
            }
            types.add(new TypeDefIR(scene.entityId(), scene.zhName(), "", "", "SCENE", "", members));
        }

        List<ProcessDefIR> processDefs = new ArrayList<>();
        Map<String, String> defKeyByEntity = new LinkedHashMap<>();
        for (GromaModuleIR.TypeIR type : safe(ir.domainTypes())) {
            for (GromaModuleIR.ProcessIR proc : safe(type.methods())) {
                ProcessDefIR def = toDef(proc, "TYPE", type.entityId(), type.zhName(), stepMs);
                processDefs.add(def);
                defKeyByEntity.put(proc.entityId(), def.defKey());
            }
        }
        for (GromaModuleIR.SceneIR scene : safe(ir.scenes())) {
            for (GromaModuleIR.ProcessIR proc : safe(scene.sceneLevelProcesses())) {
                ProcessDefIR def = toDef(proc, "SCENE", scene.entityId(), scene.zhName(), stepMs);
                processDefs.add(def);
                defKeyByEntity.put(proc.entityId(), def.defKey());
            }
        }
        for (GromaModuleIR.ProcessIR proc : safe(ir.independentProcesses())) {
            if (defKeyByEntity.containsKey(proc.entityId())) {
                continue;
            }
            ProcessDefIR def = toDef(proc, "INDEPENDENT", "", "", stepMs);
            processDefs.add(def);
            defKeyByEntity.put(proc.entityId(), def.defKey());
        }

        List<DefaultStartIR> defaultStarts = new ArrayList<>();
        for (GromaModuleIR.SceneIR scene : safe(ir.scenes())) {
            for (String procEntityId : concat(scene.defaultStartOperatorIds(), scene.defaultStartProcessIds())) {
                String defKey = defKeyByEntity.get(procEntityId);
                if (defKey == null) {
                    defKey = procEntityId.startsWith("proc_") ? "legacy:" + procEntityId : defKeyByEntity.values().stream()
                            .filter(k -> k.endsWith(":" + procEntityId)).findFirst().orElse(null);
                }
                if (defKey != null) {
                    defaultStarts.add(new DefaultStartIR(scene.entityId(), List.of(), defKey, List.of(), List.of()));
                }
            }
        }

        List<TriggerIR> triggers = new ArrayList<>();
        for (GromaModuleIR.TypeIR type : safe(ir.domainTypes())) {
            for (GromaModuleIR.FieldIR field : safe(type.fields())) {
                for (GromaModuleIR.TriggerBindingIR binding : safe(field.eventTriggers())) {
                    String defKey = defKeyByEntity.get(binding.targetProcessEntityId());
                    if (defKey == null) {
                        continue;
                    }
                    triggers.add(new TriggerIR(type.entityId(), field.entityId(), binding.eventCode(), "", List.of(), null,
                            List.of(), defKey, List.of(), List.of(), List.of(), type.entityId(),
                            nvl(binding.conditionExpr())));
                }
            }
        }

        List<InstanceIR> instances = new ArrayList<>();
        for (GromaModuleIR.SceneIR scene : safe(ir.scenes())) {
            Map<String, ValueSpec> values = new LinkedHashMap<>();
            for (GromaModuleIR.SceneMemberIR member : safe(scene.initMembers())) {
                String override = member.initialFieldOverrides().get("defaultValue");
                values.put(member.memberEntityId(), override == null || override.isBlank()
                        ? new ObjectSpec(member.typeEntityId(), Map.of()) : new ScalarSpec(override, "string"));
            }
            instances.add(new InstanceIR("scene:" + scene.entityId(), scene.zhName(), scene.entityId(),
                    new ObjectSpec(scene.entityId(), values)));
        }
        return new CompilePlanIR(stepMs, types, processDefs, instances, List.of(), triggers, defaultStarts,
                List.of(), List.of());
    }

    private static ProcessDefIR toDef(GromaModuleIR.ProcessIR proc, String ownerKind, String ownerTypeId, String ownerZhName,
                                      long stepMs) {
        List<PortIR> params = new ArrayList<>();
        for (GromaModuleIR.ParamPortIR in : safe(proc.inputs())) {
            boolean written = safe(proc.outputs()).stream().anyMatch(o -> o.portId().equals(in.portId()));
            params.add(new PortIR(in.portId(), in.zhName(), written ? "IN_OUT" : "IN", in.required(), false,
                    placeholderOf(in.fieldType(), in.typeCategory())));
        }
        for (GromaModuleIR.ParamPortIR out : safe(proc.outputs())) {
            if (safe(proc.inputs()).stream().noneMatch(in -> in.portId().equals(out.portId()))) {
                params.add(new PortIR(out.portId(), out.zhName(), "OUT", false, false,
                        placeholderOf(out.fieldType(), out.typeCategory())));
            }
        }
        List<LocalVarIR> locals = new ArrayList<>();
        for (GromaModuleIR.FieldIR local : safe(proc.localTempMembers())) {
            locals.add(new LocalVarIR(local.entityId(), local.zhName(), PlanModel.placeholderOf(local.fieldType()),
                    nvl(local.defaultValue())));
        }
        int singleton = "GLOBAL_SINGLETON".equals(proc.singletonMode()) ? 3 : "OBJECT_SINGLETON".equals(proc.singletonMode()) ? 1 : 0;
        String durationType = nvl(proc.durationParamType()).isEmpty() ? "STEP" : proc.durationParamType();
        String distribution = switch (durationType) {
            case "UNIFORM" -> "uniform";
            case "NORMAL" -> "normal";
            case "EXPONENTIAL" -> "exponential";
            default -> "";
        };
        Double min = proc.timeMin();
        Double max = proc.timeMax();
        if ("uniform".equals(distribution)) {
            if (min == null) {
                min = 1.0D;
            }
            if (max == null) {
                max = Math.max(2.0D, proc.timeNormal());
            }
        }
        Long accuracyMs = proc.executionPeriodTicks() == null || proc.executionPeriodTicks() <= 1L ? null
                : proc.executionPeriodTicks() * stepMs;
        return new ProcessDefIR("legacy:" + proc.entityId(), proc.entityId(), proc.zhName(), nvl(proc.description()), ownerKind,
                ownerTypeId, ownerZhName, proc.timeConsuming(), proc.twinLocked(), proc.asyncMode(), singleton,
                "UNIFORM".equals(durationType) || "NORMAL".equals(durationType) || "EXPONENTIAL".equals(durationType)
                        ? "FIXED" : durationType,
                proc.timeNormal(), nvl(proc.timeUnit()).isEmpty() ? "s" : proc.timeUnit(), min, max, distribution,
                "uniform".equals(distribution) ? min : null, "uniform".equals(distribution) ? max : null,
                "normal".equals(distribution) ? (proc.timeNormal() > 0.0D ? proc.timeNormal() : 1.0D) : null,
                "normal".equals(distribution) ? 1.0D : null,
                "exponential".equals(distribution) ? (proc.timeNormal() > 0.0D ? proc.timeNormal() : 1.0D) : null,
                accuracyMs, nvl(proc.rawJavaCode()), nvl(proc.designViewId()), List.copyOf(params), List.copyOf(locals),
                List.of(), List.of(), List.of(), List.of());
    }

    private static String placeholderOf(String fieldType, String typeCategory) {
        if ("ENUM".equals(typeCategory)) {
            return GromaTypes.enumType(fieldType);
        }
        if ("DEFINE".equals(typeCategory)) {
            return GromaTypes.entity(fieldType);
        }
        return PlanModel.placeholderOf(fieldType);
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(safe(a));
        out.addAll(safe(b));
        return out;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String nvl(String text) {
        return text == null ? "" : text;
    }
}
