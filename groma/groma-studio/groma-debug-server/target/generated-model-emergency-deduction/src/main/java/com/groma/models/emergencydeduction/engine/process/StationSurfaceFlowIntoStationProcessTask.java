package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.SurfaceFlowIntoStationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：地面水流入车站（方圆过程原名：地面水流入车站；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "d890lEbZtQNlk7z52Xa",
        zhName = "地面水流入车站",
        kind = "PROCESS_TASK"
)
public final class StationSurfaceFlowIntoStationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 非空输入形参：区域
     */
    private AreaEntity area;

    /**
     * 非空输入形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final SurfaceFlowIntoStationRuntimeBO frame = new SurfaceFlowIntoStationRuntimeBO();

    public StationSurfaceFlowIntoStationProcessTask(GromaWorldContext ctx, StationEntity self,
            AreaEntity area, double stepFlowVolume) {
        super(ctx, self, "dv:designView_yYvJXg9UMsYaNFITgoc", "地面水流入车站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 地面水流入车站");
        this.self = self;
        this.area = area;
        this.stepFlowVolume = stepFlowVolume;
    }

    private void setArea(AreaEntity value) {
        this.area = value;
    }

    /**
     * 非空输入形参：区域
     */
    public AreaEntity getArea() {
        return this.area;
    }

    private void setStepFlowVolume(double value) {
        this.stepFlowVolume = value;
    }

    /**
     * 非空输入形参：本步过水量
     */
    public double getStepFlowVolume() {
        return this.stepFlowVolume;
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
                fireLineProbe("gbiK1YliTxN84kmqyqQ");
                // 调用瞬时子过程：积水变化 水量变化值 = 本步过水量;
                if (!spawnOrRun(0, new StationWaterloggingChangeProcessTask(ctx, self, this.stepFlowVolume))) return false;
            }
            case 1 -> {
                fireLineProbe("KGsT6SL7LC9IoJkEKVv");
                // 取相反数（arithmetic.reverse） a = 本步过水量;
                double v0 = this.stepFlowVolume;
                double r = -v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("mx2Gaj2UYJXjtv6R8b7");
                // 调用瞬时子过程：积水变化 水量变化值 = 取相反数.b;
                if (!spawnOrRun(2, new AreaWaterloggingChangeProcessTask(ctx, this.area, frame.getOut1()))) return false;
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
                activate(2);
            }
            case 2 -> {
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.area == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("area", GromaValues.encode(this.area));
        slots.put("stepFlowVolume", GromaValues.encode(this.stepFlowVolume));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.area = (AreaEntity) GromaValues.decode(slots.get("area"), ctx);
        if (slots.get("stepFlowVolume") != null) this.stepFlowVolume = GromaValues.toDouble(slots.get("stepFlowVolume"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
