package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.AdvanceAlongSectionRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Boolean;
import java.lang.Math;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Map;

/**
 * 瞬时过程状态机：沿区间前进（方圆过程原名：沿区间前进；宿主对象：列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "7rSlUlS0cSWjxGaiErg",
        zhName = "沿区间前进",
        kind = "PROCESS_TASK"
)
public final class TrainAdvanceAlongSectionProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：列车）
     */
    private final TrainEntity self;

    /**
     * 非空输入形参：地铁区间
     */
    private MetroSectionEntity metroSection;

    /**
     * 非空输入形参：目标车站
     */
    private StationEntity targetStation;

    /**
     * 非空输入形参：行驶间隔
     */
    private double travelInterval;

    /**
     * 双向形参：已行驶距离
     */
    private double travelledDistance;

    /**
     * 非空输入形参：是否沿里程增大方向
     */
    private boolean alongIncreasingMileage;

    /**
     * 双向形参：当前线路里程
     */
    private double currentLineMileage;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final AdvanceAlongSectionRuntimeBO frame = new AdvanceAlongSectionRuntimeBO();

    public TrainAdvanceAlongSectionProcessTask(GromaWorldContext ctx, TrainEntity self,
            MetroSectionEntity metroSection, StationEntity targetStation, double travelInterval,
            double travelledDistance, boolean alongIncreasingMileage, double currentLineMileage) {
        super(ctx, self, "dv:designView_1tvmsrr8ydvpCLGAoxr", "沿区间前进", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 沿区间前进");
        this.self = self;
        this.metroSection = metroSection;
        this.targetStation = targetStation;
        this.travelInterval = travelInterval;
        this.travelledDistance = travelledDistance;
        this.alongIncreasingMileage = alongIncreasingMileage;
        this.currentLineMileage = currentLineMileage;
    }

    private void setMetroSection(MetroSectionEntity value) {
        this.metroSection = value;
    }

    /**
     * 非空输入形参：地铁区间
     */
    public MetroSectionEntity getMetroSection() {
        return this.metroSection;
    }

    private void setTargetStation(StationEntity value) {
        this.targetStation = value;
    }

    /**
     * 非空输入形参：目标车站
     */
    public StationEntity getTargetStation() {
        return this.targetStation;
    }

    private void setTravelInterval(double value) {
        this.travelInterval = value;
    }

    /**
     * 非空输入形参：行驶间隔
     */
    public double getTravelInterval() {
        return this.travelInterval;
    }

    private void setTravelledDistance(double value) {
        this.travelledDistance = value;
    }

    /**
     * 双向形参：已行驶距离
     */
    public double getTravelledDistance() {
        return this.travelledDistance;
    }

    private void setAlongIncreasingMileage(boolean value) {
        this.alongIncreasingMileage = value;
    }

    /**
     * 非空输入形参：是否沿里程增大方向
     */
    public boolean getAlongIncreasingMileage() {
        return this.alongIncreasingMileage;
    }

    private void setCurrentLineMileage(double value) {
        this.currentLineMileage = value;
    }

    /**
     * 双向形参：当前线路里程
     */
    public double getCurrentLineMileage() {
        return this.currentLineMileage;
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
        return pc == 5 || pc == 6 || pc == 7;
    }

    @Override
    protected boolean execNode(int pc) {
        switch (pc) {
            case 0 -> {
                fireLineProbe("WhdSZuoQAVlvgpOZZtg");
                // 乘（arithmetic.multiply） a = 行驶间隔; b = 本对象.当前速度;
                double v0 = this.travelInterval;
                double v1 = self.getCurrentSpeed();
                double r = v0 * v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("29bEprA8E86iI0B1NYm");
                // 除（arithmetic.divide） a = 乘.c; b = 定值d9204193ac4c4b27a73967c3ced84686; 写出: c → 本步计划行驶距离;
                double v0 = frame.getOut0();
                double v1 = 60.0D;
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("zVEcAHXkwemT1do8Aoi");
                // 减（arithmetic.minus） a = 地铁区间.长度; b = 已行驶距离;
                double v0 = this.metroSection.getLength();
                double v1 = this.travelledDistance;
                double r = v0 - v1;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("lhwtt7crZPBpUQSb2x0");
                // 取小（arithmetic.min） a = 本步计划行驶距离; b = 减.c; 写出: c → 本步实际行驶距离;
                double v0 = frame.getStepPlannedTravelDistance();
                double v1 = frame.getOut2();
                double r = Math.min(v0, v1);
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("JvmswsutkVGuBpGWboX");
                // 等于（compare.eq） a = 是否沿里程增大方向; b = 沿里程增大方向;
                Boolean v0 = this.alongIncreasingMileage;
                Boolean v1 = frame.getAlongIncreasingMileage2();
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("kdyKQ5wPtY1VvYKrSnz");
                // 按值递增（automatic.incByVal） a = 当前线路里程; b = 本步实际行驶距离; 写出: a → 当前线路里程;
                double v0 = this.currentLineMileage;
                double v1 = frame.getStepActualTravelDistance();
                this.currentLineMileage = v0 + v1;
            }
            case 6 -> {
                fireLineProbe("cV97464eHRSf61Lnwpp");
                // 按值递减（automatic.decByVal） a = 当前线路里程; b = 本步实际行驶距离; 写出: a → 当前线路里程;
                double v0 = this.currentLineMileage;
                double v1 = frame.getStepActualTravelDistance();
                this.currentLineMileage = v0 - v1;
            }
            case 7 -> {
                fireLineProbe("SZZzUqo7W9vllaJPRaj");
                // 按值递增（automatic.incByVal） a = 已行驶距离; b = 本步实际行驶距离; 写出: a → 已行驶距离;
                double v0 = this.travelledDistance;
                double v1 = frame.getStepActualTravelDistance();
                this.travelledDistance = v0 + v1;
            }
            case 8 -> {
                fireLineProbe("5SEy5bPcUxVNE6EgmxY");
                // 除（arithmetic.divide） a = 已行驶距离; b = 地铁区间.长度; 写出: c → 行驶进度;
                double v0 = this.travelledDistance;
                double v1 = this.metroSection.getLength();
                double r = BuiltinTools.divide(v0, v1);
                frame.setOut8(r);
            }
            case 9 -> {
                fireLineProbe("crLnHuhmPi2wZgpNkwx");
                // 减（arithmetic.minus） a = 目标车站.位置.经度; b = 本对象.初始位置.经度;
                double v0 = this.targetStation.getPosition().getLongitude();
                double v1 = self.getInitialPosition().getLongitude();
                double r = v0 - v1;
                frame.setOut9(r);
            }
            case 10 -> {
                fireLineProbe("dKrNSRgLK2jyA32uZqJ");
                // 乘（arithmetic.multiply） a = 减.c; b = 行驶进度;
                double v0 = frame.getOut9();
                double v1 = frame.getTravelProgress();
                double r = v0 * v1;
                frame.setOut10(r);
            }
            case 11 -> {
                fireLineProbe("Dlw7VbQqvjojy9d1h3l");
                // 加（arithmetic.add） a = 乘.c; b = 本对象.初始位置.经度; 写出: c → 本对象.当前位置.经度;
                double v0 = frame.getOut10();
                double v1 = self.getInitialPosition().getLongitude();
                double r = v0 + v1;
                frame.setOut11(r);
            }
            case 12 -> {
                fireLineProbe("fOnxvqU6ycML8mtfmch");
                // 减（arithmetic.minus） a = 目标车站.位置.纬度; b = 本对象.初始位置.纬度;
                double v0 = this.targetStation.getPosition().getLatitude();
                double v1 = self.getInitialPosition().getLatitude();
                double r = v0 - v1;
                frame.setOut12(r);
            }
            case 13 -> {
                fireLineProbe("5te74Cj7uQLwXYagKrm");
                // 乘（arithmetic.multiply） a = 减.c; b = 行驶进度;
                double v0 = frame.getOut12();
                double v1 = frame.getTravelProgress();
                double r = v0 * v1;
                frame.setOut13(r);
            }
            case 14 -> {
                fireLineProbe("7DCbM9XC7oDkHTnFENv");
                // 加（arithmetic.add） a = 乘.c; b = 本对象.初始位置.纬度; 写出: c → 本对象.当前位置.纬度;
                double v0 = frame.getOut13();
                double v1 = self.getInitialPosition().getLatitude();
                double r = v0 + v1;
                frame.setOut14(r);
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
                frame.setStepPlannedTravelDistance(frame.getOut1());
                activate(2);
            }
            case 2 -> {
                activate(3);
            }
            case 3 -> {
                frame.setStepActualTravelDistance(frame.getOut3());
                activate(4);
            }
            case 4 -> {
                if (frame.getOut4()) {
                    activate(5);
                }
                if (!(frame.getOut4())) {
                    activate(6);
                }
            }
            case 5 -> {
                activate(7);
            }
            case 6 -> {
                activate(7);
            }
            case 7 -> {
                activate(8);
            }
            case 8 -> {
                frame.setTravelProgress(frame.getOut8());
                activate(9);
            }
            case 9 -> {
                activate(10);
            }
            case 10 -> {
                activate(11);
            }
            case 11 -> {
                self.getCurrentPosition().setLongitude(frame.getOut11(), ctx);
                activate(12);
            }
            case 12 -> {
                activate(13);
            }
            case 13 -> {
                activate(14);
            }
            case 14 -> {
                self.getCurrentPosition().setLatitude(frame.getOut14(), ctx);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.metroSection == null) return false;
        if (this.targetStation == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("metroSection", GromaValues.encode(this.metroSection));
        slots.put("targetStation", GromaValues.encode(this.targetStation));
        slots.put("travelInterval", GromaValues.encode(this.travelInterval));
        slots.put("travelledDistance", GromaValues.encode(this.travelledDistance));
        slots.put("alongIncreasingMileage", GromaValues.encode(this.alongIncreasingMileage));
        slots.put("currentLineMileage", GromaValues.encode(this.currentLineMileage));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.metroSection = (MetroSectionEntity) GromaValues.decode(slots.get("metroSection"), ctx);
        this.targetStation = (StationEntity) GromaValues.decode(slots.get("targetStation"), ctx);
        if (slots.get("travelInterval") != null) this.travelInterval = GromaValues.toDouble(slots.get("travelInterval"));
        if (slots.get("travelledDistance") != null) this.travelledDistance = GromaValues.toDouble(slots.get("travelledDistance"));
        if (slots.get("alongIncreasingMileage") != null) this.alongIncreasingMileage = GromaValues.toBoolean(slots.get("alongIncreasingMileage"));
        if (slots.get("currentLineMileage") != null) this.currentLineMileage = GromaValues.toDouble(slots.get("currentLineMileage"));
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
