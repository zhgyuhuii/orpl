package com.groma.models.emergencydeduction844676b1.engine.runtime;

import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaModuleRuntime.ManualStartDescriptor;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaTriggerTable;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.context.Scene1WorldContext;
import com.groma.models.emergencydeduction844676b1.engine.context.Scene2WorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.AreaComputeRainfallCatchmentProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.AreaSurfaceDrainProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.AreaWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceFileExistsProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceFileInitProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceIsFileEmptyProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceReadFileLineByLineProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceReadTextContentProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeFileExistsProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeFileInitProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeIsEmptyFileProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeReadTextContentProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.LineWithTrainFormSectionTrainRelationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.LineWithTrainFormStationTrainRelationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.MetroSectionDrainProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.MetroSectionStationSectionFlowExchangeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.MetroSectionWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupRecordDangerExposureProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupUpdateEvacuationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PassengerGroupUpdateTrappedProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PlanExecutorExecutePlanProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PlanGeneratorComputeDisposalTimeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.PlanGeneratorFormCandidateResponsePlanProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.ProcessCatalog;
import com.groma.models.emergencydeduction844676b1.engine.process.RainfallRainfallProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene11ConvertToStopProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene12TurnNoIntoYesProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene13ConvertToRunProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene14CreateObjectTurnYesIntoNoProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.StationAreaStationFlowExchangeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.StationWaterloggingChangeProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.TrainDepartProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.TrainEnterStationProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.TrainSectionRunProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.TrainStoppedAtStationProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaWithStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileServiceEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileTypeEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FloodProtectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.GeoRegionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.GeoRouteEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.PlanExecutorEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.PlanGeneratorEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallAffectedAreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallCatchmentEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.Scene1Entity;
import com.groma.models.emergencydeduction844676b1.model.entity.Scene2Entity;
import com.groma.models.emergencydeduction844676b1.model.entity.SectionWithEndStationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationWithPassengerGroupEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.SuspensionControlEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.TrainPassingEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.TrainWithPassengerGroupEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.UndergroundInflowEntity;
import com.groma.models.emergencydeduction844676b1.utils.ConditionJudgeUtil;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Map;

/**
 * 模型「应急决策推演-关联副本-1029」运行时入口（模型编码 emergency-deduction-844676b1，版本 647）：事件接线、默认启动、场景沙盒与过程工厂
 */
public final class EmergencyDeductionModuleRuntime implements GromaModuleRuntime {
    public static final String MODEL_CODE = "emergency-deduction-844676b1";

    public static final String MODEL_VERSION = "647";

    public static final String MODEL_LABEL = "应急决策推演-关联副本-1029";

    public static final String MODEL_ID = "3cW2TNyvVxiJxV07ZlR";

    private final GromaTriggerTable triggerTable = new GromaTriggerTable();

    public EmergencyDeductionModuleRuntime() {
        GromaTriggerTable table = this.triggerTable;
        registerWiringShard0(table);
        registerWiringShard1(table);
        registerWiringShard2(table);
    }

    /**
     * 接线登记分片 0（16 条；分片注册防单方法字节码超限）
     */
    private void registerWiringShard0(GromaTriggerTable table) {
        // 默认启动：文件类型 创建后启动「文件初始化」
        table.onDefaultStart("FileType", java.util.List.of(), this::start1, "文件初始化");
        // 默认启动：线路与列车 创建后启动「形成车站与列车的关联」
        table.onDefaultStart("LineWithTrain", java.util.List.of(), this::start2, "形成车站与列车的关联");
        // 默认启动：场景1 创建后启动「1转换为停站」
        table.onDefaultStart("Scene1", java.util.List.of(), this::start3, "1转换为停站");
        // invocation 文件初始化：文件类型.路径 [v] →「文件初始化」
        table.onEvent("FileType", "abc7c313392342d8bb2f1a44ef593248", "v", "", null, java.util.List.of(), java.util.List.of(), this::start4, "文件初始化");
        // condition 区域与站内水流交换：区域.积水状态 [sa] →「区域与站内水流交换」
        table.onEvent("Area", "pdiEfPgAApLsNQ1Cp5j", "sa", "积水", null, java.util.List.of("gEzBWiOUbrpvxwZ8Pv0"), java.util.List.of("f482bs8kk6zJ2XuV6BK"), this::start5, "区域与站内水流交换");
        // invocation 进站：线路与列车.当前停靠关联 [ra] →「进站」
        table.onEvent("LineWithTrain", "0SOv8AQOpa2y8sC7shh", "ra", "", null, java.util.List.of(), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start6, "进站");
        // invocation 进站：线路与列车.当前停靠关联 [rv] →「进站」
        table.onEvent("LineWithTrain", "0SOv8AQOpa2y8sC7shh", "rv", "", null, java.util.List.of(), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start7, "进站");
        // invocation 停站：列车.列车状态 [sa] →「停站」
        table.onEvent("Train", "mQs84jpKcMkngxFWdBT", "sa", "停站", null, java.util.List.of("OxQTIIgXFvoh8WDwjOh"), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start8, "停站");
        // invocation 离站：车站与停靠列车.离站是否允许 [v] →「离站」
        table.onEvent("StationWithParkedTrain", "fOJkK885q6NfNdBgF34", "v", "", ConditionJudgeUtil::judge9, java.util.List.of("0SOv8AQOpa2y8sC7shh"), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start9, "离站");
        // invocation 形成区间与列车的关联：列车.列车状态 [sa] →「形成区间与列车的关联」
        table.onEvent("Train", "mQs84jpKcMkngxFWdBT", "sa", "行驶", null, java.util.List.of("OxQTIIgXFvoh8WDwjOh"), java.util.List.of(), this::start10, "形成区间与列车的关联");
        // invocation 形成车站与列车的关联：区间与区间内列车.是否到达目标车站 [v] →「形成车站与列车的关联」
        table.onEvent("SectionWithTrain", "bWCZ6rcu8LXoqxCT1TG", "v", "", ConditionJudgeUtil::judge11, java.util.List.of("x3P9T6T0WTa3KXjrMbT"), java.util.List.of(), this::start11, "形成车站与列车的关联");
        // fixed 区间行驶：线路与列车.当前行驶关联 [ra] →「区间行驶」
        table.onEvent("LineWithTrain", "x3P9T6T0WTa3KXjrMbT", "ra", "", null, java.util.List.of(), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start12, "区间行驶");
        // fixed 区间行驶：线路与列车.当前行驶关联 [rv] →「区间行驶」
        table.onEvent("LineWithTrain", "x3P9T6T0WTa3KXjrMbT", "rv", "", null, java.util.List.of(), java.util.List.of("OxQTIIgXFvoh8WDwjOh"), this::start13, "区间行驶");
        // condition 地表排水：区域.积水状态 [sa] →「地表排水」
        table.onEvent("Area", "pdiEfPgAApLsNQ1Cp5j", "sa", "积水", null, java.util.List.of(), java.util.List.of(), this::start14, "地表排水");
        // condition 车站与区间水流交换：车站.积水状态 [sa] →「车站与区间水流交换」
        table.onEvent("Station", "ZYx31ZY8GstGNur40z2", "sa", "积水", null, java.util.List.of("cs7DzmGZqyZnfP20Gsy"), java.util.List.of("6NOAfgYaMv32vdr5VlF"), this::start15, "车站与区间水流交换");
        // invocation 1转换为停站：场景1.文本2 [a] →「1转换为停站」
        table.onEvent("Scene1", "Cwaojup8iZOpBuisiCL", "a", "", null, java.util.List.of(), java.util.List.of(), this::start16, "1转换为停站");
    }

