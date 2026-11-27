package com.groma.studio.compiler;

import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaTraceListener;
import com.groma.base.engine.GromaWorldContext;
import com.groma.base.engine.WorldSnapshot;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.studio.codegen.GeneratedModuleBundle;
import com.groma.studio.codegen.GromaModuleSourceGeneratorFacade;
import com.groma.studio.ir.CompilePlanIR;
import com.groma.studio.ir.GromaModuleIR;
import com.groma.studio.ir.GromaTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * code3.html 收割合成裁判（全链路）：手搭 CompilePlanIR 覆盖 R01 副父类组合委托（G1）、
 * R06 结构控制门点（G2）、R22/R21 复合条件表达式完整编译（G6）、R27 备注写入 Javadoc（G9）、
 * R09/R10 @SemanticTag 运行期注解——生成 → ECJ 编译 → 隔离类加载挂载 → 推演运行。
 * 语料 1/2 不触发这些特性（mixin=0、control=0、expression 空），本 fixture 是它们的唯一运行期裁判。
 */
class SyntheticFeatureHarvestEndToEndTest {

    private static final Pattern NP_R01_STDOUT = Pattern.compile("System\\.(out|err)\\.|printStackTrace\\(");
    private static final Pattern NP_R04_UNSEEDED_RNG = Pattern.compile("ThreadLocalRandom|new Random\\(");
    private static final Pattern NP_R05_SYNCHRONIZED = Pattern.compile("\\bsynchronized\\b");

