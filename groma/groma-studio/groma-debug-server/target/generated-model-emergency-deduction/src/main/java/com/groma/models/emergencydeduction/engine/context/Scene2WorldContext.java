package com.groma.models.emergencydeduction.engine.context;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.AreaWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.GeoPointEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithSectionEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.MetroLineEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallAffectedAreaEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction.model.entity.Scene2Entity;
import com.groma.models.emergencydeduction.model.entity.SectionWithEndStationEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithPassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainWithPassengerGroupEntity;
import com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Map;

/**
 * 场景单写者推演沙盒：场景2（方圆场景原名：场景2）
 * <p>
 * 严格绑定单线程无锁推进：世界初始化按画布绑定构建命名实例对象图，
 * 随后由底座按事件接线表派发对象事件、逐拍推进耗时过程。成员的每次写入都会经底座发出标准事件码。
 * </p>
 */
@GromaMeta(
        id = "fFnAAr3WKsN0Oaghvzj",
        zhName = "场景2",
        kind = "SCENE_CONTEXT"
)
public final class Scene2WorldContext extends GromaWorldContext {
    private Scene2Entity self;

    /**
     * @param initialOverrides 启动参数：{@code 实例ID.成员名} 定点覆盖；{@code 成员名} 覆盖场景根对象成员（初始化静默期内应用，不产生值变化事件）
     */
    public Scene2WorldContext(GromaModuleRuntime runtime, String sessionId, long startEpochMs,
            long finalStepMs, long seed, Map<String, Object> initialOverrides) {
        super(runtime, sessionId, "scene2", startEpochMs, finalStepMs, seed);
        setInitialOverrides(initialOverrides);
    }

    @Override
    protected BaseSimEntity sceneRootEntity() {
        return self;
    }

    @Override
    protected void buildInitialWorld() {
        this.self = new Scene2Entity("scene:fFnAAr3WKsN0Oaghvzj");
        // 降雨关联区域 = 引用命名实例 金水区720降雨
        this.self.setRainfallRelatedArea((RainfallAffectedAreaEntity) namedInstance("dKtTMHtTCZN3dw7ljZy"), this);
        addRootMember("OlRd4AmwiiBoH9RgrTR", this.self.getRainfallRelatedArea());
        // 区域与区域内车站 = 引用命名实例 金水区内的海滩寺站
        this.self.setAreaWithStation((AreaWithStationEntity) namedInstance("6gUAkitgiM4oTMlj59y"), this);
        addRootMember("LBJJ0xxTgEtmyb9GiZu", this.self.getAreaWithStation());
        // 区间与端点车站 = 引用命名实例 区间内海滩寺站端
        this.self.setSectionWithEndStation((SectionWithEndStationEntity) namedInstance("YVr0f8tm8qsEuoaep3m"), this);
        addRootMember("HgVFnRjnjDVAUqiEA2V", this.self.getSectionWithEndStation());
        // 线路与列车 = 引用命名实例 5号线0149次列车
        this.self.setLineWithTrain((LineWithTrainEntity) namedInstance("dD52dI1AfSeD9zQyafR"), this);
        addRootMember("xTCY2kcgoHtoR9oL0Ka", this.self.getLineWithTrain());
        register(this.self);
    }

