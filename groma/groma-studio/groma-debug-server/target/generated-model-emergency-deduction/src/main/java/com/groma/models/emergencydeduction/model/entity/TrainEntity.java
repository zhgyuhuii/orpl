package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.TrainReadOnly;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 列车实体（方圆对象类型原名：列车）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>当前速度（currentSpeed，BASE，单位 m/min）</li>
 * <li>迫停时刻（forcedStopTime，BASE，单位 s）</li>
 * <li>初始位置（initialPosition，REF）</li>
 * <li>名称（name，BASE）</li>
 * <li>列车状态（trainState，ENUM）</li>
 * <li>方向（direction，BASE）</li>
 * <li>当前位置（currentPosition，REF）</li>
 * <li>行驶速度（travelSpeed，BASE，单位 m/min）</li>
 * </ul>
 */
@Schema(
        title = "列车",
        description = "方圆对象类型：列车"
)
@GromaMeta(
        id = "rWqQrIoiDfDYWa5PiWk",
        zhName = "列车",
        kind = "TYPE"
)
public class TrainEntity extends BaseSimEntity implements TrainReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("NAfVPmumtjToqjUB8dS", "当前速度", "currentSpeed", "BASE", "double", "m/min"), new GromaMemberMeta("mKbdhAWlYJQzae7Zs2Y", "迫停时刻", "forcedStopTime", "BASE", "double", "s"), new GromaMemberMeta("6q4BLT4xe5rzYrXrlde", "初始位置", "initialPosition", "REF", "com.groma.models.emergencydeduction.model.entity.GeoPointEntity", ""), new GromaMemberMeta("a0gEhiRbo6Z0gnO5dir", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("mQs84jpKcMkngxFWdBT", "列车状态", "trainState", "ENUM", "com.groma.models.emergencydeduction.model.enums.TrainStateEnum", ""), new GromaMemberMeta("VnDxdvqY8EYBbJN0JlM", "方向", "direction", "BASE", "double", ""), new GromaMemberMeta("f2Z0TqRbENyGCigHGVL", "当前位置", "currentPosition", "REF", "com.groma.models.emergencydeduction.model.entity.GeoPointEntity", ""), new GromaMemberMeta("cgZTF4S44iiPjp29iem", "行驶速度", "travelSpeed", "BASE", "double", "m/min"));

    /**
     * 当前速度（方圆属性成员原名：当前速度） 单位：m/min 默认值：0
     */
    @Schema(
            title = "当前速度"
    )
    @GromaMeta(
            id = "NAfVPmumtjToqjUB8dS",
            zhName = "当前速度",
            kind = "FIELD",
            unit = "m/min"
    )
    private double currentSpeed = 0.0D;

    /**
     * 迫停时刻（方圆属性成员原名：迫停时刻） 单位：s
     */
    @Schema(
            title = "迫停时刻"
    )
    @GromaMeta(
            id = "mKbdhAWlYJQzae7Zs2Y",
            zhName = "迫停时刻",
            kind = "FIELD",
            unit = "s"
    )
    private double forcedStopTime = 0.0D;

    /**
     * 初始位置（方圆对象成员原名：初始位置）
     */
    @Schema(
            title = "初始位置"
    )
    @GromaMeta(
            id = "6q4BLT4xe5rzYrXrlde",
            zhName = "初始位置",
            kind = "FIELD"
    )
    private GeoPointEntity initialPosition = null;

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "a0gEhiRbo6Z0gnO5dir",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 列车状态（方圆状态成员原名：列车状态） 初始状态：待进入
     */
    @Schema(
            title = "列车状态"
    )
    @GromaMeta(
            id = "mQs84jpKcMkngxFWdBT",
            zhName = "列车状态",
            kind = "FIELD",
            initState = "待进入"
    )
    private TrainStateEnum trainState = TrainStateEnum.PENDING_ENTRY;

    /**
     * 方向（方圆属性成员原名：方向）
     */
    @Schema(
            title = "方向"
    )
    @GromaMeta(
            id = "VnDxdvqY8EYBbJN0JlM",
            zhName = "方向",
            kind = "FIELD"
    )
    private double direction = 0.0D;

    /**
     * 当前位置（方圆对象成员原名：当前位置）
     */
    @Schema(
            title = "当前位置"
    )
    @GromaMeta(
            id = "f2Z0TqRbENyGCigHGVL",
            zhName = "当前位置",
            kind = "FIELD"
    )
    private GeoPointEntity currentPosition = null;

    /**
     * 行驶速度（方圆属性成员原名：行驶速度） 单位：m/min
     */
    @Schema(
            title = "行驶速度"
    )
    @GromaMeta(
            id = "cgZTF4S44iiPjp29iem",
            zhName = "行驶速度",
            kind = "FIELD",
            unit = "m/min"
    )
    private double travelSpeed = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public TrainEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Train";
    }

    @Override
    public String getTypeZhName() {
        return "列车";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public double getCurrentSpeed() {
        return this.currentSpeed;
    }

    /**
     * 写入当前速度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentSpeed(double value, GromaWorldContext ctx) {
        if (this.currentSpeed == value) {
            return;
        }
        double before = this.currentSpeed;
        this.currentSpeed = value;
        markDirty(0);
        ctx.onMemberChanged(this, "NAfVPmumtjToqjUB8dS", before, value);
    }

    /**
     * 直接写入当前速度（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentSpeed(double value) {
        this.currentSpeed = value;
    }

    @Override
    public double getForcedStopTime() {
        return this.forcedStopTime;
    }

    /**
     * 写入迫停时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setForcedStopTime(double value, GromaWorldContext ctx) {
        if (this.forcedStopTime == value) {
            return;
        }
        double before = this.forcedStopTime;
        this.forcedStopTime = value;
        markDirty(1);
        ctx.onMemberChanged(this, "mKbdhAWlYJQzae7Zs2Y", before, value);
    }

    /**
     * 直接写入迫停时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setForcedStopTime(double value) {
        this.forcedStopTime = value;
    }

    @Override
    public GeoPointEntity getInitialPosition() {
        return this.initialPosition;
    }

    /**
     * 写入初始位置并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setInitialPosition(GeoPointEntity value, GromaWorldContext ctx) {
        if (this.initialPosition == value) {
            return;
        }
        GeoPointEntity before = this.initialPosition;
        this.initialPosition = value;
        markDirty(2);
        ctx.onMemberChanged(this, "6q4BLT4xe5rzYrXrlde", before, value);
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
        markDirty(3);
        ctx.onMemberChanged(this, "a0gEhiRbo6Z0gnO5dir", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public TrainStateEnum getTrainState() {
        return this.trainState;
    }

    /**
     * 写入列车状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrainState(TrainStateEnum value, GromaWorldContext ctx) {
        if (this.trainState == value) {
            return;
        }
        TrainStateEnum before = this.trainState;
        this.trainState = value;
        markDirty(4);
        ctx.onMemberChanged(this, "mQs84jpKcMkngxFWdBT", before, value);
    }

    /**
     * 直接写入列车状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setTrainState(TrainStateEnum value) {
        this.trainState = value;
    }

    @Override
    public double getDirection() {
        return this.direction;
    }

    /**
     * 写入方向并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDirection(double value, GromaWorldContext ctx) {
        if (this.direction == value) {
            return;
        }
        double before = this.direction;
        this.direction = value;
        markDirty(5);
        ctx.onMemberChanged(this, "VnDxdvqY8EYBbJN0JlM", before, value);
    }

    /**
     * 直接写入方向（不发事件；仅用于初始化与对象创建期）
     */
    public void setDirection(double value) {
        this.direction = value;
    }

    @Override
    public GeoPointEntity getCurrentPosition() {
        return this.currentPosition;
    }

    /**
     * 写入当前位置并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentPosition(GeoPointEntity value, GromaWorldContext ctx) {
        if (this.currentPosition == value) {
            return;
        }
        GeoPointEntity before = this.currentPosition;
        this.currentPosition = value;
        markDirty(6);
        ctx.onMemberChanged(this, "f2Z0TqRbENyGCigHGVL", before, value);
    }

    @Override
    public double getTravelSpeed() {
        return this.travelSpeed;
    }

    /**
     * 写入行驶速度并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTravelSpeed(double value, GromaWorldContext ctx) {
        if (this.travelSpeed == value) {
            return;
        }
        double before = this.travelSpeed;
        this.travelSpeed = value;
        markDirty(7);
        ctx.onMemberChanged(this, "cgZTF4S44iiPjp29iem", before, value);
    }

    /**
     * 直接写入行驶速度（不发事件；仅用于初始化与对象创建期）
     */
    public void setTravelSpeed(double value) {
        this.travelSpeed = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "NAfVPmumtjToqjUB8dS" -> {
                return this.currentSpeed;
            }
            case "mKbdhAWlYJQzae7Zs2Y" -> {
                return this.forcedStopTime;
            }
            case "6q4BLT4xe5rzYrXrlde" -> {
                return this.initialPosition;
            }
            case "a0gEhiRbo6Z0gnO5dir" -> {
                return this.name;
            }
            case "mQs84jpKcMkngxFWdBT" -> {
                return this.trainState;
            }
            case "VnDxdvqY8EYBbJN0JlM" -> {
                return this.direction;
            }
            case "f2Z0TqRbENyGCigHGVL" -> {
                return this.currentPosition;
            }
            case "cgZTF4S44iiPjp29iem" -> {
                return this.travelSpeed;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "NAfVPmumtjToqjUB8dS" -> {
                this.setCurrentSpeed(GromaValues.toDouble(value), ctx);
            }
            case "mKbdhAWlYJQzae7Zs2Y" -> {
                this.setForcedStopTime(GromaValues.toDouble(value), ctx);
            }
            case "6q4BLT4xe5rzYrXrlde" -> {
                this.setInitialPosition((GeoPointEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "a0gEhiRbo6Z0gnO5dir" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "mQs84jpKcMkngxFWdBT" -> {
                this.setTrainState(value == null ? null : GromaValues.toEnum(TrainStateEnum.class, value), ctx);
            }
            case "VnDxdvqY8EYBbJN0JlM" -> {
                this.setDirection(GromaValues.toDouble(value), ctx);
            }
            case "f2Z0TqRbENyGCigHGVL" -> {
                this.setCurrentPosition((GeoPointEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "cgZTF4S44iiPjp29iem" -> {
                this.setTravelSpeed(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public TrainEntity copy(GromaWorldContext ctx) {
        TrainEntity copy = new TrainEntity(ctx.nextInstanceId("Train"));
        copy.openCreation();
        copy.setCurrentSpeed(this.getCurrentSpeed());
        copy.setForcedStopTime(this.getForcedStopTime());
        copy.setInitialPosition(this.getInitialPosition(), ctx);
        copy.setName(this.getName());
        copy.setTrainState(this.getTrainState());
        copy.setDirection(this.getDirection());
        copy.setCurrentPosition(this.getCurrentPosition(), ctx);
        copy.setTravelSpeed(this.getTravelSpeed());
        ctx.register(copy);
        return copy;
    }
}
