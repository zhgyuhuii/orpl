package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.StationReadOnly;
import com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum;
import com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 车站实体（方圆对象类型原名：车站）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>站内积水量（stationWaterVolume，BASE，单位 m³）</li>
 * <li>失效超高阈值（failureThreshold，BASE，单位 m）</li>
 * <li>失效持续时间（failureDuration，BASE，单位 s）</li>
 * <li>挡水墙顶高程（floodWallTopElevation，BASE，单位 m）</li>
 * <li>名称（name，BASE）</li>
 * <li>排水额定能力（ratedDrainCapacity，BASE，单位 m³/min）</li>
 * <li>站内底高程（stationFloorElevation，BASE，单位 m）</li>
 * <li>等效蓄水面积（equivalentStorageArea，BASE，单位 m²）</li>
 * <li>站内水深（stationWaterDepth，BASE，单位 m）</li>
 * <li>疏散能力（evacuationCapacity，BASE，单位 人/min）</li>
 * <li>位置（position，REF）</li>
 * <li>积水状态（waterloggingState，ENUM）</li>
 * <li>挡水墙状态（floodWallState，ENUM）</li>
 * <li>完好时过水宽度（intactFlowWidth，BASE，单位 m）</li>
 * <li>失效后过水宽度（failedFlowWidth，BASE，单位 m）</li>
 * <li>入口底高程（inletFloorElevation，BASE，单位 m）</li>
 * <li>入口流量系数（inletFlowCoeff，BASE）</li>
 * <li>超限累计时间（overlimitDuration，BASE，单位 s）</li>
 * </ul>
 */