    /**
     * 接线登记分片 1（16 条；分片注册防单方法字节码超限）
     */
    private void registerWiringShard1(GromaTriggerTable table) {
        // invocation 2把否变成是：场景1.列车状态1 [sa] →「2把否变成是」
        table.onEvent("Scene1", "xuFANgtC3Dzw6jUE1H4", "sa", "停站", null, java.util.List.of(), java.util.List.of(), this::start17, "2把否变成是");
        // invocation 3转换为行驶：场景1.文本1 [v] →「3转换为行驶」
        table.onEvent("Scene1", "GKzjQYuz4zLjBQGdWxy", "v", "", ConditionJudgeUtil::judge18, java.util.List.of(), java.util.List.of(), this::start18, "3转换为行驶");
        // invocation 4生成对象并把是变成否：场景1.列车状态1 [sa] →「4生成对象并把是变成否」
        table.onEvent("Scene1", "xuFANgtC3Dzw6jUE1H4", "sa", "行驶", null, java.util.List.of(), java.util.List.of(), this::start19, "4生成对象并把是变成否");
        // 手动启动点：「更新受困情况」（类型 乘客群；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「记录危险暴露」（类型 乘客群；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「更新疏散情况」（类型 乘客群；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「判断文件是否存在」（类型 文件类型；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「判断是否为空文件」（类型 文件类型；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「读取文本内容」（类型 文件类型；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「车站与区间水流交换」（类型 地铁区间；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「排水」（类型 地铁区间；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「积水变化」（类型 地铁区间；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「判断文件是否存在」（类型 文件服务；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「逐行读取文件内容」（类型 文件服务；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「判断文件是否为空」（类型 文件服务；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「文件初始化」（类型 文件服务；无事件触发线与默认启动，供 startManually 外部干预）
    }

    /**
     * 接线登记分片 2（15 条；分片注册防单方法字节码超限）
     */
    private void registerWiringShard2(GromaTriggerTable table) {
        // 手动启动点：「读取文本内容」（类型 文件服务；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「降雨」（类型 降雨；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「降雨」（类型 降雨与受影响区域；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「计算降雨汇水」（类型 降雨与受影响区域；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「执行方案」（类型 方案执行器；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「积水变化」（类型 区域；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「计算降雨汇水」（类型 区域；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「计算处置时间」（类型 方案生成器；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「形成备选响应方案」（类型 方案生成器；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「积水变化」（类型 车站；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「区域与站内水流交换」（类型 车站；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「停站」（类型 列车；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「离站」（类型 列车；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「进站」（类型 列车；无事件触发线与默认启动，供 startManually 外部干预）
        // 手动启动点：「区间行驶」（类型 列车；无事件触发线与默认启动，供 startManually 外部干预）
    }

    /**
     * 接线启动器：启动过程「文件初始化」
     */
    private void start1(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        FileTypeEntity contextEntity = (FileTypeEntity) context;
        FileTypeEntity hostEntity = (FileTypeEntity) host;
        c.launch(new FileTypeFileInitProcessTask(c, hostEntity));
    }

