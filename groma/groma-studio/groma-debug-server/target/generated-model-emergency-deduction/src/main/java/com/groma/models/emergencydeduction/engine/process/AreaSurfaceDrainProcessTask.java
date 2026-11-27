package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.SurfaceDrainRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 耗时过程状态机：地表排水（方圆过程原名：地表排水；宿主对象：区域）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "Une96ICnH2KER5gu7LT",
        zhName = "地表排水",
        kind = "PROCESS_TASK"
)
public final class AreaSurfaceDrainProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(true, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：区域）
     */
    private final AreaEntity self;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final SurfaceDrainRuntimeBO frame = new SurfaceDrainRuntimeBO();

    public AreaSurfaceDrainProcessTask(GromaWorldContext ctx, AreaEntity self) {
        super(ctx, self, "dv:designView_1BqELDRmZFoiytzNKQ5", "地表排水", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 地表排水");
        this.self = self;
    }

    @Override
    protected boolean hasFlow() {
        return true;
    }

    @Override
    protected int[] entryNodes() {
        return new int[] {0};
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                fireLineProbe("WeK9FzegvhTI3mgQQak");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 当前步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("FqPNexNP8Y3JhMeiW4v");
                // 赋值（assignment.assign） a = 仿真时间.持续运行时长; 写出: b → 下一步进秒;
                double v0 = ctx.elapsedSeconds();
                double r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("8xY3p4IVys3xWGhAeTq");
                // 减（arithmetic.minus） a = 下一步进秒; b = 当前步进秒; 写出: c → 本步秒数;
                double v0 = frame.getNextStepSeconds();
                double v1 = frame.getCurrentStepSeconds();
                double r = v0 - v1;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("a5tAwdh5HGGywzmCfHY");
                // 除（arithmetic.divide） a = 本对象.地表排水能力; b = 定值954f91c96a574f7188d064b15b9a1f6e;
                double v0 = self.getSurfaceDrainCapacity();
                double v1 = 60.0D;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("3sO2NYPe6swnvhiiSSp");
                // 乘（arithmetic.multiply） a = 除.c; b = 本步秒数;
                double v0 = frame.getOut3();
                double v1 = frame.getStepSeconds();
                double r = v0 * v1;
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("bfLjkTnkbQusbWzwBas");
                // 取小（arithmetic.min） a = 乘.c; b = 本对象.积水量;
                double v0 = frame.getOut4();
                double v1 = self.getWaterVolume();
                double r = Math.min(v0, v1);
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("MoV57ThnQ07k0YhErHl");
                // 取相反数（arithmetic.reverse） a = 取小.c;
                double v0 = frame.getOut5();
                double r = -v0;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("ioqwKeEH3kcfqtbUP6S");
                // 调用瞬时子过程：积水变化 水量变化值 = 取相反数.b;
                if (!spawnOrRun(7, new AreaWaterloggingChangeProcessTask(ctx, self, frame.getOut6()))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                frame.setCurrentStepSeconds(frame.getOut0());
                activateAfter(1, 1L);
            }
            case 1 -> {
                frame.setNextStepSeconds(frame.getOut1());
                activate(2);
            }
            case 2 -> {
                frame.setStepSeconds(frame.getOut2());
                activate(3);
            }
            case 3 -> {
                activate(4);
            }
            case 4 -> {
                activate(5);
            }
            case 5 -> {
                activate(6);
            }
            case 6 -> {
                activate(7);
            }
            case 7 -> {
                activate(0);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (!"积水".equals(BaseSimEntity.enumZhLabel((Enum<?>) (self.getWaterloggingState())))) {
            return false;
        }
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