@Schema(
        title = "车站",
        description = "方圆对象类型：车站"
)
@GromaMeta(
        id = "rFXJKdFfEChoRdeX7aT",
        zhName = "车站",
        kind = "TYPE"
)
public class StationEntity extends BaseSimEntity implements StationReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("ZSCBmOks5hfDvQgGwnd", "站内积水量", "stationWaterVolume", "BASE", "double", "m³"), new GromaMemberMeta("9cgmJtgAfA5THCTQX1Q", "失效超高阈值", "failureThreshold", "BASE", "double", "m"), new GromaMemberMeta("2x8UFMHABjbk7XdvGSq", "失效持续时间", "failureDuration", "BASE", "double", "s"), new GromaMemberMeta("HsJlhF4Qq0meQjVXQl5", "挡水墙顶高程", "floodWallTopElevation", "BASE", "double", "m"), new GromaMemberMeta("icz3z4NdDl4XzMF1lEA", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("ySKDJJ9ZM0cRG8ny4A4", "排水额定能力", "ratedDrainCapacity", "BASE", "double", "m³/min"), new GromaMemberMeta("JbCoZ3N3pGfxVn1fAHA", "站内底高程", "stationFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("TGW59nqUVYBoQzfbnSa", "等效蓄水面积", "equivalentStorageArea", "BASE", "double", "m²"), new GromaMemberMeta("4BkvxU1jidI2jo6QEpq", "站内水深", "stationWaterDepth", "BASE", "double", "m"), new GromaMemberMeta("xLpWusnJR2Aw0xM8gei", "疏散能力", "evacuationCapacity", "BASE", "double", "人/min"), new GromaMemberMeta("bsd5Noel9k5tlgpoOWP", "位置", "position", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.GeoPointEntity", ""), new GromaMemberMeta("ZYx31ZY8GstGNur40z2", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.WaterloggingStateEnum", ""), new GromaMemberMeta("KsKWUZghMV0qp4PvIP9", "挡水墙状态", "floodWallState", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.FloodWallStateEnum", ""), new GromaMemberMeta("4y48YnVOoSCjQWsqZcs", "完好时过水宽度", "intactFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("TmBu06RLACpGgpetjrU", "失效后过水宽度", "failedFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("lcRigegnMIZiDTVJben", "入口底高程", "inletFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("CbhmI1QG31xuE1fsd8M", "入口流量系数", "inletFlowCoeff", "BASE", "double", ""), new GromaMemberMeta("SnxMu3A2siAAnOyycRr", "超限累计时间", "overlimitDuration", "BASE", "double", "s"));

    /**
     * 站内积水量（方圆属性成员原名：站内积水量） 单位：m³
     */
    @Schema(
            title = "站内积水量"
    )
    @GromaMeta(
            id = "ZSCBmOks5hfDvQgGwnd",
            zhName = "站内积水量",
            kind = "FIELD",
            unit = "m³"
    )
    private double stationWaterVolume = 0.0D;

    /**
     * 失效超高阈值（方圆属性成员原名：失效超高阈值） 单位：m 默认值：0.02
     */
    @Schema(
            title = "失效超高阈值"
    )
    @GromaMeta(
            id = "9cgmJtgAfA5THCTQX1Q",
            zhName = "失效超高阈值",
            kind = "FIELD",
            unit = "m"
    )
    private double failureThreshold = 0.02D;

    /**
     * 失效持续时间（方圆属性成员原名：失效持续时间） 单位：s 默认值：60
     */
    @Schema(
            title = "失效持续时间"
    )
    @GromaMeta(
            id = "2x8UFMHABjbk7XdvGSq",
            zhName = "失效持续时间",
            kind = "FIELD",
            unit = "s"
    )
    private double failureDuration = 60.0D;

    /**
     * 挡水墙顶高程（方圆属性成员原名：挡水墙顶高程） 单位：m 默认值：100.3
     */
    @Schema(
            title = "挡水墙顶高程"
    )
    @GromaMeta(
            id = "HsJlhF4Qq0meQjVXQl5",
            zhName = "挡水墙顶高程",
            kind = "FIELD",
            unit = "m"
    )
    private double floodWallTopElevation = 100.3D;

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "icz3z4NdDl4XzMF1lEA",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 排水额定能力（方圆属性成员原名：排水额定能力） 单位：m³/min
     */
    @Schema(
            title = "排水额定能力"
    )
    @GromaMeta(
            id = "ySKDJJ9ZM0cRG8ny4A4",
            zhName = "排水额定能力",
            kind = "FIELD",
            unit = "m³/min"
    )
    private double ratedDrainCapacity = 0.0D;

    /**
     * 站内底高程（方圆属性成员原名：站内底高程） 单位：m
     */
    @Schema(
            title = "站内底高程"
    )
    @GromaMeta(
            id = "JbCoZ3N3pGfxVn1fAHA",
            zhName = "站内底高程",
            kind = "FIELD",
            unit = "m"
    )
    private double stationFloorElevation = 0.0D;

    /**
     * 等效蓄水面积（方圆属性成员原名：等效蓄水面积） 单位：m²
     */
    @Schema(
            title = "等效蓄水面积"
    )
    @GromaMeta(
            id = "TGW59nqUVYBoQzfbnSa",
            zhName = "等效蓄水面积",
            kind = "FIELD",
            unit = "m²"
    )
    private double equivalentStorageArea = 0.0D;

    /**
     * 站内水深（方圆属性成员原名：站内水深） 单位：m
     */
    @Schema(
            title = "站内水深"
    )
    @GromaMeta(
            id = "4BkvxU1jidI2jo6QEpq",
            zhName = "站内水深",
            kind = "FIELD",
            unit = "m"
    )
    private double stationWaterDepth = 0.0D;

    /**
     * 疏散能力（方圆属性成员原名：疏散能力） 单位：人/min
     */
    @Schema(
            title = "疏散能力"
    )
    @GromaMeta(
            id = "xLpWusnJR2Aw0xM8gei",
            zhName = "疏散能力",
            kind = "FIELD",
            unit = "人/min"
    )
    private double evacuationCapacity = 0.0D;

    /**
     * 位置（方圆对象成员原名：位置）
     */
    @Schema(
            title = "位置"
    )
    @GromaMeta(
            id = "bsd5Noel9k5tlgpoOWP",
            zhName = "位置",
            kind = "FIELD"
    )
    private GeoPointEntity position = null;

    /**
     * 积水状态（方圆状态成员原名：积水状态）
     */
    @Schema(
            title = "积水状态"
    )
    @GromaMeta(
            id = "ZYx31ZY8GstGNur40z2",
            zhName = "积水状态",
            kind = "FIELD"
    )
    private WaterloggingStateEnum waterloggingState = WaterloggingStateEnum.NO_WATERLOGGING;

    /**
     * 挡水墙状态（方圆状态成员原名：挡水墙状态）
     */
    @Schema(
            title = "挡水墙状态"
    )
    @GromaMeta(
            id = "KsKWUZghMV0qp4PvIP9",
            zhName = "挡水墙状态",
            kind = "FIELD"
    )
    private FloodWallStateEnum floodWallState = FloodWallStateEnum.INTACT;

    /**
     * 完好时过水宽度（方圆属性成员原名：完好时过水宽度） 单位：m
     */
    @Schema(
            title = "完好时过水宽度"
    )
    @GromaMeta(
            id = "4y48YnVOoSCjQWsqZcs",
            zhName = "完好时过水宽度",
            kind = "FIELD",
            unit = "m"
    )
    private double intactFlowWidth = 0.0D;

    /**
     * 失效后过水宽度（方圆属性成员原名：失效后过水宽度） 单位：m
     */
    @Schema(
            title = "失效后过水宽度"
    )
    @GromaMeta(
            id = "TmBu06RLACpGgpetjrU",
            zhName = "失效后过水宽度",
            kind = "FIELD",
            unit = "m"
    )
    private double failedFlowWidth = 0.0D;

    /**
     * 入口底高程（方圆属性成员原名：入口底高程） 单位：m
     */
    @Schema(
            title = "入口底高程"
    )
    @GromaMeta(
            id = "lcRigegnMIZiDTVJben",
            zhName = "入口底高程",
            kind = "FIELD",
            unit = "m"
    )
    private double inletFloorElevation = 0.0D;

    /**
     * 入口流量系数（方圆属性成员原名：入口流量系数）
     */
    @Schema(
            title = "入口流量系数"
    )
    @GromaMeta(
            id = "CbhmI1QG31xuE1fsd8M",
            zhName = "入口流量系数",
            kind = "FIELD"
    )
    private double inletFlowCoeff = 0.0D;

    /**
     * 超限累计时间（方圆属性成员原名：超限累计时间） 单位：s
     */
    @Schema(
            title = "超限累计时间"
    )
    @GromaMeta(
            id = "SnxMu3A2siAAnOyycRr",
            zhName = "超限累计时间",
            kind = "FIELD",
            unit = "s"
    )
    private double overlimitDuration = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public StationEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Station";
    }

    @Override
    public String getTypeZhName() {
        return "车站";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public double getStationWaterVolume() {
        return this.stationWaterVolume;
    }

    /**
     * 写入站内积水量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStationWaterVolume(double value, GromaWorldContext ctx) {
        if (this.stationWaterVolume == value) {
            return;
        }
        double before = this.stationWaterVolume;
        this.stationWaterVolume = value;
        markDirty(0);
        ctx.onMemberChanged(this, "ZSCBmOks5hfDvQgGwnd", before, value);
    }

    /**
     * 直接写入站内积水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setStationWaterVolume(double value) {
        this.stationWaterVolume = value;
    }

    @Override
    public double getFailureThreshold() {
        return this.failureThreshold;
    }

    /**
     * 写入失效超高阈值并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFailureThreshold(double value, GromaWorldContext ctx) {
        if (this.failureThreshold == value) {
            return;
        }
        double before = this.failureThreshold;
        this.failureThreshold = value;
        markDirty(1);
        ctx.onMemberChanged(this, "9cgmJtgAfA5THCTQX1Q", before, value);
    }

    /**
     * 直接写入失效超高阈值（不发事件；仅用于初始化与对象创建期）
     */
    public void setFailureThreshold(double value) {
        this.failureThreshold = value;
    }

    @Override
    public double getFailureDuration() {
        return this.failureDuration;
    }

    /**
     * 写入失效持续时间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFailureDuration(double value, GromaWorldContext ctx) {
        if (this.failureDuration == value) {
            return;
        }
        double before = this.failureDuration;
        this.failureDuration = value;
        markDirty(2);
        ctx.onMemberChanged(this, "2x8UFMHABjbk7XdvGSq", before, value);
    }

    /**
     * 直接写入失效持续时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setFailureDuration(double value) {
        this.failureDuration = value;
    }

    @Override
    public double getFloodWallTopElevation() {
        return this.floodWallTopElevation;
    }

    /**
     * 写入挡水墙顶高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFloodWallTopElevation(double value, GromaWorldContext ctx) {
        if (this.floodWallTopElevation == value) {
            return;
        }
        double before = this.floodWallTopElevation;
        this.floodWallTopElevation = value;
        markDirty(3);
        ctx.onMemberChanged(this, "HsJlhF4Qq0meQjVXQl5", before, value);
    }

    /**
     * 直接写入挡水墙顶高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setFloodWallTopElevation(double value) {
        this.floodWallTopElevation = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 写入名称并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setName(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.name, value)) {
            return;
        }
        String before = this.name;
        this.name = value;
        markDirty(4);
        ctx.onMemberChanged(this, "icz3z4NdDl4XzMF1lEA", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public double getRatedDrainCapacity() {
        return this.ratedDrainCapacity;
    }

    /**
     * 写入排水额定能力并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setRatedDrainCapacity(double value, GromaWorldContext ctx) {
        if (this.ratedDrainCapacity == value) {
            return;
        }
        double before = this.ratedDrainCapacity;
        this.ratedDrainCapacity = value;
        markDirty(5);
        ctx.onMemberChanged(this, "ySKDJJ9ZM0cRG8ny4A4", before, value);
    }

    /**
     * 直接写入排水额定能力（不发事件；仅用于初始化与对象创建期）
     */
    public void setRatedDrainCapacity(double value) {
        this.ratedDrainCapacity = value;
    }

    @Override
    public double getStationFloorElevation() {
        return this.stationFloorElevation;
    }

    /**
     * 写入站内底高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStationFloorElevation(double value, GromaWorldContext ctx) {
        if (this.stationFloorElevation == value) {
            return;
        }
        double before = this.stationFloorElevation;
        this.stationFloorElevation = value;
        markDirty(6);
        ctx.onMemberChanged(this, "JbCoZ3N3pGfxVn1fAHA", before, value);
    }

    /**
     * 直接写入站内底高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setStationFloorElevation(double value) {
        this.stationFloorElevation = value;
    }

    @Override
    public double getEquivalentStorageArea() {
        return this.equivalentStorageArea;
    }

    /**
     * 写入等效蓄水面积并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEquivalentStorageArea(double value, GromaWorldContext ctx) {
        if (this.equivalentStorageArea == value) {
            return;
        }
        double before = this.equivalentStorageArea;
        this.equivalentStorageArea = value;
        markDirty(7);
        ctx.onMemberChanged(this, "TGW59nqUVYBoQzfbnSa", before, value);
    }

    /**
     * 直接写入等效蓄水面积（不发事件；仅用于初始化与对象创建期）
     */
    public void setEquivalentStorageArea(double value) {
        this.equivalentStorageArea = value;
    }

    @Override
    public double getStationWaterDepth() {
        return this.stationWaterDepth;
    }

    /**
     * 写入站内水深并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStationWaterDepth(double value, GromaWorldContext ctx) {
        if (this.stationWaterDepth == value) {
            return;
        }
        double before = this.stationWaterDepth;
        this.stationWaterDepth = value;
        markDirty(8);
        ctx.onMemberChanged(this, "4BkvxU1jidI2jo6QEpq", before, value);
    }

    /**
     * 直接写入站内水深（不发事件；仅用于初始化与对象创建期）
     */
    public void setStationWaterDepth(double value) {
        this.stationWaterDepth = value;
    }

    @Override
    public double getEvacuationCapacity() {
        return this.evacuationCapacity;
    }

    /**
     * 写入疏散能力并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEvacuationCapacity(double value, GromaWorldContext ctx) {
        if (this.evacuationCapacity == value) {
            return;
        }
        double before = this.evacuationCapacity;
        this.evacuationCapacity = value;
        markDirty(9);
        ctx.onMemberChanged(this, "xLpWusnJR2Aw0xM8gei", before, value);
    }

    /**
     * 直接写入疏散能力（不发事件；仅用于初始化与对象创建期）
     */
    public void setEvacuationCapacity(double value) {
        this.evacuationCapacity = value;
    }

    @Override
    public GeoPointEntity getPosition() {
        return this.position;
    }

    /**
     * 写入位置并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPosition(GeoPointEntity value, GromaWorldContext ctx) {
        if (this.position == value) {
            return;
        }
        GeoPointEntity before = this.position;
        this.position = value;
        markDirty(10);
        ctx.onMemberChanged(this, "bsd5Noel9k5tlgpoOWP", before, value);
    }

    @Override
    public WaterloggingStateEnum getWaterloggingState() {
        return this.waterloggingState;
    }

    /**
     * 写入积水状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setWaterloggingState(WaterloggingStateEnum value, GromaWorldContext ctx) {
        if (this.waterloggingState == value) {
            return;
        }
        WaterloggingStateEnum before = this.waterloggingState;
        this.waterloggingState = value;
        markDirty(11);
        ctx.onMemberChanged(this, "ZYx31ZY8GstGNur40z2", before, value);
    }

    /**
     * 直接写入积水状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setWaterloggingState(WaterloggingStateEnum value) {
        this.waterloggingState = value;
    }

    @Override
    public FloodWallStateEnum getFloodWallState() {
        return this.floodWallState;
    }

    /**
     * 写入挡水墙状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFloodWallState(FloodWallStateEnum value, GromaWorldContext ctx) {
        if (this.floodWallState == value) {
            return;
        }
        FloodWallStateEnum before = this.floodWallState;
        this.floodWallState = value;
        markDirty(12);
        ctx.onMemberChanged(this, "KsKWUZghMV0qp4PvIP9", before, value);
    }

    /**
     * 直接写入挡水墙状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setFloodWallState(FloodWallStateEnum value) {
        this.floodWallState = value;
    }

    @Override
    public double getIntactFlowWidth() {
        return this.intactFlowWidth;
    }

    /**
     * 写入完好时过水宽度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setIntactFlowWidth(double value, GromaWorldContext ctx) {
        if (this.intactFlowWidth == value) {
            return;
        }
        double before = this.intactFlowWidth;
        this.intactFlowWidth = value;
        markDirty(13);
        ctx.onMemberChanged(this, "4y48YnVOoSCjQWsqZcs", before, value);
    }

    /**
     * 直接写入完好时过水宽度（不发事件；仅用于初始化与对象创建期）
     */
    public void setIntactFlowWidth(double value) {
        this.intactFlowWidth = value;
    }

    @Override
    public double getFailedFlowWidth() {
        return this.failedFlowWidth;
    }

    /**
     * 写入失效后过水宽度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFailedFlowWidth(double value, GromaWorldContext ctx) {
        if (this.failedFlowWidth == value) {
            return;
        }
        double before = this.failedFlowWidth;
        this.failedFlowWidth = value;
        markDirty(14);
        ctx.onMemberChanged(this, "TmBu06RLACpGgpetjrU", before, value);
    }

    /**
     * 直接写入失效后过水宽度（不发事件；仅用于初始化与对象创建期）
     */
    public void setFailedFlowWidth(double value) {
        this.failedFlowWidth = value;
    }

    @Override
    public double getInletFloorElevation() {
        return this.inletFloorElevation;
    }

    /**
     * 写入入口底高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setInletFloorElevation(double value, GromaWorldContext ctx) {
        if (this.inletFloorElevation == value) {
            return;
        }
        double before = this.inletFloorElevation;
        this.inletFloorElevation = value;
        markDirty(15);
        ctx.onMemberChanged(this, "lcRigegnMIZiDTVJben", before, value);
    }

    /**
     * 直接写入入口底高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setInletFloorElevation(double value) {
        this.inletFloorElevation = value;
    }

    @Override
    public double getInletFlowCoeff() {
        return this.inletFlowCoeff;
    }

    /**
     * 写入入口流量系数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setInletFlowCoeff(double value, GromaWorldContext ctx) {
        if (this.inletFlowCoeff == value) {
            return;
        }
        double before = this.inletFlowCoeff;
        this.inletFlowCoeff = value;
        markDirty(16);
        ctx.onMemberChanged(this, "CbhmI1QG31xuE1fsd8M", before, value);
    }

    /**
     * 直接写入入口流量系数（不发事件；仅用于初始化与对象创建期）
     */
    public void setInletFlowCoeff(double value) {
        this.inletFlowCoeff = value;
    }

    @Override
    public double getOverlimitDuration() {
        return this.overlimitDuration;
    }

    /**
     * 写入超限累计时间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setOverlimitDuration(double value, GromaWorldContext ctx) {
        if (this.overlimitDuration == value) {
            return;
        }
        double before = this.overlimitDuration;
        this.overlimitDuration = value;
        markDirty(17);
        ctx.onMemberChanged(this, "SnxMu3A2siAAnOyycRr", before, value);
    }

    /**
     * 直接写入超限累计时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setOverlimitDuration(double value) {
        this.overlimitDuration = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "ZSCBmOks5hfDvQgGwnd" -> {
                return this.stationWaterVolume;
            }
            case "9cgmJtgAfA5THCTQX1Q" -> {
                return this.failureThreshold;
            }
            case "2x8UFMHABjbk7XdvGSq" -> {
                return this.failureDuration;
            }
            case "HsJlhF4Qq0meQjVXQl5" -> {
                return this.floodWallTopElevation;
            }
            case "icz3z4NdDl4XzMF1lEA" -> {
                return this.name;
            }
            case "ySKDJJ9ZM0cRG8ny4A4" -> {
                return this.ratedDrainCapacity;
            }
            case "JbCoZ3N3pGfxVn1fAHA" -> {
                return this.stationFloorElevation;
            }
            case "TGW59nqUVYBoQzfbnSa" -> {
                return this.equivalentStorageArea;
            }
            case "4BkvxU1jidI2jo6QEpq" -> {
                return this.stationWaterDepth;
            }
            case "xLpWusnJR2Aw0xM8gei" -> {
                return this.evacuationCapacity;
            }
            case "bsd5Noel9k5tlgpoOWP" -> {
                return this.position;
            }
            case "ZYx31ZY8GstGNur40z2" -> {
                return this.waterloggingState;
            }
            case "KsKWUZghMV0qp4PvIP9" -> {
                return this.floodWallState;
            }
            case "4y48YnVOoSCjQWsqZcs" -> {
                return this.intactFlowWidth;
            }
            case "TmBu06RLACpGgpetjrU" -> {
                return this.failedFlowWidth;
            }
            case "lcRigegnMIZiDTVJben" -> {
                return this.inletFloorElevation;
            }
            case "CbhmI1QG31xuE1fsd8M" -> {
                return this.inletFlowCoeff;
            }
            case "SnxMu3A2siAAnOyycRr" -> {
                return this.overlimitDuration;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "ZSCBmOks5hfDvQgGwnd" -> {
                this.setStationWaterVolume(GromaValues.toDouble(value), ctx);
            }
            case "9cgmJtgAfA5THCTQX1Q" -> {
                this.setFailureThreshold(GromaValues.toDouble(value), ctx);
            }
            case "2x8UFMHABjbk7XdvGSq" -> {
                this.setFailureDuration(GromaValues.toDouble(value), ctx);
            }
            case "HsJlhF4Qq0meQjVXQl5" -> {
                this.setFloodWallTopElevation(GromaValues.toDouble(value), ctx);
            }
            case "icz3z4NdDl4XzMF1lEA" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "ySKDJJ9ZM0cRG8ny4A4" -> {
                this.setRatedDrainCapacity(GromaValues.toDouble(value), ctx);
            }
            case "JbCoZ3N3pGfxVn1fAHA" -> {
                this.setStationFloorElevation(GromaValues.toDouble(value), ctx);
            }
            case "TGW59nqUVYBoQzfbnSa" -> {
                this.setEquivalentStorageArea(GromaValues.toDouble(value), ctx);
            }
            case "4BkvxU1jidI2jo6QEpq" -> {
                this.setStationWaterDepth(GromaValues.toDouble(value), ctx);
            }
            case "xLpWusnJR2Aw0xM8gei" -> {
                this.setEvacuationCapacity(GromaValues.toDouble(value), ctx);
            }
            case "bsd5Noel9k5tlgpoOWP" -> {
                this.setPosition((GeoPointEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "ZYx31ZY8GstGNur40z2" -> {
                this.setWaterloggingState(value == null ? null : GromaValues.toEnum(WaterloggingStateEnum.class, value), ctx);
            }
            case "KsKWUZghMV0qp4PvIP9" -> {
                this.setFloodWallState(value == null ? null : GromaValues.toEnum(FloodWallStateEnum.class, value), ctx);
            }
            case "4y48YnVOoSCjQWsqZcs" -> {
                this.setIntactFlowWidth(GromaValues.toDouble(value), ctx);
            }
            case "TmBu06RLACpGgpetjrU" -> {
                this.setFailedFlowWidth(GromaValues.toDouble(value), ctx);
            }
            case "lcRigegnMIZiDTVJben" -> {
                this.setInletFloorElevation(GromaValues.toDouble(value), ctx);
            }
            case "CbhmI1QG31xuE1fsd8M" -> {
                this.setInletFlowCoeff(GromaValues.toDouble(value), ctx);
            }
            case "SnxMu3A2siAAnOyycRr" -> {
                this.setOverlimitDuration(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public StationEntity copy(GromaWorldContext ctx) {
        StationEntity copy = new StationEntity(ctx.nextInstanceId("Station"));
        copy.openCreation();
        copy.setStationWaterVolume(this.getStationWaterVolume());
        copy.setFailureThreshold(this.getFailureThreshold());
        copy.setFailureDuration(this.getFailureDuration());
        copy.setFloodWallTopElevation(this.getFloodWallTopElevation());
        copy.setName(this.getName());
        copy.setRatedDrainCapacity(this.getRatedDrainCapacity());
        copy.setStationFloorElevation(this.getStationFloorElevation());
        copy.setEquivalentStorageArea(this.getEquivalentStorageArea());
        copy.setStationWaterDepth(this.getStationWaterDepth());
        copy.setEvacuationCapacity(this.getEvacuationCapacity());
        copy.setPosition(this.getPosition(), ctx);
        copy.setWaterloggingState(this.getWaterloggingState());
        copy.setFloodWallState(this.getFloodWallState());
        copy.setIntactFlowWidth(this.getIntactFlowWidth());
        copy.setFailedFlowWidth(this.getFailedFlowWidth());
        copy.setInletFloorElevation(this.getInletFloorElevation());
        copy.setInletFlowCoeff(this.getInletFlowCoeff());
        copy.setOverlimitDuration(this.getOverlimitDuration());
        ctx.register(copy);
        return copy;
    }
}
