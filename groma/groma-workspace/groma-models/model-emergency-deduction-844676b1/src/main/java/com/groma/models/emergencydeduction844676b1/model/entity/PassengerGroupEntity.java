package com.groma.models.emergencydeduction844676b1.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.api.PassengerGroupReadOnly;
import com.groma.models.emergencydeduction844676b1.model.enums.PassengerStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 乘客群实体（方圆对象类型原名：乘客群）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>名称（name，BASE）</li>
 * <li>总人数（totalPeopleCount，BASE）</li>
 * <li>乘客状态（passengerState，ENUM）</li>
 * <li>首次受困时刻（firstTrappedTime，BASE）</li>
 * <li>全部安全时刻（allSafeTime，BASE）</li>
 * <li>已安全人数（safePeopleCount，BASE）</li>
 * <li>是否经历危险水深（experiencedDangerDepth，BASE）</li>
 * </ul>
 */
@Schema(
        title = "乘客群",
        description = "方圆对象类型：乘客群"
)
@GromaMeta(
        id = "3Dr4LeLCGkprqwEd722",
        zhName = "乘客群",
        kind = "TYPE"
)
public class PassengerGroupEntity extends BaseSimEntity implements PassengerGroupReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("MlorlwiBiZh8KU0r87s", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("5x0OfMIlLsvkJCzGNcQ", "总人数", "totalPeopleCount", "BASE", "long", ""), new GromaMemberMeta("AnOLDYjpTl52BIvOCMh", "乘客状态", "passengerState", "ENUM", "com.groma.models.emergencydeduction844676b1.model.enums.PassengerStateEnum", ""), new GromaMemberMeta("eTFRIJF4ZA5HaxhN1gf", "首次受困时刻", "firstTrappedTime", "BASE", "double", ""), new GromaMemberMeta("QM6Hrha06zlLEKXTv5H", "全部安全时刻", "allSafeTime", "BASE", "double", ""), new GromaMemberMeta("15RcT4aahOAxSrDBdQi", "已安全人数", "safePeopleCount", "BASE", "long", ""), new GromaMemberMeta("IY82VYr6qLtgvSCldj7", "是否经历危险水深", "experiencedDangerDepth", "BASE", "boolean", ""));

    /**
     * 名称（方圆属性成员原名：名称）
     */
    @Schema(
            title = "名称"
    )
    @GromaMeta(
            id = "MlorlwiBiZh8KU0r87s",
            zhName = "名称",
            kind = "FIELD"
    )
    private String name = "";

    /**
     * 总人数（方圆属性成员原名：总人数）
     */
    @Schema(
            title = "总人数"
    )
    @GromaMeta(
            id = "5x0OfMIlLsvkJCzGNcQ",
            zhName = "总人数",
            kind = "FIELD"
    )
    private long totalPeopleCount = 0L;

    /**
     * 乘客状态（方圆状态成员原名：乘客状态）
     */
    @Schema(
            title = "乘客状态"
    )
    @GromaMeta(
            id = "AnOLDYjpTl52BIvOCMh",
            zhName = "乘客状态",
            kind = "FIELD"
    )
    private PassengerStateEnum passengerState = PassengerStateEnum.NORMAL;

    /**
     * 首次受困时刻（方圆属性成员原名：首次受困时刻）
     */
    @Schema(
            title = "首次受困时刻"
    )
    @GromaMeta(
            id = "eTFRIJF4ZA5HaxhN1gf",
            zhName = "首次受困时刻",
            kind = "FIELD"
    )
    private double firstTrappedTime = 0.0D;

    /**
     * 全部安全时刻（方圆属性成员原名：全部安全时刻）
     */
    @Schema(
            title = "全部安全时刻"
    )
    @GromaMeta(
            id = "QM6Hrha06zlLEKXTv5H",
            zhName = "全部安全时刻",
            kind = "FIELD"
    )
    private double allSafeTime = 0.0D;

    /**
     * 已安全人数（方圆属性成员原名：已安全人数）
     */
    @Schema(
            title = "已安全人数"
    )
    @GromaMeta(
            id = "15RcT4aahOAxSrDBdQi",
            zhName = "已安全人数",
            kind = "FIELD"
    )
    private long safePeopleCount = 0L;

    /**
     * 是否经历危险水深（方圆属性成员原名：是否经历危险水深） 默认值：false
     */
    @Schema(
            title = "是否经历危险水深"
    )
    @GromaMeta(
            id = "IY82VYr6qLtgvSCldj7",
            zhName = "是否经历危险水深",
            kind = "FIELD"
    )
    private boolean experiencedDangerDepth = false;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public PassengerGroupEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "PassengerGroup";
    }

    @Override
    public String getTypeZhName() {
        return "乘客群";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
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
        markDirty(0);
        ctx.onMemberChanged(this, "MlorlwiBiZh8KU0r87s", before, value);
    }

    /**
     * 直接写入名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public long getTotalPeopleCount() {
        return this.totalPeopleCount;
    }

    /**
     * 写入总人数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTotalPeopleCount(long value, GromaWorldContext ctx) {
        if (this.totalPeopleCount == value) {
            return;
        }
        long before = this.totalPeopleCount;
        this.totalPeopleCount = value;
        markDirty(1);
        ctx.onMemberChanged(this, "5x0OfMIlLsvkJCzGNcQ", before, value);
    }

    /**
     * 直接写入总人数（不发事件；仅用于初始化与对象创建期）
     */
    public void setTotalPeopleCount(long value) {
        this.totalPeopleCount = value;
    }

    @Override
    public PassengerStateEnum getPassengerState() {
        return this.passengerState;
    }

    /**
     * 写入乘客状态并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPassengerState(PassengerStateEnum value, GromaWorldContext ctx) {
        if (this.passengerState == value) {
            return;
        }
        PassengerStateEnum before = this.passengerState;
        this.passengerState = value;
        markDirty(2);
        ctx.onMemberChanged(this, "AnOLDYjpTl52BIvOCMh", before, value);
    }

    /**
     * 直接写入乘客状态（不发事件；仅用于初始化与对象创建期）
     */
    public void setPassengerState(PassengerStateEnum value) {
        this.passengerState = value;
    }

    @Override
    public double getFirstTrappedTime() {
        return this.firstTrappedTime;
    }

    /**
     * 写入首次受困时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setFirstTrappedTime(double value, GromaWorldContext ctx) {
        if (this.firstTrappedTime == value) {
            return;
        }
        double before = this.firstTrappedTime;
        this.firstTrappedTime = value;
        markDirty(3);
        ctx.onMemberChanged(this, "eTFRIJF4ZA5HaxhN1gf", before, value);
    }

    /**
     * 直接写入首次受困时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setFirstTrappedTime(double value) {
        this.firstTrappedTime = value;
    }

    @Override
    public double getAllSafeTime() {
        return this.allSafeTime;
    }

    /**
     * 写入全部安全时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setAllSafeTime(double value, GromaWorldContext ctx) {
        if (this.allSafeTime == value) {
            return;
        }
        double before = this.allSafeTime;
        this.allSafeTime = value;
        markDirty(4);
        ctx.onMemberChanged(this, "QM6Hrha06zlLEKXTv5H", before, value);
    }

    /**
     * 直接写入全部安全时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setAllSafeTime(double value) {
        this.allSafeTime = value;
    }

    @Override
    public long getSafePeopleCount() {
        return this.safePeopleCount;
    }

    /**
     * 写入已安全人数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSafePeopleCount(long value, GromaWorldContext ctx) {
        if (this.safePeopleCount == value) {
            return;
        }
        long before = this.safePeopleCount;
        this.safePeopleCount = value;
        markDirty(5);
        ctx.onMemberChanged(this, "15RcT4aahOAxSrDBdQi", before, value);
    }

    /**
     * 直接写入已安全人数（不发事件；仅用于初始化与对象创建期）
     */
    public void setSafePeopleCount(long value) {
        this.safePeopleCount = value;
    }

    @Override
    public boolean isExperiencedDangerDepth() {
        return this.experiencedDangerDepth;
    }

    /**
     * 写入是否经历危险水深并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setExperiencedDangerDepth(boolean value, GromaWorldContext ctx) {
        if (this.experiencedDangerDepth == value) {
            return;
        }
        boolean before = this.experiencedDangerDepth;
        this.experiencedDangerDepth = value;
        markDirty(6);
        ctx.onMemberChanged(this, "IY82VYr6qLtgvSCldj7", before, value);
    }

    /**
     * 直接写入是否经历危险水深（不发事件；仅用于初始化与对象创建期）
     */
    public void setExperiencedDangerDepth(boolean value) {
        this.experiencedDangerDepth = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "MlorlwiBiZh8KU0r87s" -> {
                return this.name;
            }
            case "5x0OfMIlLsvkJCzGNcQ" -> {
                return this.totalPeopleCount;
            }
            case "AnOLDYjpTl52BIvOCMh" -> {
                return this.passengerState;
            }
            case "eTFRIJF4ZA5HaxhN1gf" -> {
                return this.firstTrappedTime;
            }
            case "QM6Hrha06zlLEKXTv5H" -> {
                return this.allSafeTime;
            }
            case "15RcT4aahOAxSrDBdQi" -> {
                return this.safePeopleCount;
            }
            case "IY82VYr6qLtgvSCldj7" -> {
                return this.experiencedDangerDepth;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "MlorlwiBiZh8KU0r87s" -> {
                this.setName(GromaValues.toText(value), ctx);
            }
            case "5x0OfMIlLsvkJCzGNcQ" -> {
                this.setTotalPeopleCount(GromaValues.toLong(value), ctx);
            }
            case "AnOLDYjpTl52BIvOCMh" -> {
                this.setPassengerState(value == null ? null : GromaValues.toEnum(PassengerStateEnum.class, value), ctx);
            }
            case "eTFRIJF4ZA5HaxhN1gf" -> {
                this.setFirstTrappedTime(GromaValues.toDouble(value), ctx);
            }
            case "QM6Hrha06zlLEKXTv5H" -> {
                this.setAllSafeTime(GromaValues.toDouble(value), ctx);
            }
            case "15RcT4aahOAxSrDBdQi" -> {
                this.setSafePeopleCount(GromaValues.toLong(value), ctx);
            }
            case "IY82VYr6qLtgvSCldj7" -> {
                this.setExperiencedDangerDepth(GromaValues.toBoolean(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public PassengerGroupEntity copy(GromaWorldContext ctx) {
        PassengerGroupEntity copy = new PassengerGroupEntity(ctx.nextInstanceId("PassengerGroup"));
        copy.openCreation();
        copy.setName(this.getName());
        copy.setTotalPeopleCount(this.getTotalPeopleCount());
        copy.setPassengerState(this.getPassengerState());
        copy.setFirstTrappedTime(this.getFirstTrappedTime());
        copy.setAllSafeTime(this.getAllSafeTime());
        copy.setSafePeopleCount(this.getSafePeopleCount());
        copy.setExperiencedDangerDepth(this.isExperiencedDangerDepth());
        ctx.register(copy);
        return copy;
    }
}