    @Override
    protected BaseSimEntity newNamedInstance(String instanceId) {
        switch (instanceId) {
            case "3W75fTsANj7jDp3P7oB" -> {
                return buildHaitansiZhan();
            }
            case "6GlLmZabrMliB2iKHB4" -> {
                return buildZhengzhouDiTie5Line();
            }
            case "6gUAkitgiM4oTMlj59y" -> {
                return buildJinshuiNeiDeHaitansiZhan();
            }
            case "A4ULv7ovnsqRGpXqP3Z" -> {
                return buildShakouluInStationDe0149TrainPassengerGroup();
            }
            case "BELMhYQsxpgp8StG7aj" -> {
                return buildHaitansiZhan2();
            }
            case "Ciaaa5oHz6ridJOkK2S" -> {
                return buildXianHaitansiShakouluSection5();
            }
            case "EfbXSycFo0FtsPfAQkM" -> {
                return buildHaitansiDaoShakouluSectionNeiDe0149Train();
            }
            case "HfqZeInw14NsUx7cUHV" -> {
                return buildSectionNeiShakouluZhanDuan();
            }
            case "K0754OxBkLtnBzrXm2J" -> {
                return buildShakouluZhan();
            }
            case "NrnQHcdiiUxxVSC10Xu" -> {
                return buildHaitansiShakouluSection();
            }
            case "YIMXB3GlELKc1DTsAQ5" -> {
                return buildHaitansiZhanDockingDe0149Train();
            }
            case "YVr0f8tm8qsEuoaep3m" -> {
                return buildSectionNeiHaitansiZhanDuan();
            }
            case "dD52dI1AfSeD9zQyafR" -> {
                return buildXian0149Train5();
            }
            case "dKtTMHtTCZN3dw7ljZy" -> {
                return buildJinshui720Rainfall();
            }
            case "h47W9VmS1w6bzyZGYJ4" -> {
                return buildTrainShangDePassengerGroup0149();
            }
            case "j10iqJlAo2owghwRLln" -> {
                return buildZhengzhou720Rainstorm();
            }
            case "mwv7PC0YU9KFNACi8G0" -> {
                return buildShakouluZhanDockingDe0149Train();
            }
            case "oQU5zzFLJwxrRLNNWhA" -> {
                return buildTrain0149();
            }
            case "pdUOBRJTtts6pSqwlEi" -> {
                return buildJinshui();
            }
            case "rEkkNjmOrgwmtX6s9ON" -> {
                return buildXianHaitansiZhan5();
            }
            case "tEN79EKLkSyfOxx8yFJ" -> {
                return buildXianShakouluZhan5();
            }
            case "wO9MHANy27nJNB6rOhJ" -> {
                return buildShakouluDaoHaitansiSectionNeiDe0149Train();
            }
            case "xjjGyESXDNeSSxefhkm" -> {
                return buildTrainPassengerGroup0149();
            }
            case "xwVjjDbBTEurKGngsId" -> {
                return buildShakouluZhan2();
            }
            default -> { return null; }
        }
    }

    @Override
    protected Object newNamedCollection(String instanceId) {
        switch (instanceId) {
            case "eZJgQWqYwRKpOTcwSYQ" -> {
                return buildLineWithSectionSet();
            }
            case "ue1TzOuUYomKixhyqSU" -> {
                return buildLineWithStationSet();
            }
            default -> { return null; }
        }
    }

    /**
     * 命名实例：海滩寺站（方圆对象实体原名：海滩寺站；实例标识 3W75fTsANj7jDp3P7oB）
     */
    private StationEntity buildHaitansiZhan() {
        StationEntity entity = new StationEntity("3W75fTsANj7jDp3P7oB");
        // 站内积水量 = 0
        entity.setStationWaterVolume(0.0D);
        // 失效超高阈值 = 0.02
        entity.setFailureThreshold(0.02D);
        // 失效持续时间 = 60
        entity.setFailureDuration(60.0D);
        // 挡水墙顶高程 = 100.3
        entity.setFloodWallTopElevation(100.3D);
        // 名称 = 海滩寺站
        entity.setName("海滩寺站");
        // 排水额定能力 = 0
        entity.setRatedDrainCapacity(0.0D);
        // 站内底高程 = 95
        entity.setStationFloorElevation(95.0D);
        // 等效蓄水面积 = 1000
        entity.setEquivalentStorageArea(1000.0D);
        // 站内水深 = 0
        entity.setStationWaterDepth(0.0D);
        // 位置 = 引用命名实例 海滩寺站
        entity.setPosition((GeoPointEntity) namedInstance("BELMhYQsxpgp8StG7aj"), this);
        // 积水状态 = 无积水
        entity.setWaterloggingState(WaterloggingStateEnum.NO_WATERLOGGING);
        // 挡水墙状态 = 完好
        entity.setFloodWallState(FloodWallStateEnum.INTACT);
        // 完好时过水宽度 = 0.2
        entity.setIntactFlowWidth(0.2D);
        // 失效后过水宽度 = 1
        entity.setFailedFlowWidth(1.0D);
        // 入口底高程 = 100
        entity.setInletFloorElevation(100.0D);
        // 入口流量系数 = 0.36
        entity.setInletFlowCoeff(0.36D);
        // 超限累计时间 = 0
        entity.setOverlimitDuration(0.0D);
        return entity;
    }

