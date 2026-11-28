package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.SectionWithEndStationReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 区间与端点车站实体（方圆对象类型原名：区间与端点车站）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>车站（station，REF）</li>
 * <li>区间（section，REF）</li>
 * <li>本步过水量（stepFlowVolume，BASE，单位 m³）</li>
 * <li>连接处底高程（junctionFloorElevation，BASE，单位 m）</li>
 * <li>有效过水宽度（effectiveFlowWidth，BASE，单位 m）</li>
 * <li>水流系数（flowCoeff，BASE）</li>
 * <li>当前过水流量（currentFlowRate，BASE，单位 m³/min）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当积水状态 进入状态「积水」 发生【状态进入(sa)】 时，启动过程「车站与区间水流交换」</li>
 * </ul>
 */
@Schema(
        title = "区间与端点车站",
        description = "方圆对象类型：区间与端点车站"
)
@GromaMeta(
        id = "yALuBQFoqC0OJuH31TP",
        zhName = "区间与端点车站",
        kind = "TYPE"
)
public class SectionWithEndStationEntity extends BaseSimEntity implements SectionWithEndStationReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("cs7DzmGZqyZnfP20Gsy", "车站", "station", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("6NOAfgYaMv32vdr5VlF", "区间", "section", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("KMlHnXHXmSsrivSkbmf", "本步过水量", "stepFlowVolume", "BASE", "double", "m³"), new GromaMemberMeta("gfXSjsjhf0iZULvQE6F", "连接处底高程", "junctionFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("aqBhFinNqNUJOw2rYuA", "有效过水宽度", "effectiveFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("oSBmVrsD3jJmoAhvGHi", "水流系数", "flowCoeff", "BASE", "double", ""), new GromaMemberMeta("QLFX5LYS3Oabdm7w2ZM", "当前过水流量", "currentFlowRate", "BASE", "double", "m³/min"));

    /**
     * 车站（方圆对象成员原名：车站）
     */
    @Schema(
            title = "车站"
    )
    @GromaMeta(
            id = "cs7DzmGZqyZnfP20Gsy",
            zhName = "车站",
            kind = "FIELD"
    )
    private StationEntity station = null;

    /**
     * 区间（方圆对象成员原名：区间）
     */
    @Schema(
            title = "区间"
    )
    @GromaMeta(
            id = "6NOAfgYaMv32vdr5VlF",
            zhName = "区间",
            kind = "FIELD"
    )
    private MetroSectionEntity section = null;

    /**
     * 本步过水量（方圆属性成员原名：本步过水量） 单位：m³
     */
    @Schema(
            title = "本步过水量"
    )
    @GromaMeta(
            id = "KMlHnXHXmSsrivSkbmf",
            zhName = "本步过水量",
            kind = "FIELD",
            unit = "m³"
    )
    private double stepFlowVolume = 0.0D;

    /**
     * 连接处底高程（方圆属性成员原名：连接处底高程） 单位：m
     */
    @Schema(
            title = "连接处底高程"
    )
    @GromaMeta(
            id = "gfXSjsjhf0iZULvQE6F",
            zhName = "连接处底高程",
            kind = "FIELD",
            unit = "m"
    )
    private double junctionFloorElevation = 0.0D;

    /**
     * 有效过水宽度（方圆属性成员原名：有效过水宽度） 单位：m
     */
    @Schema(
            title = "有效过水宽度"
    )
    @GromaMeta(
            id = "aqBhFinNqNUJOw2rYuA",
            zhName = "有效过水宽度",
            kind = "FIELD",
            unit = "m"
    )
    private double effectiveFlowWidth = 0.0D;

    /**
     * 水流系数（方圆属性成员原名：水流系数）
     */
    @Schema(
            title = "水流系数"
    )
    @GromaMeta(
            id = "oSBmVrsD3jJmoAhvGHi",
            zhName = "水流系数",
            kind = "FIELD"
    )
    private double flowCoeff = 0.0D;

    /**
     * 当前过水流量（方圆属性成员原名：当前过水流量） 单位：m³/min
     */
    @Schema(
            title = "当前过水流量"
    )
    @GromaMeta(
            id = "QLFX5LYS3Oabdm7w2ZM",
            zhName = "当前过水流量",
            kind = "FIELD",
            unit = "m³/min"
    )
    private double currentFlowRate = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public SectionWithEndStationEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "SectionWithEndStation";
    }

    @Override
    public String getTypeZhName() {
        return "区间与端点车站";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public StationEntity getStation() {
        return this.station;
    }

    /**
     * 写入车站并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStation(StationEntity value, GromaWorldContext ctx) {
        if (this.station == value) {
            return;
        }
        StationEntity before = this.station;
        this.station = value;
        markDirty(0);
        ctx.onMemberChanged(this, "cs7DzmGZqyZnfP20Gsy", before, value);
    }

    @Override
    public MetroSectionEntity getSection() {
        return this.section;
    }

    /**
     * 写入区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSection(MetroSectionEntity value, GromaWorldContext ctx) {
        if (this.section == value) {
            return;
        }
        MetroSectionEntity before = this.section;
        this.section = value;
        markDirty(1);
        ctx.onMemberChanged(this, "6NOAfgYaMv32vdr5VlF", before, value);
    }

    @Override
    public double getStepFlowVolume() {
        return this.stepFlowVolume;
    }

    /**
     * 写入本步过水量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setStepFlowVolume(double value, GromaWorldContext ctx) {
        if (this.stepFlowVolume == value) {
            return;
        }
        double before = this.stepFlowVolume;
        this.stepFlowVolume = value;
        markDirty(2);
        ctx.onMemberChanged(this, "KMlHnXHXmSsrivSkbmf", before, value);
    }

    /**
     * 直接写入本步过水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setStepFlowVolume(double value) {
        this.stepFlowVolume = value;
    }

    @Override
    public double getJunctionFloorElevation() {
        return this.junctionFloorElevation;
    }

    /**
     * 写入连接处底高程并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setJunctionFloorElevation(double value, GromaWorldContext ctx) {
        if (this.junctionFloorElevation == value) {
            return;
        }
        double before = this.junctionFloorElevation;
        this.junctionFloorElevation = value;
        markDirty(3);
        ctx.onMemberChanged(this, "gfXSjsjhf0iZULvQE6F", before, value);
    }

    /**
     * 直接写入连接处底高程（不发事件；仅用于初始化与对象创建期）
     */
    public void setJunctionFloorElevation(double value) {
        this.junctionFloorElevation = value;
    }

    @Override
    public double getEffectiveFlowWidth() {
        return this.effectiveFlowWidth;
    }

    /**
     * 写入有效过水宽度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEffectiveFlowWidth(double value, GromaWorldContext ctx) {
        if (this.effectiveFlowWidth == value) {
            return;
        }
        double before = this.effectiveFlowWidth;
        this.effectiveFlowWidth = value;
        markDirty(4);
        ctx.onMemberChanged(this, "aqBhFinNqNUJOw2rYuA", before, value);
    }

    /**
     * 直接写入有效过水宽度（不发事件；仅用于初始化与对象创建期）
     */
    public void setEffectiveFlowWidth(double value) {
        this.effectiveFlowWidth = value;
    }

    @Override
    public double getFlowCoeff() {
        return this.flowCoeff;
    }

    /**
     * 写入水流系数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFlowCoeff(double value, GromaWorldContext ctx) {
        if (this.flowCoeff == value) {
            return;
        }
        double before = this.flowCoeff;
        this.flowCoeff = value;
        markDirty(5);
        ctx.onMemberChanged(this, "oSBmVrsD3jJmoAhvGHi", before, value);
    }

    /**
     * 直接写入水流系数（不发事件；仅用于初始化与对象创建期）
     */
    public void setFlowCoeff(double value) {
        this.flowCoeff = value;
    }

    @Override
    public double getCurrentFlowRate() {
        return this.currentFlowRate;
    }

    /**
     * 写入当前过水流量并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentFlowRate(double value, GromaWorldContext ctx) {
        if (this.currentFlowRate == value) {
            return;
        }
        double before = this.currentFlowRate;
        this.currentFlowRate = value;
        markDirty(6);
        ctx.onMemberChanged(this, "QLFX5LYS3Oabdm7w2ZM", before, value);
    }

    /**
     * 直接写入当前过水流量（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentFlowRate(double value) {
        this.currentFlowRate = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "cs7DzmGZqyZnfP20Gsy" -> {
                return this.station;
            }
            case "6NOAfgYaMv32vdr5VlF" -> {
                return this.section;
            }
            case "KMlHnXHXmSsrivSkbmf" -> {
                return this.stepFlowVolume;
            }
            case "gfXSjsjhf0iZULvQE6F" -> {
                return this.junctionFloorElevation;
            }
            case "aqBhFinNqNUJOw2rYuA" -> {
                return this.effectiveFlowWidth;
            }
            case "oSBmVrsD3jJmoAhvGHi" -> {
                return this.flowCoeff;
            }
            case "QLFX5LYS3Oabdm7w2ZM" -> {
                return this.currentFlowRate;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "cs7DzmGZqyZnfP20Gsy" -> {
                this.setStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "6NOAfgYaMv32vdr5VlF" -> {
                this.setSection((MetroSectionEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "KMlHnXHXmSsrivSkbmf" -> {
                this.setStepFlowVolume(GromaValues.toDouble(value), ctx);
            }
            case "gfXSjsjhf0iZULvQE6F" -> {
                this.setJunctionFloorElevation(GromaValues.toDouble(value), ctx);
            }
            case "aqBhFinNqNUJOw2rYuA" -> {
                this.setEffectiveFlowWidth(GromaValues.toDouble(value), ctx);
            }
            case "oSBmVrsD3jJmoAhvGHi" -> {
                this.setFlowCoeff(GromaValues.toDouble(value), ctx);
            }
            case "QLFX5LYS3Oabdm7w2ZM" -> {
                this.setCurrentFlowRate(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public SectionWithEndStationEntity copy(GromaWorldContext ctx) {
        SectionWithEndStationEntity copy = new SectionWithEndStationEntity(ctx.nextInstanceId("SectionWithEndStation"));
        copy.openCreation();
        copy.setStation(this.getStation(), ctx);
        copy.setSection(this.getSection(), ctx);
        copy.setStepFlowVolume(this.getStepFlowVolume());
        copy.setJunctionFloorElevation(this.getJunctionFloorElevation());
        copy.setEffectiveFlowWidth(this.getEffectiveFlowWidth());
        copy.setFlowCoeff(this.getFlowCoeff());
        copy.setCurrentFlowRate(this.getCurrentFlowRate());
        ctx.register(copy);
        return copy;
    }
}
