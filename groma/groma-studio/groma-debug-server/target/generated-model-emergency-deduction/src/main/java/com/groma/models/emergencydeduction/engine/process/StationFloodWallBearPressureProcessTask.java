package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FloodWallBearPressureRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：挡水墙承受水压（方圆过程原名：挡水墙承受水压；宿主对象：车站）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "6gimbvYC5CskuHCzVFQ",
        zhName = "挡水墙承受水压",
        kind = "PROCESS_TASK"
)
public final class StationFloodWallBearPressureProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：车站）
     */
    private final StationEntity self;

    /**
     * 非空输入形参：本步秒数
     */
    private long stepSeconds;

    /**
     * 非空输入形参：区域水面高程
     */
    private double areaWaterLevel;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FloodWallBearPressureRuntimeBO frame = new FloodWallBearPressureRuntimeBO();

    public StationFloodWallBearPressureProcessTask(GromaWorldContext ctx, StationEntity self,
            long stepSeconds, double areaWaterLevel) {
        super(ctx, self, "dv:designView_rXSwECDiXYGPqX12Bir", "挡水墙承受水压", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 挡水墙承受水压");
        this.self = self;
        this.stepSeconds = stepSeconds;
        this.areaWaterLevel = areaWaterLevel;
    }

    private void setStepSeconds(long value) {
        this.stepSeconds = value;
    }

    /**
     * 非空输入形参：本步秒数
     */
    public long getStepSeconds() {
        return this.stepSeconds;
    }

    private void setAreaWaterLevel(double value) {
        this.areaWaterLevel = value;
    }

    /**
     * 非空输入形参：区域水面高程
     */
    public double getAreaWaterLevel() {
        return this.areaWaterLevel;
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
    protected boolean isSlowToolNode(int pc) {
        return pc == 3;
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                fireLineProbe("Q5DBrLr4nI7Divq5sa1");
                // 等于（compare.eq） a = 本对象.挡水墙状态; b = 挡水墙完好;
                FloodWallStateEnum v0 = self.getFloodWallState();
                FloodWallStateEnum v1 = frame.getFloodWallIntact();
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("s5hgPDGlyz7qFJ4SVMZ");
                // 加（arithmetic.add） a = 本对象.挡水墙顶高程; b = 本对象.失效超高阈值; 写出: c → 失效判定高程;
                double v0 = self.getFloodWallTopElevation();
                double v1 = self.getFailureThreshold();
                double r = v0 + v1;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("Xmcqc5qBK2rikkEV1VN");
                // 大于等于（compare.ge） a = 区域水面高程; b = 失效判定高程;
                double v0 = this.areaWaterLevel;
                double v1 = frame.getFailureJudgeElevation();
                boolean r = v0 >= v1;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("nwU2Vn3EmVZvnMksRLt");
                // 按值递增（automatic.incByVal） a = 本对象.超限累计时间; b = 本步秒数; 写出: a → 本对象.超限累计时间;
                double v0 = self.getOverlimitDuration();
                double v1 = this.stepSeconds;
                self.setOverlimitDuration(v0 + v1, ctx);
            }
            case 4 -> {
                fireLineProbe("482AzGDG2e7Yxo0T088");
                // 清空（assignment.clear） a = 本对象.超限累计时间; 写出: a → 本对象.超限累计时间;
                self.setOverlimitDuration(0.0D, ctx);
            }
            case 5 -> {
                fireLineProbe("heZRkWvc4Xv9z2rBrVV");
                // 大于等于（compare.ge） a = 本对象.超限累计时间; b = 本对象.失效持续时间;
                double v0 = self.getOverlimitDuration();
                double v1 = self.getFailureDuration();
                boolean r = v0 >= v1;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("0SOEMonm6Q1JZBWSgyN");
                // 调用瞬时子过程：挡水墙失效
                if (!spawnOrRun(6, new StationFloodWallFailureProcessTask(ctx, self))) return false;
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                if (frame.getOut0()) {
                    activate(1);
                }
            }
            case 1 -> {
                frame.setFailureJudgeElevation(frame.getOut1());
                activate(2);
            }
            case 2 -> {
                if (frame.getOut2()) {
                    activate(3);
                }
                if (!(frame.getOut2())) {
                    activate(4);
                }
            }
            case 3 -> {
                activate(5);
            }
            case 4 -> {
            }
            case 5 -> {
                if (frame.getOut5()) {
                    activate(6);
                }
            }
            case 6 -> {
                // 输出转换：挡水墙完好 → 失效
                frame.setFloodWallIntact(FloodWallStateEnum.FAILED);
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
        slots.put("stepSeconds", GromaValues.encode(this.stepSeconds));
        slots.put("areaWaterLevel", GromaValues.encode(this.areaWaterLevel));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        if (slots.get("stepSeconds") != null) this.stepSeconds = GromaValues.toLong(slots.get("stepSeconds"));
        if (slots.get("areaWaterLevel") != null) this.areaWaterLevel = GromaValues.toDouble(slots.get("areaWaterLevel"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