    /**
     * 命名实例：郑州地铁5号线（方圆对象实体原名：郑州地铁5号线；实例标识 6GlLmZabrMliB2iKHB4）
     */
    private MetroLineEntity buildZhengzhouDiTie5Line() {
        MetroLineEntity entity = new MetroLineEntity("6GlLmZabrMliB2iKHB4");
        // 线路名称 = 郑州地铁5号线
        entity.setLineName("郑州地铁5号线");
        // 起点里程 = 0
        entity.setStartMileage(0.0D);
        // 终点里程 = 10000
        entity.setEndMileage(10000.0D);
        return entity;
    }

    /**
     * 命名实例：金水区内的海滩寺站（方圆对象实体原名：金水区内的海滩寺站；实例标识 6gUAkitgiM4oTMlj59y）
     */
    private AreaWithStationEntity buildJinshuiNeiDeHaitansiZhan() {
        AreaWithStationEntity entity = new AreaWithStationEntity("6gUAkitgiM4oTMlj59y");
        // 区域 = 引用命名实例 金水区
        entity.setArea((AreaEntity) namedInstance("pdUOBRJTtts6pSqwlEi"), this);
        // 车站 = 引用命名实例 海滩寺站
        entity.setStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        return entity;
    }

    /**
     * 命名实例：沙口路站内的0149次列车乘客群（方圆对象实体原名：沙口路站内的0149次列车乘客群；实例标识 A4ULv7ovnsqRGpXqP3Z）
     */
    private StationWithPassengerGroupEntity buildShakouluInStationDe0149TrainPassengerGroup() {
        StationWithPassengerGroupEntity entity = new StationWithPassengerGroupEntity("A4ULv7ovnsqRGpXqP3Z");
        // 乘客群 = 引用命名实例 0149次列车乘客群
        entity.setPassengerGroup((PassengerGroupEntity) namedInstance("xjjGyESXDNeSSxefhkm"), this);
        // 车站 = 引用命名实例 沙口路站
        entity.setStation((StationEntity) namedInstance("K0754OxBkLtnBzrXm2J"), this);
        return entity;
    }

    /**
     * 命名实例：海滩寺站（方圆对象实体原名：海滩寺站；实例标识 BELMhYQsxpgp8StG7aj）
     */
    private GeoPointEntity buildHaitansiZhan2() {
        GeoPointEntity entity = new GeoPointEntity("BELMhYQsxpgp8StG7aj");
        // 经度 = 113.650702
        entity.setLongitude(113.650702D);
        // 纬度 = 34.774286
        entity.setLatitude(34.774286D);
        return entity;
    }

    /**
     * 命名实例：5号线海滩寺—沙口路区间（方圆对象实体原名：5号线海滩寺—沙口路区间；实例标识 Ciaaa5oHz6ridJOkK2S）
     */
    private LineWithSectionEntity buildXianHaitansiShakouluSection5() {
        LineWithSectionEntity entity = new LineWithSectionEntity("Ciaaa5oHz6ridJOkK2S");
        // 地铁线路 = 引用命名实例 郑州地铁5号线
        entity.setMetroLine((MetroLineEntity) namedInstance("6GlLmZabrMliB2iKHB4"), this);
        // 地铁区间 = 引用命名实例 海滩寺—沙口路区间
        entity.setMetroSection((MetroSectionEntity) namedInstance("NrnQHcdiiUxxVSC10Xu"), this);
        // 区间起点里程 = 0
        entity.setSectionStartMileage(0.0D);
        // 区间终点里程 = 1027
        entity.setSectionEndMileage(1027.0D);
        return entity;
    }

