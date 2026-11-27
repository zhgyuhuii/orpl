package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.TrainWithPassengerGroupReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 列车与车内乘客群实体（方圆对象类型原名：列车与车内乘客群）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>列车（train，REF）</li>
 * <li>乘客群（passengerGroup，REF）</li>
 * <li>当前在车人数（currentOnBoardCount，BASE）</li>
 * </ul>
 */
@Schema(
        title = "列车与车内乘客群",
        description = "方圆对象类型：列车与车内乘客群"
)
@GromaMeta(
        id = "nYxypzPOPAMpit1FFqW",
        zhName = "列车与车内乘客群",
        kind = "TYPE"
)
public class TrainWithPassengerGroupEntity extends BaseSimEntity implements TrainWithPassengerGroupReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("ZcaVkzem6xwLhB1el0Q", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("0U8JtY9HR2pGe4LO3TV", "乘客群", "passengerGroup", "REF", "com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity", ""), new GromaMemberMeta("1EXKYn7RxkMlFg71Dif", "当前在车人数", "currentOnBoardCount", "BASE", "long", ""));

    /**
     * 列车（方圆对象成员原名：列车）
     */
    @Schema(
            title = "列车"
    )
    @GromaMeta(
            id = "ZcaVkzem6xwLhB1el0Q",
            zhName = "列车",
            kind = "FIELD"
    )
    private TrainEntity train = null;

    /**
     * 乘客群（方圆对象成员原名：乘客群）
     */
    @Schema(
            title = "乘客群"
    )
    @GromaMeta(
            id = "0U8JtY9HR2pGe4LO3TV",
            zhName = "乘客群",
            kind = "FIELD"
    )
    private PassengerGroupEntity passengerGroup = null;

    /**
     * 当前在车人数（方圆属性成员原名：当前在车人数）
     */
    @Schema(
            title = "当前在车人数"
    )
    @GromaMeta(
            id = "1EXKYn7RxkMlFg71Dif",
            zhName = "当前在车人数",
            kind = "FIELD"
    )
    private long currentOnBoardCount = 0L;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public TrainWithPassengerGroupEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "TrainWithPassengerGroup";
    }

    @Override
    public String getTypeZhName() {
        return "列车与车内乘客群";
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
        ctx.onMemberChanged(this, "ZcaVkzem6xwLhB1el0Q", before, value);
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
        markDirty(1);
        ctx.onMemberChanged(this, "0U8JtY9HR2pGe4LO3TV", before, value);
    }

    @Override
    public long getCurrentOnBoardCount() {
        return this.currentOnBoardCount;
    }

    /**
     * 写入当前在车人数并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentOnBoardCount(long value, GromaWorldContext ctx) {
        if (this.currentOnBoardCount == value) {
            return;
        }
        long before = this.currentOnBoardCount;
        this.currentOnBoardCount = value;
        markDirty(2);
        ctx.onMemberChanged(this, "1EXKYn7RxkMlFg71Dif", before, value);
    }

    /**
     * 直接写入当前在车人数（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentOnBoardCount(long value) {
        this.currentOnBoardCount = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "ZcaVkzem6xwLhB1el0Q" -> {
                return this.train;
            }
            case "0U8JtY9HR2pGe4LO3TV" -> {
                return this.passengerGroup;
            }
            case "1EXKYn7RxkMlFg71Dif" -> {
                return this.currentOnBoardCount;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "ZcaVkzem6xwLhB1el0Q" -> {
                this.setTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "0U8JtY9HR2pGe4LO3TV" -> {
                this.setPassengerGroup((PassengerGroupEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "1EXKYn7RxkMlFg71Dif" -> {
                this.setCurrentOnBoardCount(GromaValues.toLong(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public TrainWithPassengerGroupEntity copy(GromaWorldContext ctx) {
        TrainWithPassengerGroupEntity copy = new TrainWithPassengerGroupEntity(ctx.nextInstanceId("TrainWithPassengerGroup"));
        copy.openCreation();
        copy.setTrain(this.getTrain(), ctx);
        copy.setPassengerGroup(this.getPassengerGroup(), ctx);
        copy.setCurrentOnBoardCount(this.getCurrentOnBoardCount());
        ctx.register(copy);
        return copy;
    }
}
