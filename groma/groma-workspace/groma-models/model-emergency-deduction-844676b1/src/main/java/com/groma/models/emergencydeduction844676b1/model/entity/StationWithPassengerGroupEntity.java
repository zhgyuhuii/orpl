package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.StationWithPassengerGroupReadOnly;
import com.groma.models.emergencydeduction844676b1.model.enums.EvacuationStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 车站与站内乘客群实体（方圆对象类型原名：车站与站内乘客群）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>乘客群（passengerGroup，REF）</li>
 * <li>车站（station，REF）</li>
 * <li>当前站内人数（currentStationPeopleCount，BASE）</li>
 * <li>已疏散人数（evacuatedPeopleCount，BASE）</li>
 * <li>疏散是否启动（evacuationStarted，BASE）</li>
 * <li>疏散状态（evacuationState，ENUM）</li>
 * </ul>
 */
@Schema(
        title = "车站与站内乘客群",
        description = "方圆对象类型：车站与站内乘客群"
)
@GromaMeta(
        id = "NKsxPhV6qn51BWMo1L7",
        zhName = "车站与站内乘客群",
        kind = "TYPE"
)
public class StationWithPassengerGroupEntity extends BaseSimEntity implements StationWithPassengerGroupReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("BcAXKtQjcKmqR1ZbpE6", "乘客群", "passengerGroup", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.PassengerGroupEntity", ""), new GromaMemberMeta("bop7naa2mk9nq5jDfQV", "车站", "station", "REF", "com.groma.models.emergencydeduction844676b1.model.entity.StationEntity", ""), new GromaMemberMeta("KgtcWRWkJVglELd7Wcv", "当前站内人数", "currentStationPeopleCount", "BASE", "long", ""), new GromaMemberMeta("GIri3KzuDHHRylbTLId", "已疏散人数", "evacuatedPeopleCount", "BASE", "long", ""), new GromaMemberMeta("fm6lWpiMy0su6wtfeah", "疏散是否启动", "evacuationStarted", "BASE", "boolean", ""), new GromaMemberMeta("a4rKo4qnNcaqAtngpGz", "疏散状态", "evacuationState", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.EvacuationStateEnum", ""));

    /**
     * 乘客群（方圆对象成员原名：乘客群）
     */
    @Schema(
            title = "乘客群"
    )
    @GromaMeta(
            id = "BcAXKtQjcKmqR1ZbpE6",
            zhName = "乘客群",
            kind = "FIELD"
    )
    private PassengerGroupEntity passengerGroup = null;

    /**
     * 车站（方圆对象成员原名：车站）
     */
    @Schema(
            title = "车站"
    )
    @GromaMeta(
            id = "bop7naa2mk9nq5jDfQV",
            zhName = "车站",
            kind = "FIELD"
    )
    private StationEntity station = null;

    /**
     * 当前站内人数（方圆属性成员原名：当前站内人数）
     */
    @Schema(
            title = "当前站内人数"
    )
    @GromaMeta(
            id = "KgtcWRWkJVglELd7Wcv",
            zhName = "当前站内人数",
            kind = "FIELD"
    )
    private long currentStationPeopleCount = 0L;

    /**
     * 已疏散人数（方圆属性成员原名：已疏散人数）
     */
    @Schema(
            title = "已疏散人数"
    )
    @GromaMeta(
            id = "GIri3KzuDHHRylbTLId",
            zhName = "已疏散人数",
            kind = "FIELD"
    )
    private long evacuatedPeopleCount = 0L;

    /**
     * 疏散是否启动（方圆属性成员原名：疏散是否启动）
     */
    @Schema(
            title = "疏散是否启动"
    )
    @GromaMeta(
            id = "fm6lWpiMy0su6wtfeah",
            zhName = "疏散是否启动",
            kind = "FIELD"
    )
    private boolean evacuationStarted = false;

    /**
     * 疏散状态（方圆状态成员原名：疏散状态）
     */
    @Schema(
            title = "疏散状态"
    )
    @GromaMeta(
            id = "a4rKo4qnNcaqAtngpGz",
            zhName = "疏散状态",
            kind = "FIELD"
    )
    private EvacuationStateEnum evacuationState = EvacuationStateEnum.PENDING_EVACUATION;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public StationWithPassengerGroupEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "StationWithPassengerGroup";
    }

    @Override
    public String getTypeZhName() {
        return "车站与站内乘客群";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public PassengerGroupEntity getPassengerGroup() {
        return this.passengerGroup;
    }

    /**
     * 写入乘客群并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPassengerGroup(PassengerGroupEntity value, GromaWorldContext ctx) {
        if (this.passengerGroup == value) {
            return;
        }
        PassengerGroupEntity before = this.passengerGroup;
        this.passengerGroup = value;
        markDirty(0);
        ctx.onMemberChanged(this, "BcAXKtQjcKmqR1ZbpE6", before, value);
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
        ctx.onMemberChanged(this, "bop7naa2mk9nq5jDfQV", before, value);
    }

    @Override
    public long getCurrentStationPeopleCount() {
        return this.currentStationPeopleCount;
    }

    /**
     * 写入当前站内人数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentStationPeopleCount(long value, GromaWorldContext ctx) {
        if (this.currentStationPeopleCount == value) {
            return;
        }
        long before = this.currentStationPeopleCount;
        this.currentStationPeopleCount = value;
        markDirty(2);
        ctx.onMemberChanged(this, "KgtcWRWkJVglELd7Wcv", before, value);
    }

    /**
     * 直接写入当前站内人数（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentStationPeopleCount(long value) {
        this.currentStationPeopleCount = value;
    }

    @Override
    public long getEvacuatedPeopleCount() {
        return this.evacuatedPeopleCount;
    }

    /**
     * 写入已疏散人数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEvacuatedPeopleCount(long value, GromaWorldContext ctx) {
        if (this.evacuatedPeopleCount == value) {
            return;
        }
        long before = this.evacuatedPeopleCount;
        this.evacuatedPeopleCount = value;
        markDirty(3);
        ctx.onMemberChanged(this, "GIri3KzuDHHRylbTLId", before, value);
    }

    /**
     * 直接写入已疏散人数（不发事件；仅用于初始化与对象创建期）
     */
    public void setEvacuatedPeopleCount(long value) {
        this.evacuatedPeopleCount = value;
    }

    @Override
    public boolean isEvacuationStarted() {
        return this.evacuationStarted;
    }

    /**
     * 写入疏散是否启动并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEvacuationStarted(boolean value, GromaWorldContext ctx) {
        if (this.evacuationStarted == value) {
            return;
        }
        boolean before = this.evacuationStarted;
        this.evacuationStarted = value;
        markDirty(4);
        ctx.onMemberChanged(this, "fm6lWpiMy0su6wtfeah", before, value);
    }

    /**
     * 直接写入疏散是否启动（不发事件；仅用于初始化与对象创建期）
     */
    public void setEvacuationStarted(boolean value) {
        this.evacuationStarted = value;
    }

    @Override
    public EvacuationStateEnum getEvacuationState() {
        return this.evacuationState;
    }

    /**
     * 写入疏散状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setEvacuationState(EvacuationStateEnum value, GromaWorldContext ctx) {
        if (this.evacuationState == value) {
            return;
        }
        EvacuationStateEnum before = this.evacuationState;
        this.evacuationState = value;
        markDirty(5);
        ctx.onMemberChanged(this, "a4rKo4qnNcaqAtngpGz", before, value);
    }

    /**
     * 直接写入疏散状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setEvacuationState(EvacuationStateEnum value) {
        this.evacuationState = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "BcAXKtQjcKmqR1ZbpE6" -> {
                return this.passengerGroup;
            }
            case "bop7naa2mk9nq5jDfQV" -> {
                return this.station;
            }
            case "KgtcWRWkJVglELd7Wcv" -> {
                return this.currentStationPeopleCount;
            }
            case "GIri3KzuDHHRylbTLId" -> {
                return this.evacuatedPeopleCount;
            }
            case "fm6lWpiMy0su6wtfeah" -> {
                return this.evacuationStarted;
            }
            case "a4rKo4qnNcaqAtngpGz" -> {
                return this.evacuationState;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "BcAXKtQjcKmqR1ZbpE6" -> {
                this.setPassengerGroup((PassengerGroupEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "bop7naa2mk9nq5jDfQV" -> {
                this.setStation((StationEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "KgtcWRWkJVglELd7Wcv" -> {
                this.setCurrentStationPeopleCount(GromaValues.toLong(value), ctx);
            }
            case "GIri3KzuDHHRylbTLId" -> {
                this.setEvacuatedPeopleCount(GromaValues.toLong(value), ctx);
            }
            case "fm6lWpiMy0su6wtfeah" -> {
                this.setEvacuationStarted(GromaValues.toBoolean(value), ctx);
            }
            case "a4rKo4qnNcaqAtngpGz" -> {
                this.setEvacuationState(value == null ? null : GromaValues.toEnum(EvacuationStateEnum.class, value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public StationWithPassengerGroupEntity copy(GromaWorldContext ctx) {
        StationWithPassengerGroupEntity copy = new StationWithPassengerGroupEntity(ctx.nextInstanceId("StationWithPassengerGroup"));
        copy.openCreation();
        copy.setPassengerGroup(this.getPassengerGroup(), ctx);
        copy.setStation(this.getStation(), ctx);
        copy.setCurrentStationPeopleCount(this.getCurrentStationPeopleCount());
        copy.setEvacuatedPeopleCount(this.getEvacuatedPeopleCount());
        copy.setEvacuationStarted(this.isEvacuationStarted());
        copy.setEvacuationState(this.getEvacuationState());
        ctx.register(copy);
        return copy;
    }
}