    /**
     * 命名实例：海滩寺到沙口路区间内的0149次列车（方圆对象实体原名：海滩寺到沙口路区间内的0149次列车；实例标识 EfbXSycFo0FtsPfAQkM）
     */
    private SectionWithTrainEntity buildHaitansiDaoShakouluSectionNeiDe0149Train() {
        SectionWithTrainEntity entity = new SectionWithTrainEntity("EfbXSycFo0FtsPfAQkM");
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 区间 = 引用命名实例 海滩寺—沙口路区间
        entity.setSection((MetroSectionEntity) namedInstance("NrnQHcdiiUxxVSC10Xu"), this);
        // 已行驶距离 = 0
        entity.setTravelledDistance(0.0D);
        // 目标车站 = 引用命名实例 沙口路站
        entity.setTargetStation((StationEntity) namedInstance("K0754OxBkLtnBzrXm2J"), this);
        // 出发车站 = 引用命名实例 海滩寺站
        entity.setDepartureStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        return entity;
    }

    /**
     * 命名实例：区间内沙口路站端（方圆对象实体原名：区间内沙口路站端；实例标识 HfqZeInw14NsUx7cUHV）
     */
    private SectionWithEndStationEntity buildSectionNeiShakouluZhanDuan() {
        SectionWithEndStationEntity entity = new SectionWithEndStationEntity("HfqZeInw14NsUx7cUHV");
        // 区间 = 引用命名实例 海滩寺—沙口路区间
        entity.setSection((MetroSectionEntity) namedInstance("NrnQHcdiiUxxVSC10Xu"), this);
        return entity;
    }

    /**
     * 命名实例：沙口路站（方圆对象实体原名：沙口路站；实例标识 K0754OxBkLtnBzrXm2J）
     */
    private StationEntity buildShakouluZhan() {
        StationEntity entity = new StationEntity("K0754OxBkLtnBzrXm2J");
        // 站内积水量 = 0.3
        entity.setStationWaterVolume(0.3D);
        // 失效超高阈值 = 0.02
        entity.setFailureThreshold(0.02D);
        // 失效持续时间 = 60
        entity.setFailureDuration(60.0D);
        // 挡水墙顶高程 = 100.3
        entity.setFloodWallTopElevation(100.3D);
        // 名称 = 沙口路站
        entity.setName("沙口路站");
        // 位置 = 引用命名实例 沙口路站
        entity.setPosition((GeoPointEntity) namedInstance("xwVjjDbBTEurKGngsId"), this);
        return entity;
    }

    /**
     * 命名实例：海滩寺—沙口路区间（方圆对象实体原名：海滩寺—沙口路区间；实例标识 NrnQHcdiiUxxVSC10Xu）
     */
    private MetroSectionEntity buildHaitansiShakouluSection() {
        MetroSectionEntity entity = new MetroSectionEntity("NrnQHcdiiUxxVSC10Xu");
        // 长度 = 1027
        entity.setLength(1027.0D);
        // 等效储水宽度 = 3
        entity.setEquivalentStorageWidth(3.0D);
        // 危险水深 = 0.1
        entity.setDangerDepth(0.1D);
        // 迫停水深 = 0.15
        entity.setForcedStopDepth(0.15D);
        // 积水量 = 0
        entity.setWaterVolume(0.0D);
        // 代表性水深 = 0
        entity.setRepresentativeDepth(0.0D);
        // 是否放行 = true
        entity.setReleaseAllowed(true);
        // 名称 = 海滩寺—沙口路区间
        entity.setName("海滩寺—沙口路区间");
        // 代表底高程 = 95
        entity.setRepresentativeFloorElevation(95.0D);
        // 积水状态 = 无积水
        entity.setWaterloggingState(WaterloggingStateEnum.NO_WATERLOGGING);
        return entity;
    }

