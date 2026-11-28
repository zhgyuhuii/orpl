package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.AreaWithStationReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 区域与区域内车站实体（方圆对象类型原名：区域与区域内车站）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>区域（area，REF）</li>
 * <li>车站（station，REF）</li>
 * <li>当前过水流量（currentFlowRate，BASE，单位 m³/min）</li>
 * <li>本步过水量（stepFlowVolume，BASE，单位 0）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当积水状态 进入状态「积水」 发生【状态进入(sa)】 时，启动过程「区域与站内水流交换」</li>
 * </ul>
 */
@Schema(
        title = "区域与区域内车站",
        description = "方圆对象类型：区域与区域内车站"
)
@GromaMeta(
        id = "GY5EmLZdeZGsXBJcHef",
        zhName = "区域与区域内车站",
        kind = "TYPE"
)
public class AreaWithStationEntity extends BaseSimEntity implements AreaWithStationReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("gEzBWiOUbrpvxwZ8Pv0", "区域", "area", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity", ""), new GromaMemberMeta("f482bs8kk6zJ2XuV6BK", "车站", "station", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("XLTMneA2bEyAdE4Auta", "当前过水流量", "currentFlowRate", "BASE", "double", "m³/min"), new GromaMemberMeta("vjXgOFLkILFHLg8eh5U", "本步过水量", "stepFlowVolume", "BASE", "double", "0"));

    /**
     * 区域（方圆对象成员原名：区域）
     */
    @Schema(
            title = "区域"
    )
    @GromaMeta(
            id = "gEzBWiOUbrpvxwZ8Pv0",
            zhName = "区域",
            kind = "FIELD"
    )
    private AreaEntity area = null;

    /**
     * 车站（方圆对象成员原名：车站）
     */
    @Schema(
            title = "车站"
    )
    @GromaMeta(
            id = "f482bs8kk6zJ2XuV6BK",
            zhName = "车站",
            kind = "FIELD"
    )
    private StationEntity station = null;

    /**
     * 当前过水流量（方圆属性成员原名：当前过水流量） 单位：m³/min
     */
    @Schema(
            title = "当前过水流量"
    )
    @GromaMeta(
            id = "XLTMneA2bEyAdE4Auta",
            zhName = "当前过水流量",
            kind = "FIELD",
            unit = "m³/min"
    )
    private double currentFlowRate = 0.0D;

    /**
     * 本步过水量（方圆属性成员原名：本步过水量） 单位：0
     */
    @Schema(
            title = "本步过水量"
    )
    @GromaMeta(
            id = "vjXgOFLkILFHLg8eh5U",
            zhName = "本步过水量",
            kind = "FIELD",
            unit = "0"
    )
    private double stepFlowVolume = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public AreaWithStationEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "AreaWithStation";
    }

    @Override
    public String getTypeZhName() {
        return "区域与区域内车站";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public AreaEntity getArea() {
        return this.area;
    }

    /**
     * 写入区域并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setArea(AreaEntity value, GromaWorldContext ctx) {
        if (this.area == value) {
            return;
        }
        AreaEntity before = this.area;
        this.area = value;
        markDirty(0);
        ctx.onMemberChanged(this, "gEzBWiOUbrpvxwZ8Pv0", before, value);
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
        markDirty(1);
        ctx.onMemberChanged(this, "f482bs8kk6zJ2XuV6BK", before, value);
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
        markDirty(2);
        ctx.onMemberChanged(this, "XLTMneA2bEyAdE4Auta", before, value);
    }

    /**
     * 直接写入当前过水流量（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentFlowRate(double value) {
        this.currentFlowRate = value;
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
        markDirty(3);
        ctx.onMemberChanged(this, "vjXgOFLkILFHLg8eh5U", before, value);
    }

    /**
     * 直接写入本步过水量（不发事件；仅用于初始化与对象创建期）
     */
    public void setStepFlowVolume(double value) {
        this.stepFlowVolume = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "gEzBWiOUbrpvxwZ8Pv0" -> {
                return this.area;
            }
            case "f482bs8kk6zJ2XuV6BK" -> {
                return this.station;
            }
            case "XLTMneA2bEyAdE4Auta" -> {
                return this.currentFlowRate;
            }
            case "vjXgOFLkILFHLg8eh5U" -> {
                return this.stepFlowVolume;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "gEzBWiOUbrpvxwZ8Pv0" -> {
                this.setArea((AreaEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "f482bs8kk6zJ2XuV6BK" -> {
                this.setStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "XLTMneA2bEyAdE4Auta" -> {
                this.setCurrentFlowRate(GromaValues.toDouble(value), ctx);
            }
            case "vjXgOFLkILFHLg8eh5U" -> {
                this.setStepFlowVolume(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public AreaWithStationEntity copy(GromaWorldContext ctx) {
        AreaWithStationEntity copy = new AreaWithStationEntity(ctx.nextInstanceId("AreaWithStation"));
        copy.openCreation();
        copy.setArea(this.getArea(), ctx);
        copy.setStation(this.getStation(), ctx);
        copy.setCurrentFlowRate(this.getCurrentFlowRate());
        copy.setStepFlowVolume(this.getStepFlowVolume());
        ctx.register(copy);
        return copy;
    }
}
