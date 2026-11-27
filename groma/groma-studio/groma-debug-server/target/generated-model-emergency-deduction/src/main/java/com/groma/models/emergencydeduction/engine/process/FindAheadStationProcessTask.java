package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FindAheadStationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Map;

/**
 * 瞬时过程状态机：查找前方车站（方圆过程原名：查找前方车站；宿主对象：独立过程）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "oHh6xrqSMTxFFZGd07W",
        zhName = "查找前方车站",
        kind = "PROCESS_TASK"
)
public final class FindAheadStationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 非空输入形参：当前线路与列车
     */
    private LineWithTrainEntity currentLineWithTrain;

    /**
     * 输出形参：是否找到
     */
    private boolean found;

    /**
     * 输出形参：前方线路与车站
     */
    private LineWithStationEntity aheadLineWithStation;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FindAheadStationRuntimeBO frame = new FindAheadStationRuntimeBO();

    public FindAheadStationProcessTask(GromaWorldContext ctx,
            LineWithTrainEntity currentLineWithTrain, boolean found,
            LineWithStationEntity aheadLineWithStation) {
        super(ctx, null, "dv:designView_65ClRS3n9vUb3935COH", "查找前方车站", TIMING);
        this.currentLineWithTrain = currentLineWithTrain;
        this.found = found;
        this.aheadLineWithStation = aheadLineWithStation;
    }

    private void setCurrentLineWithTrain(LineWithTrainEntity value) {
        this.currentLineWithTrain = value;
    }

    /**
     * 非空输入形参：当前线路与列车
     */
    public LineWithTrainEntity getCurrentLineWithTrain() {
        return this.currentLineWithTrain;
    }

    private void setFound(boolean value) {
        this.found = value;
    }

    /**
     * 输出形参：是否找到
     */
    public boolean getFound() {
        return this.found;
    }

    private void setAheadLineWithStation(LineWithStationEntity value) {
        this.aheadLineWithStation = value;
    }

    /**
     * 输出形参：前方线路与车站
     */
    public LineWithStationEntity getAheadLineWithStation() {
        return this.aheadLineWithStation;
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
                fireLineProbe("SOThJzPIpqd7A6adGEY");
                // 等于（compare.eq） a = 当前线路与列车.沿里程增大方向; b = 里程增大方向;
                Boolean v0 = this.currentLineWithTrain.isAlongIncreasingMileage();
                Boolean v1 = true;
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("OxlZSC60MOY6W3K5rRH");
                // 赋值（assignment.assign） a = 里程减小方向; 写出: b → 方向系数;
                long v0 = -1L;
                long r = v0;
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("CjvnKhrPosEv4JEVrm7");
                // 赋值（assignment.assign） a = 里程增大方向; 写出: b → 方向系数;
                long v0 = 1L;
                long r = v0;
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("2VxiLpjLHsfabfdIox1");
                // 大于等于（compare.ge） a = 实例线路与车站集合.长度; b = 车站标识;
                double v0 = BuiltinTools.length((List<LineWithStationEntity>) ctx.namedCollection("ue1TzOuUYomKixhyqSU"));
                double v1 = frame.getStationIdentifier();
                boolean r = v0 >= v1;
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("TIFAEqMiV8fszoD7tEU");
                // 大于（compare.gt） a = 最近距离; b = 定值584ce308543241a398762b3685167615;
                double v0 = frame.getNearestDistance();
                double v1 = 0L;
                boolean r = v0 > v1;
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("KyIgTTNNCIWQrTXRDl4");
                // 按标识取元素（innerCollectionArray.getByIndex） 标识 = 车站标识; 写出: 元素 → 临时线路与车站;
                long v0 = frame.getStationIdentifier();
                LineWithStationEntity r = (LineWithStationEntity) BuiltinTools.getByIndex((List<LineWithStationEntity>) ctx.namedCollection("ue1TzOuUYomKixhyqSU"), v0);
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("jUhSXXz9OoMnVBbI1xd");
                // 赋值（assignment.assign） a = 未找到; 写出: b → 是否找到;
                boolean v0 = false;
                boolean r = v0;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("MxFIzmKsnE6rVnoy63r");
                // 赋值（assignment.assign） a = 找到; 写出: b → 是否找到;
                boolean v0 = true;
                boolean r = v0;
                frame.setOut7(r);
            }
            case 8 -> {
                fireLineProbe("qo3EKXWmgASKmrb5CzK");
                // 减（arithmetic.minus） a = 临时线路与车站.线路里程; b = 当前线路与列车.列车当前在线路上的里程;
                double v0 = frame.getTempLineWithStation().getLineMileage();
                double v1 = this.currentLineWithTrain.getTrainMileageOnLine();
                double r = v0 - v1;
                frame.setOut8(r);
            }
            case 9 -> {
                fireLineProbe("9J7Lb9xHuMnRWD2oIzL");
                // 乘（arithmetic.multiply） a = 减.c; b = 方向系数; 写出: c → 候选距离;
                double v0 = frame.getOut8();
                double v1 = frame.getDirectionFactor();
                double r = v0 * v1;
                frame.setOut9(r);
            }
            case 10 -> {
                fireLineProbe("SRe6sUFJmUzo2rD83ZF");
                // 大于（compare.gt） a = 候选距离; b = 定值584ce308543241a398762b3685167615;
                double v0 = frame.getCandidateDistance();
                double v1 = 0L;
                boolean r = v0 > v1;
                frame.setOut10(r);
            }
            case 11 -> {
                fireLineProbe("IDUTDpQVXbyWPLmcaGS");
                // 递增（automatic.inc） a = 车站标识; 写出: a → 车站标识;
                long v0 = frame.getStationIdentifier();
                frame.setStationIdentifier(v0 + 1L);
            }
            case 12 -> {
                fireLineProbe("2fXlTNGYqqrLUNZ9D8K");
                // 等于（compare.eq） a = 最近距离; b = 定值584ce308543241a398762b3685167615;
                Double v0 = frame.getNearestDistance();
                Long v1 = 0L;
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut12(r);
            }
            case 13 -> {
                fireLineProbe("lg7Ce8mMojX6LKwsPPR");
                // 赋值（assignment.assign） a = 候选距离; 写出: b → 最近距离;
                double v0 = frame.getCandidateDistance();
                double r = v0;
                frame.setOut13(r);
            }
            case 14 -> {
                fireLineProbe("23FUj4GptpqrFQvhyIg");
                // 小于（compare.lt） a = 候选距离; b = 最近距离;
                double v0 = frame.getCandidateDistance();
                double v1 = frame.getNearestDistance();
                boolean r = v0 < v1;
                frame.setOut14(r);
            }
            case 15 -> {
                fireLineProbe("UZbEp4GHc4NixTcZK5k");
                // 赋值（assignment.assign） a = 临时线路与车站; 写出: b → 前方线路与车站;
                LineWithStationEntity v0 = frame.getTempLineWithStation();
                LineWithStationEntity r = v0.copy(ctx);
                frame.setOut15(r);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
        return true;
    }

    @Override
    protected void nodeDone(int pc) {
        switch (pc) {
            case 0 -> {
                if (!(frame.getOut0())) {
                    activate(1);
                }
                if (frame.getOut0()) {
                    activate(2);
                }
            }
            case 1 -> {
                frame.setDirectionFactor(frame.getOut1());
                activate(3);
            }
            case 2 -> {
                frame.setDirectionFactor(frame.getOut2());
                activate(3);
            }
            case 3 -> {
                if (!(frame.getOut3())) {
                    activate(4);
                }
                if (frame.getOut3()) {
                    activate(5);
                }
            }
            case 4 -> {
                if (!(frame.getOut4())) {
                    activate(6);
                }
                if (frame.getOut4()) {
                    activate(7);
                }
            }
            case 5 -> {
                frame.setTempLineWithStation(frame.getOut5());
                activate(8);
            }
            case 6 -> {
                this.found = frame.getOut6();
            }
            case 7 -> {
                this.found = frame.getOut7();
            }
            case 8 -> {
                activate(9);
            }
            case 9 -> {
                frame.setCandidateDistance(frame.getOut9());
                activate(10);
            }
            case 10 -> {
                if (!(frame.getOut10())) {
                    activate(11);
                }
                if (frame.getOut10()) {
                    activate(12);
                }
            }
            case 11 -> {
                activate(3);
            }
            case 12 -> {
                if (frame.getOut12()) {
                    activate(13);
                }
                if (!(frame.getOut12())) {
                    activate(14);
                }
            }
            case 13 -> {
                frame.setNearestDistance(frame.getOut13());
                activate(15);
            }
            case 14 -> {
                if (frame.getOut14()) {
                    activate(13);
                }
                if (!(frame.getOut14())) {
                    activate(11);
                }
            }
            case 15 -> {
                this.aheadLineWithStation = frame.getOut15();
                activate(11);
            }
            default -> throw new IllegalStateException("未知的过程节点: pc=" + pc);
        }
    }

    @Override
    protected boolean canStart() {
        if (this.currentLineWithTrain == null) return false;
        return true;
    }

    @Override
    protected void exportSlots(Map<String, Object> slots) {
        slots.put("currentLineWithTrain", GromaValues.encode(this.currentLineWithTrain));
        slots.put("frame", frame.exportState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void importSlots(Map<String, Object> slots) {
        this.currentLineWithTrain = (LineWithTrainEntity) GromaValues.decode(slots.get("currentLineWithTrain"), ctx);
        if (slots.get("frame") instanceof Map<?, ?> frameState) frame.importState((java.util.Map<String, Object>) frameState, ctx);
    }
}
