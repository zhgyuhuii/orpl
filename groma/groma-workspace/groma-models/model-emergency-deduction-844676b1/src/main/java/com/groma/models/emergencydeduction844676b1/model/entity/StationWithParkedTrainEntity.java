package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.StationWithParkedTrainReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 车站与停靠列车实体（方圆对象类型原名：车站与停靠列车）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>列车（train，REF）</li>
 * <li>车站（station，REF）</li>
 * <li>实际离站时刻（actualDepartureTime，BASE）</li>
 * <li>实际到站时刻（actualArrivalTime，BASE）</li>
 * <li>规定停站时长（prescribedStopDuration，BASE，单位 s）</li>
 * <li>离站是否允许（departureAllowed，BASE）</li>
 * </ul>
 */
@Schema(
        title = "车站与停靠列车",
        description = "方圆对象类型：车站与停靠列车"
)
@GromaMeta(
        id = "Pcp7gmk5wwGxDtnmiCR",
        zhName = "车站与停靠列车",
        kind = "TYPE"
)
public class StationWithParkedTrainEntity extends BaseSimEntity implements StationWithParkedTrainReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("Y73IW1ewl3mlUJlQvWU", "列车", "train", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity", ""), new GromaMemberMeta("lrd9m2v82kyD3mrdk8p", "车站", "station", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("wSFHvbuLXhbgxEbboPe", "实际离站时刻", "actualDepartureTime", "BASE", "long", ""), new GromaMemberMeta("HAIVzK7lukwXbFKbuIu", "实际到站时刻", "actualArrivalTime", "BASE", "long", ""), new GromaMemberMeta("Bslj13aZSBZZ50cq6uY", "规定停站时长", "prescribedStopDuration", "BASE", "long", "s"), new GromaMemberMeta("fOJkK885q6NfNdBgF34", "离站是否允许", "departureAllowed", "BASE", "boolean", ""));

    /**
     * 列车（方圆对象成员原名：列车）
     */
    @Schema(
            title = "列车"
    )
    @GromaMeta(
            id = "Y73IW1ewl3mlUJlQvWU",
            zhName = "列车",
            kind = "FIELD"
    )
    private TrainEntity train = null;

    /**
     * 车站（方圆对象成员原名：车站）
     */
    @Schema(
            title = "车站"
    )
    @GromaMeta(
            id = "lrd9m2v82kyD3mrdk8p",
            zhName = "车站",
            kind = "FIELD"
    )
    private StationEntity station = null;

    /**
     * 实际离站时刻（方圆属性成员原名：实际离站时刻）
     */
    @Schema(
            title = "实际离站时刻"
    )
    @GromaMeta(
            id = "wSFHvbuLXhbgxEbboPe",
            zhName = "实际离站时刻",
            kind = "FIELD"
    )
    private long actualDepartureTime = 0L;

    /**
     * 实际到站时刻（方圆属性成员原名：实际到站时刻）
     */
    @Schema(
            title = "实际到站时刻"
    )
    @GromaMeta(
            id = "HAIVzK7lukwXbFKbuIu",
            zhName = "实际到站时刻",
            kind = "FIELD"
    )
    private long actualArrivalTime = 0L;

    /**
     * 规定停站时长（方圆属性成员原名：规定停站时长） 单位：s 默认值：30
     */
    @Schema(
            title = "规定停站时长"
    )
    @GromaMeta(
            id = "Bslj13aZSBZZ50cq6uY",
            zhName = "规定停站时长",
            kind = "FIELD",
            unit = "s"
    )
    private long prescribedStopDuration = 30L;

    /**
     * 离站是否允许（方圆属性成员原名：离站是否允许） 默认值：false
     */
    @Schema(
            title = "离站是否允许"
    )
    @GromaMeta(
            id = "fOJkK885q6NfNdBgF34",
            zhName = "离站是否允许",
            kind = "FIELD"
    )
    private boolean departureAllowed = false;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public StationWithParkedTrainEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "StationWithParkedTrain";
    }

    @Override
    public String getTypeZhName() {
        return "车站与停靠列车";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public TrainEntity getTrain() {
        return this.train;
    }

    /**
     * 写入列车并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrain(TrainEntity value, GromaWorldContext ctx) {
        if (this.train == value) {
            return;
        }
        TrainEntity before = this.train;
        this.train = value;
        markDirty(0);
        ctx.onMemberChanged(this, "Y73IW1ewl3mlUJlQvWU", before, value);
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
        ctx.onMemberChanged(this, "lrd9m2v82kyD3mrdk8p", before, value);
    }

    @Override
    public long getActualDepartureTime() {
        return this.actualDepartureTime;
    }

    /**
     * 写入实际离站时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setActualDepartureTime(long value, GromaWorldContext ctx) {
        if (this.actualDepartureTime == value) {
            return;
        }
        long before = this.actualDepartureTime;
        this.actualDepartureTime = value;
        markDirty(2);
        ctx.onMemberChanged(this, "wSFHvbuLXhbgxEbboPe", before, value);
    }

    /**
     * 直接写入实际离站时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setActualDepartureTime(long value) {
        this.actualDepartureTime = value;
    }

    @Override
    public long getActualArrivalTime() {
        return this.actualArrivalTime;
    }

    /**
     * 写入实际到站时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setActualArrivalTime(long value, GromaWorldContext ctx) {
        if (this.actualArrivalTime == value) {
            return;
        }
        long before = this.actualArrivalTime;
        this.actualArrivalTime = value;
        markDirty(3);
        ctx.onMemberChanged(this, "HAIVzK7lukwXbFKbuIu", before, value);
    }

    /**
     * 直接写入实际到站时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setActualArrivalTime(long value) {
        this.actualArrivalTime = value;
    }

    @Override
    public long getPrescribedStopDuration() {
        return this.prescribedStopDuration;
    }

    /**
     * 写入规定停站时长并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPrescribedStopDuration(long value, GromaWorldContext ctx) {
        if (this.prescribedStopDuration == value) {
            return;
        }
        long before = this.prescribedStopDuration;
        this.prescribedStopDuration = value;
        markDirty(4);
        ctx.onMemberChanged(this, "Bslj13aZSBZZ50cq6uY", before, value);
    }

    /**
     * 直接写入规定停站时长（不发事件；仅用于初始化与对象创建期）
     */
    public void setPrescribedStopDuration(long value) {
        this.prescribedStopDuration = value;
    }

    @Override
    public boolean isDepartureAllowed() {
        return this.departureAllowed;
    }

    /**
     * 写入离站是否允许并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDepartureAllowed(boolean value, GromaWorldContext ctx) {
        if (this.departureAllowed == value) {
            return;
        }
        boolean before = this.departureAllowed;
        this.departureAllowed = value;
        markDirty(5);
        ctx.onMemberChanged(this, "fOJkK885q6NfNdBgF34", before, value);
    }

    /**
     * 直接写入离站是否允许（不发事件；仅用于初始化与对象创建期）
     */
    public void setDepartureAllowed(boolean value) {
        this.departureAllowed = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "Y73IW1ewl3mlUJlQvWU" -> {
                return this.train;
            }
            case "lrd9m2v82kyD3mrdk8p" -> {
                return this.station;
            }
            case "wSFHvbuLXhbgxEbboPe" -> {
                return this.actualDepartureTime;
            }
            case "HAIVzK7lukwXbFKbuIu" -> {
                return this.actualArrivalTime;
            }
            case "Bslj13aZSBZZ50cq6uY" -> {
                return this.prescribedStopDuration;
            }
            case "fOJkK885q6NfNdBgF34" -> {
                return this.departureAllowed;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "Y73IW1ewl3mlUJlQvWU" -> {
                this.setTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "lrd9m2v82kyD3mrdk8p" -> {
                this.setStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "wSFHvbuLXhbgxEbboPe" -> {
                this.setActualDepartureTime(GromaValues.toLong(value), ctx);
            }
            case "HAIVzK7lukwXbFKbuIu" -> {
                this.setActualArrivalTime(GromaValues.toLong(value), ctx);
            }
            case "Bslj13aZSBZZ50cq6uY" -> {
                this.setPrescribedStopDuration(GromaValues.toLong(value), ctx);
            }
            case "fOJkK885q6NfNdBgF34" -> {
                this.setDepartureAllowed(GromaValues.toBoolean(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public StationWithParkedTrainEntity copy(GromaWorldContext ctx) {
        StationWithParkedTrainEntity copy = new StationWithParkedTrainEntity(ctx.nextInstanceId("StationWithParkedTrain"));
        copy.openCreation();
        copy.setTrain(this.getTrain(), ctx);
        copy.setStation(this.getStation(), ctx);
        copy.setActualDepartureTime(this.getActualDepartureTime());
        copy.setActualArrivalTime(this.getActualArrivalTime());
        copy.setPrescribedStopDuration(this.getPrescribedStopDuration());
        copy.setDepartureAllowed(this.isDepartureAllowed());
        ctx.register(copy);
        return copy;
    }
}