    /**
     * 过程「形成车站与列车的关联」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after2(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            LineWithTrainFormStationTrainRelationProcessTask task) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        // 输出影响回写：本对象.当前停靠关联 ← 过程「形成车站与列车的关联」的输出 车站与停靠列车
        contextEntity.setCurrentParkedRelation(task.getStationWithParkedTrain(), c);
    }

    /**
     * 接线启动器：启动过程「形成车站与列车的关联」，成功后执行调用点上的后置动作
     */
    private void start2(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        LineWithTrainEntity hostEntity = (LineWithTrainEntity) host;
        LineWithTrainFormStationTrainRelationProcessTask task = new LineWithTrainFormStationTrainRelationProcessTask(c, hostEntity, null);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after2(c, context, host, task));
        c.launch(task);
    }

    /**
     * 接线启动器：启动过程「1转换为停站」
     */
    private void start3(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        Scene1Entity hostEntity = (Scene1Entity) host;
        c.launch(new Scene11ConvertToStopProcessTask(c, hostEntity));
    }

    /**
     * 接线启动器：启动过程「文件初始化」
     */
    private void start4(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        FileTypeEntity contextEntity = (FileTypeEntity) context;
        FileTypeEntity hostEntity = (FileTypeEntity) host;
        c.launch(new FileTypeFileInitProcessTask(c, hostEntity));
    }

    /**
     * 过程「区域与站内水流交换」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after5(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            StationAreaStationFlowExchangeProcessTask task) {
        AreaWithStationEntity contextEntity = (AreaWithStationEntity) context;
        // 输出影响回写：本对象.本步过水量 ← 过程「区域与站内水流交换」的输出 本步过水量
        contextEntity.setStepFlowVolume(task.getStepFlowVolume(), c);
        // 输出影响回写：本对象.当前过水流量 ← 过程「区域与站内水流交换」的输出 当前过水流量
        contextEntity.setCurrentFlowRate(task.getCurrentFlowRate(), c);
    }

    /**
     * 接线启动器：启动过程「区域与站内水流交换」，成功后执行调用点上的后置动作
     */
    private void start5(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        AreaWithStationEntity contextEntity = (AreaWithStationEntity) context;
        StationEntity hostEntity = (StationEntity) host;
        StationAreaStationFlowExchangeProcessTask task = new StationAreaStationFlowExchangeProcessTask(c, hostEntity, 0.0D, 0.0D, contextEntity.getArea());
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after5(c, context, host, task));
        c.launch(task);
    }

    /**
     * 接线启动器：启动过程「进站」
     */
    private void start6(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        c.launch(new TrainEnterStationProcessTask(c, hostEntity));
    }

    /**
     * 接线启动器：启动过程「进站」
     */
    private void start7(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        c.launch(new TrainEnterStationProcessTask(c, hostEntity));
    }

    /**
     * 接线启动器：启动过程「停站」
     */
    private void start8(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        c.launch(new TrainStoppedAtStationProcessTask(c, hostEntity, contextEntity.getCurrentParkedRelation()));
    }

    /**
     * 接线启动器：启动过程「离站」
     */
    private void start9(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        c.launch(new TrainDepartProcessTask(c, hostEntity, contextEntity.getCurrentParkedRelation()));
    }

    /**
     * 过程「形成区间与列车的关联」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after10(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            LineWithTrainFormSectionTrainRelationProcessTask task) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        // 输出影响回写：本对象.当前行驶关联 ← 过程「形成区间与列车的关联」的输出 区间与区间内列车
        contextEntity.setCurrentRunningRelation(task.getSectionWithTrain(), c);
    }

    /**
     * 接线启动器：启动过程「形成区间与列车的关联」，成功后执行调用点上的后置动作
     */
    private void start10(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        LineWithTrainEntity hostEntity = (LineWithTrainEntity) host;
        LineWithTrainFormSectionTrainRelationProcessTask task = new LineWithTrainFormSectionTrainRelationProcessTask(c, hostEntity, null);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after10(c, context, host, task));
        c.launch(task);
    }

    /**
     * 过程「形成车站与列车的关联」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after11(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            LineWithTrainFormStationTrainRelationProcessTask task) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        // 输出影响回写：本对象.当前停靠关联 ← 过程「形成车站与列车的关联」的输出 车站与停靠列车
        contextEntity.setCurrentParkedRelation(task.getStationWithParkedTrain(), c);
    }

    /**
     * 接线启动器：启动过程「形成车站与列车的关联」，成功后执行调用点上的后置动作
     */
    private void start11(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        LineWithTrainEntity hostEntity = (LineWithTrainEntity) host;
        LineWithTrainFormStationTrainRelationProcessTask task = new LineWithTrainFormStationTrainRelationProcessTask(c, hostEntity, null);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after11(c, context, host, task));
        c.launch(task);
    }

    /**
     * 过程「区间行驶」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after12(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            TrainSectionRunProcessTask task) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        // 输出影响回写：本对象.列车当前在线路上的里程 ← 过程「区间行驶」的输出 当前线路里程
        contextEntity.setTrainMileageOnLine(task.getCurrentLineMileage(), c);
    }

    /**
     * 接线启动器：启动过程「区间行驶」，成功后执行调用点上的后置动作
     */
    private void start12(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        TrainSectionRunProcessTask task = new TrainSectionRunProcessTask(c, hostEntity, contextEntity.getCurrentRunningRelation(), contextEntity.getTrainMileageOnLine(), contextEntity.isAlongIncreasingMileage());
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after12(c, context, host, task));
        c.launch(task);
    }

    /**
     * 过程「区间行驶」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after13(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            TrainSectionRunProcessTask task) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        // 输出影响回写：本对象.列车当前在线路上的里程 ← 过程「区间行驶」的输出 当前线路里程
        contextEntity.setTrainMileageOnLine(task.getCurrentLineMileage(), c);
    }

    /**
     * 接线启动器：启动过程「区间行驶」，成功后执行调用点上的后置动作
     */
    private void start13(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        LineWithTrainEntity contextEntity = (LineWithTrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        TrainSectionRunProcessTask task = new TrainSectionRunProcessTask(c, hostEntity, contextEntity.getCurrentRunningRelation(), contextEntity.getTrainMileageOnLine(), contextEntity.isAlongIncreasingMileage());
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after13(c, context, host, task));
        c.launch(task);
    }

    /**
     * 接线启动器：启动过程「地表排水」
     */
    private void start14(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        AreaEntity contextEntity = (AreaEntity) context;
        AreaEntity hostEntity = (AreaEntity) host;
        c.launch(new AreaSurfaceDrainProcessTask(c, hostEntity));
    }

    /**
     * 过程「车站与区间水流交换」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after15(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            MetroSectionStationSectionFlowExchangeProcessTask task) {
        SectionWithEndStationEntity contextEntity = (SectionWithEndStationEntity) context;
        // 输出影响回写：本对象.本步过水量 ← 过程「车站与区间水流交换」的输出 本步过水量
        contextEntity.setStepFlowVolume(task.getStepFlowVolume(), c);
        // 输出影响回写：本对象.当前过水流量 ← 过程「车站与区间水流交换」的输出 当前过水流量
        contextEntity.setCurrentFlowRate(task.getCurrentFlowRate(), c);
    }

    /**
     * 接线启动器：启动过程「车站与区间水流交换」，成功后执行调用点上的后置动作
     */
    private void start15(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        SectionWithEndStationEntity contextEntity = (SectionWithEndStationEntity) context;
        MetroSectionEntity hostEntity = (MetroSectionEntity) host;
        MetroSectionStationSectionFlowExchangeProcessTask task = new MetroSectionStationSectionFlowExchangeProcessTask(c, hostEntity, contextEntity.getFlowCoeff(), contextEntity.getJunctionFloorElevation(), 0.0D, contextEntity.getStation(), 0.0D, contextEntity.getEffectiveFlowWidth());
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after15(c, context, host, task));
        c.launch(task);
    }

    /**
     * 接线启动器：启动过程「1转换为停站」
     */
    private void start16(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        Scene1Entity hostEntity = (Scene1Entity) host;
        c.launch(new Scene11ConvertToStopProcessTask(c, hostEntity));
    }

    /**
     * 过程「2把否变成是」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after17(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            Scene12TurnNoIntoYesProcessTask task) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        // 输出影响回写：本对象.文本1 ← 过程「2把否变成是」的输出 文本1
        contextEntity.setText1(task.getText1(), c);
    }

    /**
     * 接线启动器：启动过程「2把否变成是」，成功后执行调用点上的后置动作
     */
    private void start17(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        Scene1Entity hostEntity = (Scene1Entity) host;
        Scene12TurnNoIntoYesProcessTask task = new Scene12TurnNoIntoYesProcessTask(c, hostEntity, false);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after17(c, context, host, task));
        c.launch(task);
    }

    /**
     * 接线启动器：启动过程「3转换为行驶」
     */
    private void start18(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        Scene1Entity hostEntity = (Scene1Entity) host;
        c.launch(new Scene13ConvertToRunProcessTask(c, hostEntity));
    }

    /**
     * 过程「4生成对象并把是变成否」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after19(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            Scene14CreateObjectTurnYesIntoNoProcessTask task) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        // 生成：本对象.文本2
        contextEntity.setText2("", c);
        c.onMemberGenerated(contextEntity, "Cwaojup8iZOpBuisiCL");
        // 输出影响回写：本对象.文本1 ← 过程「4生成对象并把是变成否」的输出 文本1
        contextEntity.setText1(task.getText1(), c);
    }

    /**
     * 接线启动器：启动过程「4生成对象并把是变成否」，成功后执行调用点上的后置动作
     */
    private void start19(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host) {
        Scene1Entity contextEntity = (Scene1Entity) context;
        Scene1Entity hostEntity = (Scene1Entity) host;
        Scene14CreateObjectTurnYesIntoNoProcessTask task = new Scene14CreateObjectTurnYesIntoNoProcessTask(c, hostEntity, false);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after19(c, context, host, task));
        c.launch(task);
    }

    /**
     * 手动启动过程「更新受困情况」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual20(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PassengerGroupEntity contextEntity = (PassengerGroupEntity) context;
        PassengerGroupEntity hostEntity = (PassengerGroupEntity) host;
        return new PassengerGroupUpdateTrappedProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「记录危险暴露」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual21(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PassengerGroupEntity contextEntity = (PassengerGroupEntity) context;
        PassengerGroupEntity hostEntity = (PassengerGroupEntity) host;
        return new PassengerGroupRecordDangerExposureProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「更新疏散情况」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual22(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PassengerGroupEntity contextEntity = (PassengerGroupEntity) context;
        PassengerGroupEntity hostEntity = (PassengerGroupEntity) host;
        return new PassengerGroupUpdateEvacuationProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「判断文件是否存在」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual23(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileTypeEntity contextEntity = (FileTypeEntity) context;
        FileTypeEntity hostEntity = (FileTypeEntity) host;
        return new FileTypeFileExistsProcessTask(c, hostEntity, false);
    }

    /**
     * 手动启动过程「判断是否为空文件」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual24(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileTypeEntity contextEntity = (FileTypeEntity) context;
        FileTypeEntity hostEntity = (FileTypeEntity) host;
        return new FileTypeIsEmptyFileProcessTask(c, hostEntity, false);
    }

    /**
     * 手动启动过程「读取文本内容」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual25(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileTypeEntity contextEntity = (FileTypeEntity) context;
        FileTypeEntity hostEntity = (FileTypeEntity) host;
        return new FileTypeReadTextContentProcessTask(c, hostEntity, "");
    }

    /**
     * 手动启动过程「车站与区间水流交换」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual26(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        MetroSectionEntity contextEntity = (MetroSectionEntity) context;
        MetroSectionEntity hostEntity = (MetroSectionEntity) host;
        return new MetroSectionStationSectionFlowExchangeProcessTask(c, hostEntity, overrides != null && overrides.get("水流系数") != null ? GromaValues.toDouble(overrides.get("水流系数")) : 0.0D, overrides != null && overrides.get("连接处底高程") != null ? GromaValues.toDouble(overrides.get("连接处底高程")) : 0.0D, 0.0D, overrides != null && overrides.get("端点车站") != null ? (StationEntity) overrides.get("端点车站") : null, 0.0D, overrides != null && overrides.get("有效过水宽度") != null ? GromaValues.toDouble(overrides.get("有效过水宽度")) : 0.0D);
    }

    /**
     * 手动启动过程「排水」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual27(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        MetroSectionEntity contextEntity = (MetroSectionEntity) context;
        MetroSectionEntity hostEntity = (MetroSectionEntity) host;
        return new MetroSectionDrainProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「积水变化」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual28(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        MetroSectionEntity contextEntity = (MetroSectionEntity) context;
        MetroSectionEntity hostEntity = (MetroSectionEntity) host;
        return new MetroSectionWaterloggingChangeProcessTask(c, hostEntity, overrides != null && overrides.get("水量变化") != null ? GromaValues.toDouble(overrides.get("水量变化")) : 0.0D);
    }

    /**
     * 手动启动过程「判断文件是否存在」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual29(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileServiceEntity contextEntity = (FileServiceEntity) context;
        FileServiceEntity hostEntity = (FileServiceEntity) host;
        return new FileServiceFileExistsProcessTask(c, hostEntity, overrides != null && overrides.get("路径/标识") != null ? GromaValues.toText(overrides.get("路径/标识")) : "", false, false);
    }

    /**
     * 手动启动过程「逐行读取文件内容」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual30(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileServiceEntity contextEntity = (FileServiceEntity) context;
        FileServiceEntity hostEntity = (FileServiceEntity) host;
        return new FileServiceReadFileLineByLineProcessTask(c, hostEntity, overrides != null && overrides.get("路径/标识") != null ? GromaValues.toText(overrides.get("路径/标识")) : "", 0L, "");
    }

    /**
     * 手动启动过程「判断文件是否为空」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual31(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileServiceEntity contextEntity = (FileServiceEntity) context;
        FileServiceEntity hostEntity = (FileServiceEntity) host;
        return new FileServiceIsFileEmptyProcessTask(c, hostEntity, overrides != null && overrides.get("路径/标识") != null ? GromaValues.toText(overrides.get("路径/标识")) : "", false, false);
    }

    /**
     * 手动启动过程「文件初始化」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual32(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileServiceEntity contextEntity = (FileServiceEntity) context;
        FileServiceEntity hostEntity = (FileServiceEntity) host;
        return new FileServiceFileInitProcessTask(c, hostEntity, overrides != null && overrides.get("路径/标识") != null ? GromaValues.toText(overrides.get("路径/标识")) : "", false, null, "");
    }

    /**
     * 手动启动过程「读取文本内容」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual33(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        FileServiceEntity contextEntity = (FileServiceEntity) context;
        FileServiceEntity hostEntity = (FileServiceEntity) host;
        return new FileServiceReadTextContentProcessTask(c, hostEntity, overrides != null && overrides.get("路径/标识") != null ? GromaValues.toText(overrides.get("路径/标识")) : "", false, "");
    }

    /**
     * 手动启动过程「降雨」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual34(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        RainfallEntity contextEntity = (RainfallEntity) context;
        RainfallEntity hostEntity = (RainfallEntity) host;
        return new RainfallRainfallProcessTask(c, hostEntity, overrides != null && overrides.get("雨量序列") != null ? (Map<Long, Double>) overrides.get("雨量序列") : new java.util.LinkedHashMap<>(16), 0.0D, 0.0D);
    }

    /**
     * 手动启动「降雨」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after35(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            RainfallRainfallProcessTask task) {
        RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) context;
        // 输出影响回写：本对象.当前雨强 ← 过程「降雨」的输出 当前雨强
        contextEntity.setCurrentRainIntensity(task.getCurrentRainIntensity(), c);
        // 输出影响回写：本对象.累计雨量 ← 过程「降雨」的输出 累计雨量
        contextEntity.setCumulativeRainfall(task.getCumulativeRainfall(), c);
    }

    /**
     * 手动启动过程「降雨」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual35(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) context;
        RainfallEntity hostEntity = (RainfallEntity) host;
        RainfallRainfallProcessTask task = new RainfallRainfallProcessTask(c, hostEntity, overrides != null && overrides.get("雨量序列") != null ? (Map<Long, Double>) overrides.get("雨量序列") : contextEntity.getRainfallSeries(), 0.0D, 0.0D);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after35(c, context, host, task));
        return task;
    }

    /**
     * 手动启动「计算降雨汇水」成功后的后置动作（输出影响回写读取 task 的形参终值）
     */
    private void after36(GromaWorldContext c, BaseSimEntity context, BaseSimEntity host,
            AreaComputeRainfallCatchmentProcessTask task) {
        RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) context;
        // 输出影响回写：本对象.当前汇水流量 ← 过程「计算降雨汇水」的输出 当前汇水流量
        contextEntity.setCurrentCatchmentFlow(task.getCurrentCatchmentFlow(), c);
        // 输出影响回写：本对象.本步进汇水量 ← 过程「计算降雨汇水」的输出 本步汇水量
        contextEntity.setStepCatchmentVolume(task.getStepCatchmentVolume(), c);
    }

    /**
     * 手动启动过程「计算降雨汇水」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual36(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) context;
        AreaEntity hostEntity = (AreaEntity) host;
        AreaComputeRainfallCatchmentProcessTask task = new AreaComputeRainfallCatchmentProcessTask(c, hostEntity, overrides != null && overrides.get("当前雨强") != null ? GromaValues.toDouble(overrides.get("当前雨强")) : contextEntity.getCurrentRainIntensity(), 0.0D, 0.0D);
        task.setWiringContext(contextEntity);
        task.setCompletionHook(() -> after36(c, context, host, task));
        return task;
    }

    /**
     * 手动启动过程「执行方案」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual37(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PlanExecutorEntity contextEntity = (PlanExecutorEntity) context;
        PlanExecutorEntity hostEntity = (PlanExecutorEntity) host;
        return new PlanExecutorExecutePlanProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「积水变化」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual38(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        AreaEntity contextEntity = (AreaEntity) context;
        AreaEntity hostEntity = (AreaEntity) host;
        return new AreaWaterloggingChangeProcessTask(c, hostEntity, overrides != null && overrides.get("水量变化值") != null ? GromaValues.toDouble(overrides.get("水量变化值")) : 0.0D);
    }

    /**
     * 手动启动过程「计算降雨汇水」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual39(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        AreaEntity contextEntity = (AreaEntity) context;
        AreaEntity hostEntity = (AreaEntity) host;
        return new AreaComputeRainfallCatchmentProcessTask(c, hostEntity, overrides != null && overrides.get("当前雨强") != null ? GromaValues.toDouble(overrides.get("当前雨强")) : 0.0D, 0.0D, 0.0D);
    }

    /**
     * 手动启动过程「计算处置时间」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual40(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PlanGeneratorEntity contextEntity = (PlanGeneratorEntity) context;
        PlanGeneratorEntity hostEntity = (PlanGeneratorEntity) host;
        return new PlanGeneratorComputeDisposalTimeProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「形成备选响应方案」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual41(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        PlanGeneratorEntity contextEntity = (PlanGeneratorEntity) context;
        PlanGeneratorEntity hostEntity = (PlanGeneratorEntity) host;
        return new PlanGeneratorFormCandidateResponsePlanProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「积水变化」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual42(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        StationEntity contextEntity = (StationEntity) context;
        StationEntity hostEntity = (StationEntity) host;
        return new StationWaterloggingChangeProcessTask(c, hostEntity, overrides != null && overrides.get("水量变化值") != null ? GromaValues.toDouble(overrides.get("水量变化值")) : 0.0D);
    }

    /**
     * 手动启动过程「区域与站内水流交换」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual43(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        StationEntity contextEntity = (StationEntity) context;
        StationEntity hostEntity = (StationEntity) host;
        return new StationAreaStationFlowExchangeProcessTask(c, hostEntity, 0.0D, 0.0D, overrides != null && overrides.get("区域") != null ? (AreaEntity) overrides.get("区域") : null);
    }

    /**
     * 手动启动过程「停站」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual44(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        TrainEntity contextEntity = (TrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        return new TrainStoppedAtStationProcessTask(c, hostEntity, overrides != null && overrides.get("车站与停靠列车") != null ? (StationWithParkedTrainEntity) overrides.get("车站与停靠列车") : null);
    }

    /**
     * 手动启动过程「离站」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual45(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        TrainEntity contextEntity = (TrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        return new TrainDepartProcessTask(c, hostEntity, overrides != null && overrides.get("车站与停靠列车") != null ? (StationWithParkedTrainEntity) overrides.get("车站与停靠列车") : null);
    }

    /**
     * 手动启动过程「进站」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual46(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        TrainEntity contextEntity = (TrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        return new TrainEnterStationProcessTask(c, hostEntity);
    }

    /**
     * 手动启动过程「区间行驶」：overrides 按中文形参名覆盖画布默认实参；调用方负责 c.launch
     */
    private GromaProcessTask startManual47(GromaWorldContext c, BaseSimEntity context,
            BaseSimEntity host, Map<String, Object> overrides) {
        TrainEntity contextEntity = (TrainEntity) context;
        TrainEntity hostEntity = (TrainEntity) host;
        return new TrainSectionRunProcessTask(c, hostEntity, overrides != null && overrides.get("区间与区间内列车") != null ? (SectionWithTrainEntity) overrides.get("区间与区间内列车") : null, overrides != null && overrides.get("当前线路里程") != null ? GromaValues.toDouble(overrides.get("当前线路里程")) : 0.0D, overrides != null && overrides.get("是否沿里程增大方向") != null ? GromaValues.toBoolean(overrides.get("是否沿里程增大方向")) : false);
    }

    @Override
    public String modelCode() {
        return MODEL_CODE;
    }

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public String modelLabel() {
        return MODEL_LABEL;
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public long defaultStepMs() {
        return 1000L;
    }

    @Override
    public GromaTriggerTable triggerTable() {
        return this.triggerTable;
    }

    @Override
    public List<GromaModuleRuntime.SceneDescriptor> scenes() {
        return List.of(new GromaModuleRuntime.SceneDescriptor("scene2", "场景2", "方圆运行场景：场景2", "fFnAAr3WKsN0Oaghvzj"), new GromaModuleRuntime.SceneDescriptor("scene1", "场景1", "方圆运行场景：场景1", "bX4vMzSlVMdk6QjF7CD"));
    }

    @Override
    public GromaWorldContext createSession(String sceneCode, String sessionId, long startEpochMs,
            long finalStepMs, long seed, Map<String, Object> initialOverrides) {
        switch (sceneCode) {
            case "scene2" -> {
                return new Scene2WorldContext(this, sessionId, startEpochMs, finalStepMs, seed, initialOverrides);
            }
            case "scene1" -> {
                return new Scene1WorldContext(this, sessionId, startEpochMs, finalStepMs, seed, initialOverrides);
            }
            default -> throw new IllegalArgumentException("SCENE_NOT_FOUND: 场景不存在 " + sceneCode);
        }
    }

    @Override
    public BaseSimEntity newEntity(String typeCode, String instanceId) {
        switch (typeCode) {
            case "PassengerGroup" -> {
                return new PassengerGroupEntity(instanceId);
            }
            case "GeoRoute" -> {
                return new GeoRouteEntity(instanceId);
            }
            case "FileType" -> {
                return new FileTypeEntity(instanceId);
            }
            case "GeoRegion" -> {
                return new GeoRegionEntity(instanceId);
            }
            case "MetroSection" -> {
                return new MetroSectionEntity(instanceId);
            }
            case "SectionWithTrain" -> {
                return new SectionWithTrainEntity(instanceId);
            }
            case "DisposalPlan" -> {
                return new DisposalPlanEntity(instanceId);
            }
            case "AreaWithStation" -> {
                return new AreaWithStationEntity(instanceId);
            }
            case "FileService" -> {
                return new FileServiceEntity(instanceId);
            }
            case "Rainfall" -> {
                return new RainfallEntity(instanceId);
            }
            case "LineWithSection" -> {
                return new LineWithSectionEntity(instanceId);
            }
            case "LineWithTrain" -> {
                return new LineWithTrainEntity(instanceId);
            }
            case "StationWithPassengerGroup" -> {
                return new StationWithPassengerGroupEntity(instanceId);
            }
            case "StationWithParkedTrain" -> {
                return new StationWithParkedTrainEntity(instanceId);
            }
            case "MetroLine" -> {
                return new MetroLineEntity(instanceId);
            }
            case "RainfallAffectedArea" -> {
                return new RainfallAffectedAreaEntity(instanceId);
            }
            case "PlanExecutor" -> {
                return new PlanExecutorEntity(instanceId);
            }
            case "LineWithStation" -> {
                return new LineWithStationEntity(instanceId);
            }
            case "GeoPoint" -> {
                return new GeoPointEntity(instanceId);
            }
            case "Area" -> {
                return new AreaEntity(instanceId);
            }
            case "FileInfo" -> {
                return new FileInfoEntity(instanceId);
            }
            case "TrainWithPassengerGroup" -> {
                return new TrainWithPassengerGroupEntity(instanceId);
            }
            case "PlanGenerator" -> {
                return new PlanGeneratorEntity(instanceId);
            }
            case "Station" -> {
                return new StationEntity(instanceId);
            }
            case "Train" -> {
                return new TrainEntity(instanceId);
            }
            case "SectionWithEndStation" -> {
                return new SectionWithEndStationEntity(instanceId);
            }
            case "Scene2" -> {
                return new Scene2Entity(instanceId);
            }
            case "Scene1" -> {
                return new Scene1Entity(instanceId);
            }
            case "TrainPassing" -> {
                return new TrainPassingEntity(instanceId);
            }
            case "UndergroundInflow" -> {
                return new UndergroundInflowEntity(instanceId);
            }
            case "RainfallCatchment" -> {
                return new RainfallCatchmentEntity(instanceId);
            }
            case "FloodProtection" -> {
                return new FloodProtectionEntity(instanceId);
            }
            case "SuspensionControl" -> {
                return new SuspensionControlEntity(instanceId);
            }
            default -> { return null; }
        }
    }

    @Override
    public GromaProcessTask newProcess(String defKey, GromaWorldContext ctx, BaseSimEntity host,
            Map<String, Object> args) {
        return ProcessCatalog.newTask(defKey, ctx, host, args);
    }

    /**
     * 按定义键与接线上下文类型重挂输出影响回写钩子（存档续跑专用）
     */
    @Override
    public void reattachWiringHook(String defKey, GromaWorldContext ctx, BaseSimEntity context,
            BaseSimEntity host, GromaProcessTask task) {
        if ("dv:designView_tOSCSMGoda2sHYuuDqN".equals(defKey) && context instanceof LineWithTrainEntity) {
            task.setCompletionHook(() -> after2(ctx, context, host, (LineWithTrainFormStationTrainRelationProcessTask) task));
            return;
        }
        if ("dv:designView_xmjSEU4SGv2otlq6mLP".equals(defKey) && context instanceof AreaWithStationEntity) {
            task.setCompletionHook(() -> after5(ctx, context, host, (StationAreaStationFlowExchangeProcessTask) task));
            return;
        }
        if ("dv:designView_z02AJALkr13QaoAQd4l".equals(defKey) && context instanceof LineWithTrainEntity) {
            task.setCompletionHook(() -> after10(ctx, context, host, (LineWithTrainFormSectionTrainRelationProcessTask) task));
            return;
        }
        if ("dv:designView_XLrKPtiJlmUbySIChuF".equals(defKey) && context instanceof LineWithTrainEntity) {
            task.setCompletionHook(() -> after12(ctx, context, host, (TrainSectionRunProcessTask) task));
            return;
        }
        if ("dv:designView_5mX6EkRtocaf9rXbKu1".equals(defKey) && context instanceof SectionWithEndStationEntity) {
            task.setCompletionHook(() -> after15(ctx, context, host, (MetroSectionStationSectionFlowExchangeProcessTask) task));
            return;
        }
        if ("dv:designView_2WLvrJyBv6htmVscnRk".equals(defKey) && context instanceof Scene1Entity) {
            task.setCompletionHook(() -> after17(ctx, context, host, (Scene12TurnNoIntoYesProcessTask) task));
            return;
        }
        if ("dv:designView_miDwXHXHR407mRDMPzO".equals(defKey) && context instanceof Scene1Entity) {
            task.setCompletionHook(() -> after19(ctx, context, host, (Scene14CreateObjectTurnYesIntoNoProcessTask) task));
            return;
        }
        if ("dv:designView_fjlFgY29HmzqjbmHxYC".equals(defKey) && context instanceof RainfallAffectedAreaEntity) {
            task.setCompletionHook(() -> after35(ctx, context, host, (RainfallRainfallProcessTask) task));
            return;
        }
        if ("dv:designView_aroqnVQzhKVnM1h7wPt".equals(defKey) && context instanceof RainfallAffectedAreaEntity) {
            task.setCompletionHook(() -> after36(ctx, context, host, (AreaComputeRainfallCatchmentProcessTask) task));
            return;
        }
        // 无输出影响接线或非接线启动（手动/子过程）的过程不需要回写钩子

    }

    @Override
    public List<GromaModuleRuntime.TypeDescriptor> types() {
        return ProcessCatalog.TYPES;
    }

    @Override
    public List<GromaModuleRuntime.ProcessDescriptor> processes() {
        return ProcessCatalog.PROCESSES;
    }

    @Override
    public String indexMarkdown() {
        try (java.io.InputStream in = com.groma.base.engine.GromaModuleRuntime.class
                .getResourceAsStream("/META-INF/groma/GROMA_MODEL_INDEX.md")) {
            return in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            return "";
        }
    }

    /**
     * 全部手动启动点（供宿主 / MCP 发现与校验）
     */
    @Override
    public List<GromaModuleRuntime.ManualStartDescriptor> manualStarts() {
        return List.of(new GromaModuleRuntime.ManualStartDescriptor("ent:nVjHf2KrmdIijCrca8a", "更新受困情况", "PassengerGroup", List.of()), new GromaModuleRuntime.ManualStartDescriptor("ent:XUr2ph0kb7en26PZ8gp", "记录危险暴露", "PassengerGroup", List.of()), new GromaModuleRuntime.ManualStartDescriptor("ent:azSevtqA1GFz9T7YjDq", "更新疏散情况", "PassengerGroup", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_04fb48efd78444e3bfc948e0fa91c1f3", "判断文件是否存在", "FileType", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_218a29d6cd004955b234ad6b1bafc7b6", "判断是否为空文件", "FileType", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_80233ba35326427aa7886a6ec6051604", "读取文本内容", "FileType", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_5mX6EkRtocaf9rXbKu1", "车站与区间水流交换", "MetroSection", List.of("水流系数", "连接处底高程", "端点车站", "有效过水宽度")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_kclxVkgMIrkBe36K656", "排水", "MetroSection", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_kVXT3R1XGHTwkie2rur", "积水变化", "MetroSection", List.of("水量变化")), new GromaModuleRuntime.ManualStartDescriptor("ent:rxdnGDCWlbpOGBwd3RG", "判断文件是否存在", "FileService", List.of("路径/标识")), new GromaModuleRuntime.ManualStartDescriptor("ent:R4yssk5cauVoZXGtqkM", "逐行读取文件内容", "FileService", List.of("路径/标识")), new GromaModuleRuntime.ManualStartDescriptor("ent:IeK32FL7HMYxvSd4PnP", "判断文件是否为空", "FileService", List.of("路径/标识")), new GromaModuleRuntime.ManualStartDescriptor("ent:Ne04mUfbnKpBEjTlARK", "文件初始化", "FileService", List.of("路径/标识")), new GromaModuleRuntime.ManualStartDescriptor("ent:3UULlXsUgqgKXOiinlG", "读取文本内容", "FileService", List.of("路径/标识")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_fjlFgY29HmzqjbmHxYC", "降雨", "Rainfall", List.of("雨量序列")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_fjlFgY29HmzqjbmHxYC", "降雨", "RainfallAffectedArea", List.of("雨量序列")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_aroqnVQzhKVnM1h7wPt", "计算降雨汇水", "RainfallAffectedArea", List.of("当前雨强")), new GromaModuleRuntime.ManualStartDescriptor("ent:jtIxDyIRLVxMZw7Tq35", "执行方案", "PlanExecutor", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_175Y0QSMw786DGgXBXZ", "积水变化", "Area", List.of("水量变化值")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_aroqnVQzhKVnM1h7wPt", "计算降雨汇水", "Area", List.of("当前雨强")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_P80bc3nl6S9bWISn6bg", "计算处置时间", "PlanGenerator", List.of()), new GromaModuleRuntime.ManualStartDescriptor("ent:UkKv7XkK4ASpaf1Bnit", "形成备选响应方案", "PlanGenerator", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_i8XnCQtmJLFiB3KzdpL", "积水变化", "Station", List.of("水量变化值")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_xmjSEU4SGv2otlq6mLP", "区域与站内水流交换", "Station", List.of("区域")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_eHq1YpIkoLxwHKywWis", "停站", "Train", List.of("车站与停靠列车")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_767z0mfmOaNst2dPwlG", "离站", "Train", List.of("车站与停靠列车")), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_joMlLooi2InkPOjk59H", "进站", "Train", List.of()), new GromaModuleRuntime.ManualStartDescriptor("dv:designView_XLrKPtiJlmUbySIChuF", "区间行驶", "Train", List.of("区间与区间内列车", "当前线路里程", "是否沿里程增大方向")));
    }

    /**
     * 手动启动过程：宿主按类型校验，成员路径经 resolveHost 解析；未注册的键返回 null
     */
    @Override
    public GromaProcessTask startManually(String defKey, GromaWorldContext ctx, BaseSimEntity host,
            Map<String, Object> argOverrides) {
        if ("ent:nVjHf2KrmdIijCrca8a".equals(defKey) && host instanceof PassengerGroupEntity) {
            PassengerGroupEntity contextEntity = (PassengerGroupEntity) host;
            return startManual20(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:XUr2ph0kb7en26PZ8gp".equals(defKey) && host instanceof PassengerGroupEntity) {
            PassengerGroupEntity contextEntity = (PassengerGroupEntity) host;
            return startManual21(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:azSevtqA1GFz9T7YjDq".equals(defKey) && host instanceof PassengerGroupEntity) {
            PassengerGroupEntity contextEntity = (PassengerGroupEntity) host;
            return startManual22(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_04fb48efd78444e3bfc948e0fa91c1f3".equals(defKey) && host instanceof FileTypeEntity) {
            FileTypeEntity contextEntity = (FileTypeEntity) host;
            return startManual23(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_218a29d6cd004955b234ad6b1bafc7b6".equals(defKey) && host instanceof FileTypeEntity) {
            FileTypeEntity contextEntity = (FileTypeEntity) host;
            return startManual24(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_80233ba35326427aa7886a6ec6051604".equals(defKey) && host instanceof FileTypeEntity) {
            FileTypeEntity contextEntity = (FileTypeEntity) host;
            return startManual25(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_5mX6EkRtocaf9rXbKu1".equals(defKey) && host instanceof MetroSectionEntity) {
            MetroSectionEntity contextEntity = (MetroSectionEntity) host;
            return startManual26(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_kclxVkgMIrkBe36K656".equals(defKey) && host instanceof MetroSectionEntity) {
            MetroSectionEntity contextEntity = (MetroSectionEntity) host;
            return startManual27(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_kVXT3R1XGHTwkie2rur".equals(defKey) && host instanceof MetroSectionEntity) {
            MetroSectionEntity contextEntity = (MetroSectionEntity) host;
            return startManual28(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:rxdnGDCWlbpOGBwd3RG".equals(defKey) && host instanceof FileServiceEntity) {
            FileServiceEntity contextEntity = (FileServiceEntity) host;
            return startManual29(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:R4yssk5cauVoZXGtqkM".equals(defKey) && host instanceof FileServiceEntity) {
            FileServiceEntity contextEntity = (FileServiceEntity) host;
            return startManual30(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:IeK32FL7HMYxvSd4PnP".equals(defKey) && host instanceof FileServiceEntity) {
            FileServiceEntity contextEntity = (FileServiceEntity) host;
            return startManual31(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:Ne04mUfbnKpBEjTlARK".equals(defKey) && host instanceof FileServiceEntity) {
            FileServiceEntity contextEntity = (FileServiceEntity) host;
            return startManual32(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:3UULlXsUgqgKXOiinlG".equals(defKey) && host instanceof FileServiceEntity) {
            FileServiceEntity contextEntity = (FileServiceEntity) host;
            return startManual33(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_fjlFgY29HmzqjbmHxYC".equals(defKey) && host instanceof RainfallEntity) {
            RainfallEntity contextEntity = (RainfallEntity) host;
            return startManual34(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_fjlFgY29HmzqjbmHxYC".equals(defKey) && host instanceof RainfallAffectedAreaEntity) {
            RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) host;
            BaseSimEntity hostEntity = GromaTriggerTable.resolveHost(ctx, contextEntity, List.of("GLWeuTcB89MzugElwt0"));
            return hostEntity == null ? null : startManual35(ctx, contextEntity, hostEntity, argOverrides);
        }
        if ("dv:designView_aroqnVQzhKVnM1h7wPt".equals(defKey) && host instanceof RainfallAffectedAreaEntity) {
            RainfallAffectedAreaEntity contextEntity = (RainfallAffectedAreaEntity) host;
            BaseSimEntity hostEntity = GromaTriggerTable.resolveHost(ctx, contextEntity, List.of("qtEOSTOdGnKsYsTZdtB"));
            return hostEntity == null ? null : startManual36(ctx, contextEntity, hostEntity, argOverrides);
        }
        if ("ent:jtIxDyIRLVxMZw7Tq35".equals(defKey) && host instanceof PlanExecutorEntity) {
            PlanExecutorEntity contextEntity = (PlanExecutorEntity) host;
            return startManual37(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_175Y0QSMw786DGgXBXZ".equals(defKey) && host instanceof AreaEntity) {
            AreaEntity contextEntity = (AreaEntity) host;
            return startManual38(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_aroqnVQzhKVnM1h7wPt".equals(defKey) && host instanceof AreaEntity) {
            AreaEntity contextEntity = (AreaEntity) host;
            return startManual39(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_P80bc3nl6S9bWISn6bg".equals(defKey) && host instanceof PlanGeneratorEntity) {
            PlanGeneratorEntity contextEntity = (PlanGeneratorEntity) host;
            return startManual40(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("ent:UkKv7XkK4ASpaf1Bnit".equals(defKey) && host instanceof PlanGeneratorEntity) {
            PlanGeneratorEntity contextEntity = (PlanGeneratorEntity) host;
            return startManual41(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_i8XnCQtmJLFiB3KzdpL".equals(defKey) && host instanceof StationEntity) {
            StationEntity contextEntity = (StationEntity) host;
            return startManual42(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_xmjSEU4SGv2otlq6mLP".equals(defKey) && host instanceof StationEntity) {
            StationEntity contextEntity = (StationEntity) host;
            return startManual43(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_eHq1YpIkoLxwHKywWis".equals(defKey) && host instanceof TrainEntity) {
            TrainEntity contextEntity = (TrainEntity) host;
            return startManual44(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_767z0mfmOaNst2dPwlG".equals(defKey) && host instanceof TrainEntity) {
            TrainEntity contextEntity = (TrainEntity) host;
            return startManual45(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_joMlLooi2InkPOjk59H".equals(defKey) && host instanceof TrainEntity) {
            TrainEntity contextEntity = (TrainEntity) host;
            return startManual46(ctx, contextEntity, contextEntity, argOverrides);
        }
        if ("dv:designView_XLrKPtiJlmUbySIChuF".equals(defKey) && host instanceof TrainEntity) {
            TrainEntity contextEntity = (TrainEntity) host;
            return startManual47(ctx, contextEntity, contextEntity, argOverrides);
        }
        return null;
    }
}
