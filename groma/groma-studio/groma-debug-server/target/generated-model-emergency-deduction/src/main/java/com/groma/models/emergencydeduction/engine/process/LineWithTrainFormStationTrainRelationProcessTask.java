package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FormStationTrainRelationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Map;

/**
 * 瞬时过程状态机：形成车站与列车的关联（方圆过程原名：形成车站与列车的关联；宿主对象：线路与列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "eJ4DHDmPNpDiyA8MPzb",
        zhName = "形成车站与列车的关联",
        kind = "PROCESS_TASK"
)
public final class LineWithTrainFormStationTrainRelationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：线路与列车）
     */
    private final LineWithTrainEntity self;

    /**
     * 输出形参：车站与停靠列车
     */
    private StationWithParkedTrainEntity stationWithParkedTrain;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FormStationTrainRelationRuntimeBO frame = new FormStationTrainRelationRuntimeBO();

    public LineWithTrainFormStationTrainRelationProcessTask(GromaWorldContext ctx,
            LineWithTrainEntity self, StationWithParkedTrainEntity stationWithParkedTrain) {
        super(ctx, self, "dv:designView_tOSCSMGoda2sHYuuDqN", "形成车站与列车的关联", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 形成车站与列车的关联");
        this.self = self;
        this.stationWithParkedTrain = stationWithParkedTrain;
    }

    private void setStationWithParkedTrain(StationWithParkedTrainEntity value) {
        this.stationWithParkedTrain = value;
    }

    /**
     * 输出形参：车站与停靠列车
     */
    public StationWithParkedTrainEntity getStationWithParkedTrain() {
        return this.stationWithParkedTrain;
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
                fireLineProbe("XHpMYNRj9mtFepZlDvG");
                // 大于等于（compare.ge） a = 实例线路与车站集合.长度; b = 标识;
                double v0 = BuiltinTools.length((List<LineWithStationEntity>) ctx.namedCollection("ue1TzOuUYomKixhyqSU"));
                double v1 = frame.getIdentifier();
                boolean r = v0 >= v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("Klq4GqX0nqW0pOUBSRZ");
                // 按标识取元素（innerCollectionArray.getByIndex） 标识 = 标识; 写出: 元素 → 线路与车站;
                long v0 = frame.getIdentifier();
                LineWithStationEntity r = (LineWithStationEntity) BuiltinTools.getByIndex((List<LineWithStationEntity>) ctx.namedCollection("ue1TzOuUYomKixhyqSU"), v0);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("JQ7DXr05bnotBD1VAW8");
                // 等于（compare.eq） a = 线路与车站.线路里程; b = 本对象.列车当前在线路上的里程;
                Double v0 = frame.getLineWithStation().getLineMileage();
                Double v1 = self.getTrainMileageOnLine();
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("aYeZ0F93olomhbAxOZe");
                // 递增（automatic.inc） a = 标识; 写出: a → 标识;
                long v0 = frame.getIdentifier();
                frame.setIdentifier(v0 + 1L);
            }
            case 4 -> {
                fireLineProbe("B9ETTj8Q0Xc459Kx4r5");
                // 调用瞬时子过程：生成关联
                if (!spawnOrRun(4, new LineWithTrainGenerateRelationProcessTask(ctx, self))) return false;
            }
            case 5 -> {
                fireLineProbe("mCREyib1J4XyopoD0Ch");
                // 指向（assignment.assignRef） a = 实例0149次列车; 写出: b → 临时关联.列车;
                TrainEntity v0 = (TrainEntity) ctx.namedInstance("oQU5zzFLJwxrRLNNWhA");
                TrainEntity r = v0;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("ikNCcSfaTBovLKfO65O");
                // 指向（assignment.assignRef） a = 线路与车站.车站; 写出: b → 临时关联.车站;
                StationEntity v0 = frame.getLineWithStation().getStation();
                StationEntity r = v0;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("MQlKgy4ApflKT1hIeLR");
                // 指向（assignment.assignRef） a = 临时关联; 写出: b → 车站与停靠列车;
                StationWithParkedTrainEntity v0 = frame.getTempRelation();
                StationWithParkedTrainEntity r = v0;
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
                if (frame.getOut0()) {
                    activate(1);
                }
            }
            case 1 -> {
                frame.setLineWithStation(frame.getOut1());
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
                activate(0);
            }
            case 4 -> {
                // 生成：临时关联
                frame.setTempRelation(ctx.spawn(new StationWithParkedTrainEntity(ctx.nextInstanceId("StationWithParkedTrain"))));
                activate(5);
            }
            case 5 -> {
                frame.getTempRelation().setTrain(frame.getOut5(), ctx);
                activate(6);
            }
            case 6 -> {
                frame.getTempRelation().setStation(frame.getOut6(), ctx);
                activate(7);
            }
            case 7 -> {
                this.stationWithParkedTrain = frame.getOut7();
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