    /** 找生成包里以指定简单类名结尾的源文件内容 */
    private static String sourceOf(GeneratedModuleBundle bundle, String simpleNameSuffix) {
        return bundle.getFiles().entrySet().stream()
                .filter(e -> e.getKey().replace('\\', '/').endsWith(simpleNameSuffix + ".java"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("生成包中未找到 " + simpleNameSuffix));
    }

    @Test
    void syntheticFeatureModelGeneratesCompilesMountsAndRuns() {
        GeneratedModuleBundle bundle = new GromaModuleSourceGeneratorFacade().generateModule(harvestIr(), Map.of());

        // 1. 确定性：同 IR 连续两次生成 SHA 一致
        GeneratedModuleBundle again = new GromaModuleSourceGeneratorFacade().generateModule(harvestIr(), Map.of());
        Assertions.assertEquals(bundle.computeBundleSha256(), again.computeBundleSha256(), "合成模型出码必须逐字节确定");

        // 2. 源码级断言：五类收割特性都有真实落点
        String deviceEntity = sourceOf(bundle, "DeviceEntity");
        assertTrue(deviceEntity.contains("refreshControlMask"), "R06：受控类型应生成 refreshControlMask 覆写");
        assertTrue(deviceEntity.contains("memberEnabled("), "R06：受控成员 setter 应生成使能门点");
        assertTrue(deviceEntity.contains("applyControlMasks("), "R06：掩码求值应写回基类缓存");

        String advancedEntity = sourceOf(bundle, "AdvancedUavEntity");
        assertTrue(advancedEntity.contains("mixinDeviceEntity"), "R01：副父类应生成组合委托字段");
        assertTrue(advancedEntity.contains("注意防水"), "R27：建模备注应写入实体 Javadoc");

        String chargeTask = bundle.getFiles().entrySet().stream()
                .filter(e -> e.getKey().replace('\\', '/').endsWith("ProcessTask.java"))
                .map(Map.Entry::getValue)
                .filter(code -> code.contains("充电"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到充电过程任务源文件"));
        assertTrue(chargeTask.contains("processEnableBits"), "R06：被控过程任务应覆写 processEnableBits");
        assertTrue(chargeTask.contains("||"), "G6：复合条件表达式 1&&(2||3) 应编译出 || 分支");
        assertTrue(chargeTask.contains(BuiltinJudgeRef), "G6：比较项应经 BuiltinTools.judge 安全求值");

        String relationManager = bundle.getFiles().entrySet().stream()
                .filter(e -> e.getKey().replace('\\', '/').contains("engine/relation/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("关联类型应生成 RelationManager"));
        assertTrue(relationManager.contains("SemanticTag"), "R09/R10：关联管理器应携带 @SemanticTag 运行期注解");

        // 3. 生成代码全部过静态红线
        for (Map.Entry<String, String> entry : bundle.getFiles().entrySet()) {
            if (!entry.getKey().endsWith(".java")) {
                continue;
            }
            String code = entry.getValue();
            Assertions.assertFalse(NP_R01_STDOUT.matcher(code).find(), "违反 NP-R01: " + entry.getKey());
            Assertions.assertFalse(NP_R04_UNSEEDED_RNG.matcher(code).find(), "违反 NP-R04: " + entry.getKey());
            Assertions.assertFalse(NP_R05_SYNCHRONIZED.matcher(code).find(), "违反 NP-R05: " + entry.getKey());
        }

        // 4. ECJ 内存编译：五层源码（含 mixin 委托 / 控制门点 / 复合条件）必须全部编译通过
        Map<String, byte[]> bytecodes = compileBundle(bundle);
        GromaModuleRuntime runtime = mountBundle(bundle, bytecodes);
        Assertions.assertEquals(1, runtime.scenes().size(), "合成模型应有一个运行场景");

        // 5. 运行期 R06 门点：电源状态=关机 时启动充电 → 首拍即被控制失能中断（paa）
        GromaSessionManager manager = new GromaSessionManager(null, 8);
        List<String> trace = new ArrayList<>();
        try {
            String sceneCode = runtime.scenes().get(0).sceneCode();
            String sessionId = manager.startSession(runtime, sceneCode, Map.of(), 20261128L, true, ctx -> {
                BaseSimEntity dev = ctx.spawn(runtime.newEntity("Device", "dev-1"));
                // 先写电量（默认开机、成员使能），再切关机——门点会拦截关机后的电量写入（见 dev-gate 幕）
                Assertions.assertEquals("", ctx.setInstanceValue("dev-1", "电量", "50.0"));
                Assertions.assertEquals("", ctx.setInstanceValue("dev-1", "电源状态", "@enum:POWER_OFF"),
                        "电源状态写入应成功");
                GromaProcessTask task = runtime.startManually("proc_charge", ctx, dev, Map.of());
                Assertions.assertNotNull(task, "手动启动应返回充电任务");
                ctx.launch(task);
                ctx.setTraceListener(new HarvestTrace(trace));
            });
            WorldSnapshot aborted = manager.stepSession(sessionId, 3);
            Assertions.assertTrue(trace.stream().anyMatch(line -> line.contains("paa")),
                    "关机状态下充电应在首拍被 R06 控制失能中断（paa），轨迹：\n" + String.join("\n", trace));
            Assertions.assertTrue(trace.stream().anyMatch(line -> line.contains("R06")),
                    "中断原因应标明 R06 控制失能");
            Assertions.assertTrue(aborted.activeProcesses().isEmpty(), "中断后不应有在途过程");

            // 受控成员门点：关机时写电量被静默忽略
            GromaSessionManager gate = new GromaSessionManager(null, 8);
            try {
                String gateSession = gate.startSession(runtime, sceneCode, Map.of(), 20261128L, true, ctx -> {
                    ctx.spawn(runtime.newEntity("Device", "dev-gate"));
                    Assertions.assertEquals("", ctx.setInstanceValue("dev-gate", "电源状态", "@enum:POWER_OFF"));
                    Assertions.assertEquals("", ctx.setInstanceValue("dev-gate", "电量", "77.0"));
                });
                WorldSnapshot snap = gate.stepSession(gateSession, 1);
                Object battery = snap.instances().get("dev-gate") == null
                        ? null : snap.instances().get("dev-gate").get("电量");
                Assertions.assertTrue(battery == null || !"77.0".equals(String.valueOf(battery)),
                        "关机时受控成员写入应被门点忽略，实际快照：" + snap.instances());
            } finally {
                gate.close();
            }

            // 6. 开机 + 复合启动条件成立：充电完整跑完（elapsed 5s @ 1000ms 步长 ≈ 5 拍）
            trace.clear();
            String okSession = manager.startSession(runtime, sceneCode, Map.of(), 20261128L, true, ctx -> {
                BaseSimEntity dev = ctx.spawn(runtime.newEntity("Device", "dev-2"));
                ctx.setInstanceValue("dev-2", "电量", "50.0");
                GromaProcessTask task = runtime.startManually("proc_charge", ctx, dev, Map.of());
                ctx.launch(task);
                ctx.setTraceListener(new HarvestTrace(trace));
            });
            manager.stepSession(okSession, 8);
            Assertions.assertTrue(trace.stream().noneMatch(line -> line.contains("paa")),
                    "开机状态下充电不应被中断，轨迹：\n" + String.join("\n", trace));
            Assertions.assertTrue(trace.stream().anyMatch(line -> line.contains("finish") || line.contains("成功")),
                    "充电应正常收尾");

            // 7. R01 mixin 运行期读写：副父类成员经委托路由持久化
            GromaSessionManager mixin = new GromaSessionManager(null, 8);
            try {
                String mixinSession = mixin.startSession(runtime, sceneCode, Map.of(), 20261128L, true, ctx -> {
                    ctx.spawn(runtime.newEntity("AdvancedUav", "adv-1"));
                    Assertions.assertEquals("", ctx.setInstanceValue("adv-1", "电量", "66.0"),
                            "副父类成员写入应经委托路由成功");
                });
                WorldSnapshot snap = mixin.stepSession(mixinSession, 1);
                Map<String, Object> adv = snap.instances().get("adv-1");
                Assertions.assertNotNull(adv, "副父类实例应进入快照");
                Assertions.assertTrue(String.valueOf(adv.get("电量")).contains("66"),
                        "副父类成员电量应经委托持久化，实际：" + adv);
            } finally {
                mixin.close();
            }
        } finally {
            manager.close();
        }
    }

    private static final String BuiltinJudgeRef = "judge(";

    private static Map<String, byte[]> compileBundle(GeneratedModuleBundle bundle) {
        CompilationResult result = new EcjInMemoryCompiler().compile(
                bundle.getModuleDirName(), bundle.getFiles(), bundle.getSourceMap(),
                SyntheticFeatureHarvestEndToEndTest.class.getClassLoader());
        List<String> errors = new ArrayList<>();
        for (CanvasCompileDiagnostic d : result.diagnostics()) {
            if (d.error()) {
                errors.add(d.relativeJavaFile() + ":" + d.javaLine() + " " + d.message());
            }
        }
        assertTrue(errors.isEmpty(), () -> "合成模型生成代码必须能被 ECJ 编译，错误 " + errors.size() + " 个:\n"
                + String.join("\n", errors.subList(0, Math.min(errors.size(), 40))));
        return Map.copyOf(result.classBytecodes());
    }

    private static GromaModuleRuntime mountBundle(GeneratedModuleBundle bundle, Map<String, byte[]> bytecodes) {
        Map<String, byte[]> resources = EcjInMemoryCompiler.extractResources(bundle.getFiles());
        ModelModuleClassLoader loader = new ModelModuleClassLoader(
                bundle.getModuleDirName(), "harvest", bundle.getBasePackage(), bytecodes, resources,
                SyntheticFeatureHarvestEndToEndTest.class.getClassLoader());
        List<GromaModuleRuntime> runtimes = new ArrayList<>();
        ServiceLoader<GromaModuleRuntime> services = ServiceLoader.load(GromaModuleRuntime.class, loader);
        services.forEach(runtimes::add);
        Assertions.assertEquals(1, runtimes.size(), "合成模块应登记且仅登记一个运行时入口");
        return runtimes.get(0);
    }

    /**
     * 收割特性合成模型：设备（R06 控制源与被控方）+ 无人机（主父类）+ 高级无人机（R01 mixin）、
     * 充电（R06 被控耗时过程，复合启动条件）、演练场景、无人机-设备双向关联（R09/R10）。
     */
    static GromaModuleIR harvestIr() {
        // 设备：电源状态（ENUM，控制源）+ 电量（受控成员）
        CompilePlanIR.TypeDefIR deviceType = new CompilePlanIR.TypeDefIR(
                "type_device", "设备", "", "", "TYPE", "",
                List.of(), List.of(),
                List.of(
                        new CompilePlanIR.MemberIR("m_power_state", "电源状态", "", "string", "ENUM",
                                "enum_power", false, "", "", "", "", "", "开机", "", GromaTypes.enumType("enum_power")),
                        new CompilePlanIR.MemberIR("m_battery", "电量", "", "double", "BASE",
                                "", false, "", "", "", "%", "", "", "", GromaTypes.DOUBLE)),
                List.of(), "",
                List.of(
                        // R06 语义：源状态激活时目标使能（开机 → 充电使能；关机 → 失能 paa 中断）
                        new CompilePlanIR.ControlRuleIR("m_power_state", List.of("开机"), "MEMBER", "m_battery", 0),
                        new CompilePlanIR.ControlRuleIR("m_power_state", List.of("开机"), "PROCESS", "proc_charge", 0)));

        CompilePlanIR.TypeDefIR uavType = new CompilePlanIR.TypeDefIR(
                "type_uav", "无人机", "", "", "TYPE", "",
                List.of(), List.of(),
                List.of(new CompilePlanIR.MemberIR("m_callsign", "编号", "", "string", "BASE",
                        "", false, "", "", "", "", "", "", "", GromaTypes.STRING)),
                List.of(), "", List.of());

        // R01 多继承：主父类 无人机 + 副父类 设备（mixin）
        CompilePlanIR.TypeDefIR advancedType = new CompilePlanIR.TypeDefIR(
                "type_advanced_uav", "高级无人机", "", "", "TYPE", "type_uav",
                List.of("type_device"), List.of(),
                List.of(new CompilePlanIR.MemberIR("m_payload", "载荷", "", "string", "BASE",
                        "", false, "", "", "", "", "", "", "", GromaTypes.STRING)),
                List.of("注意防水"), "", List.of());

        CompilePlanIR.TypeDefIR sceneType = new CompilePlanIR.TypeDefIR(
                "type_drill_scene", "演练场景", "", "", "SCENE", "",
                List.of(), List.of(), List.of(), List.of(), "", List.of());

        // G6：复合启动条件（电量 >= 10 && (电量 <= 90 || 电量 != 999)）
        CompilePlanIR.ConditionRuleIR compositeStart = new CompilePlanIR.ConditionRuleIR(
                new CompilePlanIR.MemberPathIR(
                        new CompilePlanIR.SelfIR(GromaTypes.entity("type_device")),
                        List.of("m_battery"), GromaTypes.DOUBLE),
                "",
                List.of(
                        new CompilePlanIR.ConditionIR(1, ">=", "10", "number"),
                        new CompilePlanIR.ConditionIR(2, "<=", "90", "number"),
                        new CompilePlanIR.ConditionIR(3, "!=", "999", "number")),
                "电量复合条件", "1&&(2||3)");

        CompilePlanIR.ProcessDefIR chargeProc = new CompilePlanIR.ProcessDefIR(
                "proc_charge", "proc_charge", "充电", "受 R06 控制的耗时过程", "TYPE", "type_device", "设备",
                true, false, false, 0, "FIXED", 5.0D, "s", null, null, "", null, null, null, null, null,
                1000L, "", "",
                List.of(), List.of(), List.of(), List.of(),
                List.of(compositeStart), List.of());

        CompilePlanIR.AssocTypeIR assoc = new CompilePlanIR.AssocTypeIR(
                "assoc_uav_device", "无人机与设备", "m_callsign", "编号", "type_uav",
                "m_battery", "电量", "type_device", "0..1", "0..*", "挂载", "被挂载");

        CompilePlanIR.ManualStartIR manualCharge = new CompilePlanIR.ManualStartIR(
                "type_device", List.of(), "proc_charge", List.of(), List.of());

        CompilePlanIR plan = new CompilePlanIR(
                1000L, List.of(deviceType, uavType, advancedType, sceneType), List.of(chargeProc),
                List.of(), List.of(assoc), List.of(), List.of(), List.of(manualCharge), List.of());

        GromaModuleIR.EnumIR powerEnum = new GromaModuleIR.EnumIR(
                "enum_power", "电源状态", List.of("开机", "关机"), "开机");
        return new GromaModuleIR("1.0", "harvest01", "harvest_demo", "收割验证模型", 1, "sha_harvest", 1000L,
                List.of(), List.of(powerEnum), List.of(), List.of(), List.of(), Map.of(), plan);
    }

    /** 收割裁判轨迹监听：记录过程启停、事件与问题 */
    private static final class HarvestTrace implements GromaTraceListener {
        private final List<String> lines;

        HarvestTrace(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public void onProcessStarted(GromaWorldContext ctx, GromaProcessTask task) {
            lines.add("start " + task.getProcessName());
        }

        @Override
        public void onProcessFinished(GromaWorldContext ctx, GromaProcessTask task) {
            lines.add("finish " + task.getProcessName() + " status=" + task.getStatus());
        }

        @Override
        public void onEvent(GromaWorldContext ctx, long tick, boolean silent, WorldSnapshot.SimEventRecord event) {
            if (!silent) {
                lines.add("event " + event.eventCode() + " " + event.detail());
            }
        }

        @Override
        public void onProblem(GromaWorldContext ctx, long tick, String message) {
            lines.add("problem " + message);
        }
    }
}