    /**
     * 命名实例：海滩寺站停靠的0149次列车（方圆对象实体原名：海滩寺站停靠的0149次列车；实例标识 YIMXB3GlELKc1DTsAQ5）
     */
    private StationWithParkedTrainEntity buildHaitansiZhanDockingDe0149Train() {
        StationWithParkedTrainEntity entity = new StationWithParkedTrainEntity("YIMXB3GlELKc1DTsAQ5");
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 车站 = 引用命名实例 海滩寺站
        entity.setStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        // 规定停站时长 = 30
        entity.setPrescribedStopDuration(30L);
        // 离站是否允许 = true
        entity.setDepartureAllowed(true);
        return entity;
    }

    /**
     * 命名实例：区间内海滩寺站端（方圆对象实体原名：区间内海滩寺站端；实例标识 YVr0f8tm8qsEuoaep3m）
     */
    private SectionWithEndStationEntity buildSectionNeiHaitansiZhanDuan() {
        SectionWithEndStationEntity entity = new SectionWithEndStationEntity("YVr0f8tm8qsEuoaep3m");
        // 车站 = 引用命名实例 海滩寺站
        entity.setStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        // 区间 = 引用命名实例 海滩寺—沙口路区间
        entity.setSection((MetroSectionEntity) namedInstance("NrnQHcdiiUxxVSC10Xu"), this);
        // 连接处底高程 = 95
        entity.setJunctionFloorElevation(95.0D);
        // 有效过水宽度 = 1
        entity.setEffectiveFlowWidth(1.0D);
        // 水流系数 = 0.4
        entity.setFlowCoeff(0.4D);
        return entity;
    }

    /**
     * 命名实例：5号线0149次列车（方圆对象实体原名：5号线0149次列车；实例标识 dD52dI1AfSeD9zQyafR）
     */
    private LineWithTrainEntity buildXian0149Train5() {
        LineWithTrainEntity entity = new LineWithTrainEntity("dD52dI1AfSeD9zQyafR");
        // 线路 = 引用命名实例 郑州地铁5号线
        entity.setLine((MetroLineEntity) namedInstance("6GlLmZabrMliB2iKHB4"), this);
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 沿里程增大方向 = true
        entity.setAlongIncreasingMileage(true);
        // 演示往返 = true
        entity.setDemoRoundTrip(true);
        return entity;
    }

    /**
     * 命名实例：金水区720降雨（方圆对象实体原名：金水区720降雨；实例标识 dKtTMHtTCZN3dw7ljZy）
     */
    private RainfallAffectedAreaEntity buildJinshui720Rainfall() {
        RainfallAffectedAreaEntity entity = new RainfallAffectedAreaEntity("dKtTMHtTCZN3dw7ljZy");
        // 降雨 = 引用命名实例 郑州720暴雨
        entity.setRainfall((RainfallEntity) namedInstance("j10iqJlAo2owghwRLln"), this);
        // 区域 = 引用命名实例 金水区
        entity.setArea((AreaEntity) namedInstance("pdUOBRJTtts6pSqwlEi"), this);
        // 当前雨强 = 1.5
        entity.setCurrentRainIntensity(1.5D);
        // 累计雨量 = 0
        entity.setCumulativeRainfall(0.0D);
        // 雨量序列 = 映射，3 个键值对
        entity.setRainfallSeries(newInline0());
        return entity;
    }

    /**
     * 命名实例：0149次列车上的乘客群（方圆对象实体原名：0149次列车上的乘客群；实例标识 h47W9VmS1w6bzyZGYJ4）
     */
    private TrainWithPassengerGroupEntity buildTrainShangDePassengerGroup0149() {
        TrainWithPassengerGroupEntity entity = new TrainWithPassengerGroupEntity("h47W9VmS1w6bzyZGYJ4");
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 乘客群 = 引用命名实例 0149次列车乘客群
        entity.setPassengerGroup((PassengerGroupEntity) namedInstance("xjjGyESXDNeSSxefhkm"), this);
        return entity;
    }

