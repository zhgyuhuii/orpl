package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BuiltinTools;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.bo.FormSectionTrainRelationRuntimeBO;
import com.groma.models.emergencydeduction.model.entity.LineWithSectionEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Map;

/**
 * 瞬时过程状态机：形成区间与列车的关联（方圆过程原名：形成区间与列车的关联；宿主对象：线路与列车）
 * <p>
 * 执行周期 P = 1，单例模式 = 0，孪生锁速 = false。
 * 耗时节点与耗时子过程由底座挂入等待表逐拍推进，瞬时节点同步级联；回边循环按底座的零时间循环配额保护。
 * </p>
 */
@GromaMeta(
        id = "Ls7rMbkit4rP36FTZiY",
        zhName = "形成区间与列车的关联",
        kind = "PROCESS_TASK"
)
public final class LineWithTrainFormSectionTrainRelationProcessTask extends GromaProcessTask {
    private static final GromaProcessTask.Timing TIMING = new GromaProcessTask.Timing(false, false, false, 0, "STEP", 1.0D, "s", null, null, "", null, null, null, null, null, null, 1L);

    /**
     * 宿主对象（方圆对象类型原名：线路与列车）
     */
    private final LineWithTrainEntity self;

    /**
     * 输出形参：区间与区间内列车
     */
    private SectionWithTrainEntity sectionWithTrain;

    /**
     * 过程运行帧（局部量 / 节点输出 / 子过程句柄）
     */
    private final FormSectionTrainRelationRuntimeBO frame = new FormSectionTrainRelationRuntimeBO();

    public LineWithTrainFormSectionTrainRelationProcessTask(GromaWorldContext ctx,
            LineWithTrainEntity self, SectionWithTrainEntity sectionWithTrain) {
        super(ctx, self, "dv:designView_z02AJALkr13QaoAQd4l", "形成区间与列车的关联", TIMING);
        java.util.Objects.requireNonNull(self, "宿主对象不能为空: 形成区间与列车的关联");
        this.self = self;
        this.sectionWithTrain = sectionWithTrain;
    }

    private void setSectionWithTrain(SectionWithTrainEntity value) {
        this.sectionWithTrain = value;
    }

