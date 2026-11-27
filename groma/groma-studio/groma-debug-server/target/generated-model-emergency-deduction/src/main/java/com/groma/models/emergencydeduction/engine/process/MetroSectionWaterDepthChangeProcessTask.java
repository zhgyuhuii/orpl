package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.WaterDepthChangeRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：水深变化（方圆过程原名：水深变化；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "d3UYEBS88YPu8FoGckq",
        zhName = "水深变化",
        kind = "PROCESS_TASK"
)
public final class MetroSectionWaterDepthChangeProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final WaterDepthChangeRuntimeBO frame = new WaterDepthChangeRuntimeBO();

    public MetroSectionWaterDepthChangeProcessTask(GromaWorldContext ctx, MetroSectionEntity self) {
        super(ctx, self, "dv:designView_DwYZrRL0fsJBQ7H79np", "水深变化", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 水深变化");
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
                fireLineProbe("RjUwi1DVYUIOfdKbBl3");
                // 乘（arithmetic.multiply） a = 本对象.长度; b = 本对象.等效储水宽度;
                double v0 = self.getLength();
                double v1 = self.getEquivalentStorageWidth();
                double r = v0 * v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("17ggHkJVOz9tay2QVgK");
                // 除（arithmetic.divide） a = 本对象.积水量; b = 乘.c; 写出: c → 本对象.代表性水深;
                double v0 = self.getWaterVolume();
                double v1 = frame.getOut0();
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("rcb8q2OfyjB1Whk6o10");
                // 大于（compare.gt） a = 本对象.代表性水深; b = 定值8b06b50735c84f0e8868d121dc22d11a;
                double v0 = self.getRepresentativeDepth();
                double v1 = 0.0D;
                boolean r = v0 > v1;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("hUJn9MwuKTfuVYv1Wou");
                // 调用瞬时子过程：区间无积水
                if (!spawnOrRun(3, new MetroSectionSectionNoWaterloggingProcessTask(ctx, self))) return false;
            }
            case 4 -> {
                fireLineProbe("GuTCHtZlizEurs8D0As");
                // 调用瞬时子过程：区间积水
                if (!spawnOrRun(4, new MetroSectionSectionWaterloggingProcessTask(ctx, self))) return false;
            }
            case 5 -> {
                fireLineProbe("So7CSHmY52JpzCNjIOg");
                // 大于（compare.gt） a = 本对象.代表性水深; b = 本对象.危险水深;
                double v0 = self.getRepresentativeDepth();
                double v1 = self.getDangerDepth();
                boolean r = v0 > v1;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("eiPLRSqY6iTvJp1QE0y");
                // 赋值（assignment.assign） a = 允许放行; 写出: b → 本对象.是否放行;
                boolean v0 = true;
                boolean r = v0;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("rYibpUNjKnQeXEgbu6g");
                // 赋值（assignment.assign） a = 不允许放行; 写出: b → 本对象.是否放行;
                boolean v0 = false;
                boolean r = v0;
                frame.setOut7(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                activate(1);
            }
            case 1 -> {
                self.setRepresentativeDepth(frame.getOut1(), ctx);
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
            }
            case 4 -> {
                // 输出转换：本对象.积水状态 → 积水
                self.setWaterloggingState(WaterloggingStateEnum.WATERLOGGING, ctx);
                activate(5);
            }
            case 5 -> {
                if (!(frame.getOut5())) {
                    activate(6);
                }
                if (frame.getOut5()) {
                    activate(7);
                }
            }
            case 6 -> {
                self.setReleaseAllowed(frame.getOut6(), ctx);
            }
            case 7 -> {
                self.setReleaseAllowed(frame.getOut7(), ctx);
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
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