    /**
     * 命名实例：郑州720暴雨（方圆对象实体原名：郑州720暴雨；实例标识 j10iqJlAo2owghwRLln）
     */
    private RainfallEntity buildZhengzhou720Rainstorm() {
        RainfallEntity entity = new RainfallEntity("j10iqJlAo2owghwRLln");
        return entity;
    }

    /**
     * 命名实例：沙口路站停靠的0149次列车（方圆对象实体原名：沙口路站停靠的0149次列车；实例标识 mwv7PC0YU9KFNACi8G0）
     */
    private StationWithParkedTrainEntity buildShakouluZhanDockingDe0149Train() {
        StationWithParkedTrainEntity entity = new StationWithParkedTrainEntity("mwv7PC0YU9KFNACi8G0");
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 车站 = 引用命名实例 沙口路站
        entity.setStation((StationEntity) namedInstance("K0754OxBkLtnBzrXm2J"), this);
        // 规定停站时长 = 30
        entity.setPrescribedStopDuration(30L);
        // 离站是否允许 = true
        entity.setDepartureAllowed(true);
        return entity;
    }

    /**
     * 命名实例：0149次列车（方圆对象实体原名：0149次列车；实例标识 oQU5zzFLJwxrRLNNWhA）
     */
    private TrainEntity buildTrain0149() {
        TrainEntity entity = new TrainEntity("oQU5zzFLJwxrRLNNWhA");
        // 当前速度 = 0
        entity.setCurrentSpeed(0.0D);
        // 名称 = 0149次列车
        entity.setName("0149次列车");
        // 列车状态 = 待进入
        entity.setTrainState(TrainStateEnum.PENDING_ENTRY);
        // 行驶速度 = 300
        entity.setTravelSpeed(300.0D);
        return entity;
    }

    /**
     * 命名实例：金水区（方圆对象实体原名：金水区；实例标识 pdUOBRJTtts6pSqwlEi）
     */
    private AreaEntity buildJinshui() {
        AreaEntity entity = new AreaEntity("pdUOBRJTtts6pSqwlEi");
        // 汇水面积 = 10000
        entity.setCatchmentArea(10000.0D);
        // 径流系数 = 0.8
        entity.setRunoffCoeff(0.8D);
        // 等效蓄水面积 = 1000
        entity.setEquivalentStorageArea(1000.0D);
        // 地面高程 = 100
        entity.setGroundElevation(100.0D);
        // 地表排水能力 = 2
        entity.setSurfaceDrainCapacity(2.0D);
        // 积水深度 = 0
        entity.setWaterDepth(0.0D);
        // 名称 = 金水区
        entity.setName("金水区");
        // 积水状态 = 无积水
        entity.setWaterloggingState(WaterloggingStateEnum.NO_WATERLOGGING);
        return entity;
    }

    /**
     * 命名实例：5号线海滩寺站（方圆对象实体原名：5号线海滩寺站；实例标识 rEkkNjmOrgwmtX6s9ON）
     */
    private LineWithStationEntity buildXianHaitansiZhan5() {
        LineWithStationEntity entity = new LineWithStationEntity("rEkkNjmOrgwmtX6s9ON");
        // 地铁线路 = 引用命名实例 郑州地铁5号线
        entity.setMetroLine((MetroLineEntity) namedInstance("6GlLmZabrMliB2iKHB4"), this);
        // 车站 = 引用命名实例 海滩寺站
        entity.setStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        // 线路里程 = 0
        entity.setLineMileage(0.0D);
        return entity;
    }

