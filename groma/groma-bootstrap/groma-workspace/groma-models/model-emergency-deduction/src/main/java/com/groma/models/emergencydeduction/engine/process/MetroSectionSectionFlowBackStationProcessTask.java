package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.SectionFlowBackStationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：区间水回流车站（方圆过程原名：区间水回流车站；宿主对象：地铁区间）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "Sq99s0zjy6vmnjVSRMz",
        zhName = "区间水回流车站",
        kind = "PROCESS_TASK"
)
public final class MetroSectionSectionFlowBackStationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：地铁区间）
     */
    private final MetroSectionEntity self;

    /**
     * 非空输入形参：车站
     */
    private StationEntity station;

    /**
     * 非空输入形参：本步过水量
     */
    private double stepFlowVolume;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final SectionFlowBackStationRuntimeBO frame = new SectionFlowBackStationRuntimeBO();

    public MetroSectionSectionFlowBackStationProcessTask(GromaWorldContext ctx,
            MetroSectionEntity self, StationEntity station, double stepFlowVolume) {
        super(ctx, self, "dv:designView_bXHqX2hJtSvKqqleN8E", "区间水回流车站", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 区间水回流车站");
        this.self = self;
        this.station = station;
        this.stepFlowVolume = stepFlowVolume;
    }

    private void setStation(StationEntity value) {
        this.station = value;
    }

    /**
     * 非空输入形参：车站
     */
    public StationEntity getStation() {
        return this.station;
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
                // 取相反数（arithmetic.reverse） a = 本步过水量;
                double v0 = this.stepFlowVolume;
                double r = -v0;
                frame.setOut0(r);
            }
            case 1 -> {
                // 调用瞬时子过程：积水变化 水量变化 = 取相反数.b;
                if (!spawnOrRun(1, new MetroSectionWaterloggingChangeProcessTask(ctx, self, frame.getOut0()))) return false;
            }
            case 2 -> {
                // 调用瞬时子过程：积水变化 水量变化值 = 本步过水量;
                if (!spawnOrRun(2, new StationWaterloggingChangeProcessTask(ctx, this.station, this.stepFlowVolume))) return false;
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
        if (this.station == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("station", GromaValues.encode(this.station));
        slots.put("stepFlowVolume", GromaValues.encode(this.stepFlowVolume));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.station = (StationEntity) GromaValues.decode(slots.get("station"), ctx);
        if (slots.get("stepFlowVolume") != null) this.stepFlowVolume = GromaValues.toDouble(slots.get("stepFlowVolume"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