    /**
     * 输出形参：区间与区间内列车
     */
    public SectionWithTrainEntity getSectionWithTrain() {
        return this.sectionWithTrain;
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
                fireLineProbe("c4XiGj0Yl9M403d6FoT");
                // 大于等于（compare.ge） a = 实例线路与区间集合.长度; b = 区间标识;
                double v0 = BuiltinTools.length((List<LineWithSectionEntity>) ctx.namedCollection("eZJgQWqYwRKpOTcwSYQ"));
                double v1 = frame.getSectionIdentifier();
                boolean r = v0 >= v1;
                frame.setOut0(r);
            }
            case 1 -> {
                fireLineProbe("vpR37v0m5eQrFel5mdB");
                // 按标识取元素（innerCollectionArray.getByIndex） 标识 = 区间标识; 写出: 元素 → 临时线路与区间;
                long v0 = frame.getSectionIdentifier();
                LineWithSectionEntity r = (LineWithSectionEntity) BuiltinTools.getByIndex((List<LineWithSectionEntity>) ctx.namedCollection("eZJgQWqYwRKpOTcwSYQ"), v0);
                frame.setOut1(r);
            }
            case 2 -> {
                fireLineProbe("wT82mvHH1ICO6nRgK50");
                // 等于（compare.eq） a = 本对象.演示往返; b = 往返;
                Boolean v0 = self.isDemoRoundTrip();
                Boolean v1 = true;
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut2(r);
            }
            case 3 -> {
                fireLineProbe("AYplo2ZTMJDuhu9g4AI");
                // 等于（compare.eq） a = 本对象.沿里程增大方向; b = 沿里程增大方向;
                Boolean v0 = self.isAlongIncreasingMileage();
                Boolean v1 = true;
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut3(r);
            }
            case 4 -> {
                fireLineProbe("AOL7jwNKnKnh8DO6QCs");
                // 等于（compare.eq） a = 本次已折返; b = 本次已折返;
                Boolean v0 = frame.getReturnedThisTime();
                Boolean v1 = false;
                boolean r = BuiltinTools.eq(v0, v1);
                frame.setOut4(r);
            }
            case 5 -> {
                fireLineProbe("VzOjTYcmFvVkfhQ9fMC");
                // 大于等于（compare.ge） a = 本对象.列车当前在线路上的里程; b = 临时线路与区间.区间起点里程;
                double v0 = self.getTrainMileageOnLine();
                double v1 = frame.getTempLineWithSection().getSectionStartMileage();
                boolean r = v0 >= v1;
                frame.setOut5(r);
            }
            case 6 -> {
                fireLineProbe("Lz2yuv6MrwztklOisL0");
                // 大于（compare.gt） a = 本对象.列车当前在线路上的里程; b = 临时线路与区间.区间起点里程;
                double v0 = self.getTrainMileageOnLine();
                double v1 = frame.getTempLineWithSection().getSectionStartMileage();
                boolean r = v0 > v1;
                frame.setOut6(r);
            }
            case 7 -> {
                fireLineProbe("T2DQ1MUgP01IM9pJp5f");
                // 赋值（assignment.assign） a = 往返; 写出: b → 本次已折返;
                boolean v0 = true;
                boolean r = v0;
                frame.setOut7(r);
            }
            case 8 -> {
                fireLineProbe("Kf8iFKeniVB7BoUrPM0");
                // 小于（compare.lt） a = 本对象.列车当前在线路上的里程; b = 临时线路与区间.区间终点里程;
                double v0 = self.getTrainMileageOnLine();
                double v1 = frame.getTempLineWithSection().getSectionEndMileage();
                boolean r = v0 < v1;
                frame.setOut8(r);
            }
            case 9 -> {
                fireLineProbe("GtP4dJ6UOgx0EXqhX5V");
                // 小于等于（compare.le） a = 本对象.列车当前在线路上的里程; b = 临时线路与区间.区间终点里程;
                double v0 = self.getTrainMileageOnLine();
                double v1 = frame.getTempLineWithSection().getSectionEndMileage();
                boolean r = v0 <= v1;
                frame.setOut9(r);
            }
            case 10 -> {
                fireLineProbe("USFir7dvNqAFA5CyEFb");
                // 非（logic.logNot） a = 本对象.沿里程增大方向; 写出: b → 本对象.沿里程增大方向;
                boolean v0 = self.isAlongIncreasingMileage();
                boolean r = !v0;
                frame.setOut10(r);
            }
            case 11 -> {
                fireLineProbe("Ic7A8IDoOh53Xza61zK");
                // 与（logic.logAnd） a = 大于等于.c; b = 小于.c;
                boolean v0 = frame.getOut5();
                boolean v1 = frame.getOut8();
                boolean r = v0 && v1;
                frame.setOut11(r);
            }
            case 12 -> {
                fireLineProbe("i3TGgEIrU7EZWGcC6SW");
                // 与（logic.logAnd） a = 小于等于.c; b = 大于.c;
                boolean v0 = frame.getOut9();
                boolean v1 = frame.getOut6();
                boolean r = v0 && v1;
                frame.setOut12(r);
            }
            case 13 -> {
                fireLineProbe("dC15RdhjNxCjhfKzm70");
                // 重置为1（assignment.resetToOne） a = 区间标识; 写出: a → 区间标识;
                frame.setSectionIdentifier(1L);
            }
            case 14 -> {
                fireLineProbe("DVHePBAv5JnoBjgveQ5");
                // 递增（automatic.inc） a = 区间标识; 写出: a → 区间标识;
                long v0 = frame.getSectionIdentifier();
                frame.setSectionIdentifier(v0 + 1L);
            }
            case 15 -> {
                fireLineProbe("dQc0xtWg6wdwt7PfMUN");
                // 调用瞬时子过程：查找前方车站 当前线路与列车 = 本对象;
                frame.setChild15(new FindAheadStationProcessTask(ctx, self, false, null));
                if (!spawnOrRun(15, frame.getChild15())) return false;
            }
            case 16 -> {
                fireLineProbe("9Ks2jiQjr1xPhQ8o3EX");
                // 调用瞬时子过程：创建关联
                if (!spawnOrRun(16, new LineWithTrainCreateRelationProcessTask(ctx, self))) return false;
            }
            case 17 -> {
                fireLineProbe("ArnoFIBiUpOQRwjkYqz");
                // 指向（assignment.assignRef） a = 线路与车站.车站; 写出: b → 临时关联.目标车站;
                StationEntity v0 = frame.getLineWithStation().getStation();
                StationEntity r = v0;
                frame.setOut17(r);
            }
            case 18 -> {
                fireLineProbe("8gGwDUTH37HMOuYRoLN");
                // 指向（assignment.assignRef） a = 实例0149次列车; 写出: b → 临时关联.列车;
                TrainEntity v0 = (TrainEntity) ctx.namedInstance("oQU5zzFLJwxrRLNNWhA");
                TrainEntity r = v0;
                frame.setOut18(r);
            }
            case 19 -> {
                fireLineProbe("er8Yej0xsuMDHMhvQ0A");
                // 指向（assignment.assignRef） a = 临时线路与区间.地铁区间; 写出: b → 临时关联.区间;
                MetroSectionEntity v0 = frame.getTempLineWithSection().getMetroSection();
                MetroSectionEntity r = v0;
                frame.setOut19(r);
            }
            case 20 -> {
                fireLineProbe("GMxRrqhLYBtjnY2jhuw");
                // 指向（assignment.assignRef） a = 临时关联; 写出: b → 区间与区间内列车;
                SectionWithTrainEntity v0 = frame.getTempRelation();
                SectionWithTrainEntity r = v0;
                frame.setOut20(r);
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
                if (!(frame.getOut0())) {
                    activate(2);
                }
            }
            case 1 -> {
                frame.setTempLineWithSection(frame.getOut1());
                activate(3);
            }
            case 2 -> {
                if (frame.getOut2()) {
                    activate(4);
                }
            }
            case 3 -> {
                if (frame.getOut3()) {
                    activate(5);
                }
                if (!(frame.getOut3())) {
                    activate(6);
                }
            }
            case 4 -> {
                if (frame.getOut4()) {
                    activate(7);
                }
            }
            case 5 -> {
                activate(8);
            }
            case 6 -> {
                activate(9);
            }
            case 7 -> {
                frame.setReturnedThisTime(frame.getOut7());
                activate(10);
            }
            case 8 -> {
                activate(11);
            }
            case 9 -> {
                activate(12);
            }
            case 10 -> {
                self.setAlongIncreasingMileage(frame.getOut10(), ctx);
                activate(13);
            }
            case 11 -> {
                if (!(frame.getOut11())) {
                    activate(14);
                }
                if (frame.getOut11()) {
                    activate(15);
                }
            }
            case 12 -> {
                if (!(frame.getOut12())) {
                    activate(14);
                }
                if (frame.getOut12()) {
                    activate(15);
                }
            }
            case 13 -> {
                activate(0);
            }
            case 14 -> {
                activate(0);
            }
            case 15 -> {
                // 回写：线路与车站 ← 查找前方车站.前方线路与车站
                frame.setLineWithStation(frame.getChild15().getAheadLineWithStation());
                if (!(frame.getChild15().getFound())) {
                    activate(2);
                }
                if (frame.getChild15().getFound()) {
                    activate(16);
                }
            }
            case 16 -> {
                // 生成：临时关联
                frame.setTempRelation(ctx.spawn(new SectionWithTrainEntity(ctx.nextInstanceId("SectionWithTrain"))));
                activate(17);
            }
            case 17 -> {
                frame.getTempRelation().setTargetStation(frame.getOut17(), ctx);
                activate(18);
            }
            case 18 -> {
                frame.getTempRelation().setTrain(frame.getOut18(), ctx);
                activate(19);
            }
            case 19 -> {
                frame.getTempRelation().setSection(frame.getOut19(), ctx);
                activate(20);
            }
            case 20 -> {
                this.sectionWithTrain = frame.getOut20();
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

    @Override
    protected void onChildRestored(int pc, GromaProcessTask child) {
        if (pc == 15) {
            frame.setChild15((FindAheadStationProcessTask) child);
        }
    }
}
