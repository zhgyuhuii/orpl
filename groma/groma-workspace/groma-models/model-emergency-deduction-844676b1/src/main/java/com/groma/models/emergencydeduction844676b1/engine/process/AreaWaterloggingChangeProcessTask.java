package com.groma.models.emergencydeduction844676b1.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.bo.WaterloggingChangeRuntimeBO;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：积水变化（方圆过程原名：积水变化；宿主对象：区域）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "E5B8xzaV9xRnt2LDEgH",
        zhName = "积水变化",
        kind = "PROCESS_TASK"
)
public final class AreaWaterloggingChangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：区域）
     */
    private final AreaEntity self;

    /**
     * 非空输入形参：水量变化值
     */
    private double waterVolumeDelta;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final WaterloggingChangeRuntimeBO frame = new WaterloggingChangeRuntimeBO();

    public AreaWaterloggingChangeProcessTask(GromaWorldContext ctx, AreaEntity self,
            double waterVolumeDelta) {
        super(ctx, self, "dv:designView_175Y0QSMw786DGgXBXZ", "积水变化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 积水变化");
        this.self = self;
        this.waterVolumeDelta = waterVolumeDelta;
    }

    private void setWaterVolumeDelta(double value) {
        this.waterVolumeDelta = value;
    }

    /**
     * 非空输入形参：水量变化值
     */
    public double getWaterVolumeDelta() {
        return this.waterVolumeDelta;
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
                fireLineProbe("YXWj3YOuNFEW9b0qA1L");
                // 加（arithmetic.add） a = 水量变化值; b = 本对象.积水量; 写出: c → 可用水量;
                double v0 = this.waterVolumeDelta;
                double v1 = self.getWaterVolume();
                double r = v0 + v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("LlKdD3fJpwcS5pHUIAk");
                // 取大（arithmetic.max） a = 可用水量; b = 定值f1e5a899074a45b5be13d44c17334c05; 写出: c → 本对象.积水量;
                double v0 = frame.getAvailableWaterVolume();
                double v1 = 0L;
                double r = Math.max(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("vdgWvWtno5C3WM7bwUC");
                // 大于（compare.gt） a = 本对象.积水量; b = 定值f1e5a899074a45b5be13d44c17334c05;
                double v0 = self.getWaterVolume();
                double v1 = 0L;
                boolean r = v0 > v1;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("7z50VJiJu8n44vrkMuG");
                // 调用瞬时子过程：转换为无积水状态
                if (!spawnOrRun(3, new AreaToNoWaterloggingStateProcessTask(ctx, self))) return false;
            }
            case 4 -> {
                fireLineProbe("piJI2rqjMjQw1FQZiWI");
                // 调用瞬时子过程：转换为积水状态
                if (!spawnOrRun(4, new AreaToWaterloggedStateProcessTask(ctx, self))) return false;
            }
            case 5 -> {
                fireLineProbe("WG0iiAVGQsm0SytukJy");
                // 赋值（assignment.assign） a = 定值77575daf12b94b3397b54ebb7dad9a42; 写出: b → 本对象.积水深度;
                double v0 = 0.0D;
                double r = v0;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("0icOHJO5bYY3TZBRp43");
                // 除（arithmetic.divide） a = 本对象.积水量; b = 本对象.等效蓄水面积; 写出: c → 本对象.积水深度;
                double v0 = self.getWaterVolume();
                double v1 = self.getEquivalentStorageArea();
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut6(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                frame.setAvailableWaterVolume(frame.getOut0());
                activate(1);
            }
            case 1 -> {
                self.setWaterVolume(frame.getOut1(), ctx);
                activate(2);
            }
            case 2 -> {
                if (!(frame.getOut2())) {
                    activate(3);
                }
                if (frame.getOut2()) {
                    activate(4);
                }
            }
            case 3 -> {
                // 输出转换：本对象.积水状态 → 无积水
                self.setWaterloggingState(WaterloggingStateEnum.NO_WATERLOGGING, ctx);
                activate(5);
            }
            case 4 -> {
                // 输出转换：本对象.积水状态 → 积水
                self.setWaterloggingState(WaterloggingStateEnum.WATERLOGGING, ctx);
                activate(6);
            }
            case 5 -> {
                self.setWaterDepth(frame.getOut5(), ctx);
            }
            case 6 -> {
                self.setWaterDepth(frame.getOut6(), ctx);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("waterVolumeDelta", GromaValues.encode(this.waterVolumeDelta));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("waterVolumeDelta") != null) this.waterVolumeDelta = GromaValues.toDouble(slots.get("waterVolumeDelta"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