    /**
     * 命名实例：5号线沙口路站（方圆对象实体原名：5号线沙口路站；实例标识 tEN79EKLkSyfOxx8yFJ）
     */
    private LineWithStationEntity buildXianShakouluZhan5() {
        LineWithStationEntity entity = new LineWithStationEntity("tEN79EKLkSyfOxx8yFJ");
        // 地铁线路 = 引用命名实例 郑州地铁5号线
        entity.setMetroLine((MetroLineEntity) namedInstance("6GlLmZabrMliB2iKHB4"), this);
        // 车站 = 引用命名实例 沙口路站
        entity.setStation((StationEntity) namedInstance("K0754OxBkLtnBzrXm2J"), this);
        // 线路里程 = 1027
        entity.setLineMileage(1027.0D);
        return entity;
    }

    /**
     * 命名实例：沙口路到海滩寺区间内的0149次列车（方圆对象实体原名：沙口路到海滩寺区间内的0149次列车；实例标识 wO9MHANy27nJNB6rOhJ）
     */
    private SectionWithTrainEntity buildShakouluDaoHaitansiSectionNeiDe0149Train() {
        SectionWithTrainEntity entity = new SectionWithTrainEntity("wO9MHANy27nJNB6rOhJ");
        // 列车 = 引用命名实例 0149次列车
        entity.setTrain((TrainEntity) namedInstance("oQU5zzFLJwxrRLNNWhA"), this);
        // 区间 = 引用命名实例 海滩寺—沙口路区间
        entity.setSection((MetroSectionEntity) namedInstance("NrnQHcdiiUxxVSC10Xu"), this);
        // 已行驶距离 = 0
        entity.setTravelledDistance(0.0D);
        // 目标车站 = 引用命名实例 海滩寺站
        entity.setTargetStation((StationEntity) namedInstance("3W75fTsANj7jDp3P7oB"), this);
        // 出发车站 = 引用命名实例 沙口路站
        entity.setDepartureStation((StationEntity) namedInstance("K0754OxBkLtnBzrXm2J"), this);
        return entity;
    }

    /**
     * 命名实例：0149次列车乘客群（方圆对象实体原名：0149次列车乘客群；实例标识 xjjGyESXDNeSSxefhkm）
     */
    private PassengerGroupEntity buildTrainPassengerGroup0149() {
        PassengerGroupEntity entity = new PassengerGroupEntity("xjjGyESXDNeSSxefhkm");
        return entity;
    }

    /**
     * 命名实例：沙口路站（方圆对象实体原名：沙口路站；实例标识 xwVjjDbBTEurKGngsId）
     */
    private GeoPointEntity buildShakouluZhan2() {
        GeoPointEntity entity = new GeoPointEntity("xwVjjDbBTEurKGngsId");
        // 经度 = 113.640426
        entity.setLongitude(113.640426D);
        // 纬度 = 34.770541
        entity.setLatitude(34.770541D);
        return entity;
    }

    /**
     * 命名集合实例：线路与区间集合（方圆对象实体原名：线路与区间集合；实例标识 eZJgQWqYwRKpOTcwSYQ）
     */
    private List<LineWithSectionEntity> buildLineWithSectionSet() {
        return new java.util.ArrayList<>(java.util.List.of((LineWithSectionEntity) namedInstance("Ciaaa5oHz6ridJOkK2S")));
    }

    /**
     * 命名集合实例：线路与车站集合（方圆对象实体原名：线路与车站集合；实例标识 ue1TzOuUYomKixhyqSU）
     */
    private List<LineWithStationEntity> buildLineWithStationSet() {
        return new java.util.ArrayList<>(java.util.List.of((LineWithStationEntity) namedInstance("rEkkNjmOrgwmtX6s9ON"), (LineWithStationEntity) namedInstance("tEN79EKLkSyfOxx8yFJ")));
    }

    private Map<Long, Double> newInline0() {
        Map<Long, Double> value = new java.util.LinkedHashMap<>();
        value.put(1626696000000L, 1.67D);
        value.put(1626699600000L, 0.83D);
        value.put(1626703200000L, 2.5D);
        return value;
    }
}
