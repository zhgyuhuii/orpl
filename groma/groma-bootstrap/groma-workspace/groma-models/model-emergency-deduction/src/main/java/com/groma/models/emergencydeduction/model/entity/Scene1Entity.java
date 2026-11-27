package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.Scene1ReadOnly;
import com.groma.models.emergencydeduction.model.enums.TrainStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 场景1实体（方圆对象类型原名：场景1）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>列车状态1（trainState1，ENUM）</li>
 * <li>文本1（text1，BASE）</li>
 * <li>文本2（text2，BASE）</li>
 * </ul>
 * 事件接线（类型设计视图）：
 * <ul>
 * <li>当文本2 发生【对象出现(a)】 时，启动过程「1转换为停站」</li>
 * <li>当列车状态1 进入状态「停站」 发生【状态进入(sa)】 时，启动过程「2把否变成是」</li>
 * <li>当文本1 发生【值变化(v)】 且满足线上判断 = true 时，启动过程「3转换为行驶」</li>
 * <li>当列车状态1 进入状态「行驶」 发生【状态进入(sa)】 时，启动过程「4生成对象并把是变成否」</li>
 * </ul>
 */
@Schema(
        title = "场景1",
        description = "方圆对象类型：场景1"
)
@GromaMeta(
        id = "bX4vMzSlVMdk6QjF7CD",
        zhName = "场景1",
        kind = "TYPE"
)
public class Scene1Entity extends BaseSimEntity implements Scene1ReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("xuFANgtC3Dzw6jUE1H4", "列车状态1", "trainState1", "ENUM", "com.groma.models.emergencydeduction.model.enums.TrainStateEnum", ""), new GromaMemberMeta("GKzjQYuz4zLjBQGdWxy", "文本1", "text1", "BASE", "boolean", ""), new GromaMemberMeta("Cwaojup8iZOpBuisiCL", "文本2", "text2", "BASE", "java.lang.String", ""));

    /**
     * 列车状态1（方圆状态成员原名：列车状态1） 默认值：待进入 初始状态：待进入
     * 事件触发关系：当列车状态1 进入状态「停站」 发生【状态进入(sa)】 时，启动过程「2把否变成是」
     * 事件触发关系：当列车状态1 进入状态「行驶」 发生【状态进入(sa)】 时，启动过程「4生成对象并把是变成否」
     */
    @Schema(
            title = "列车状态1"
    )
    @GromaMeta(
            id = "xuFANgtC3Dzw6jUE1H4",
            zhName = "列车状态1",
            kind = "FIELD",
            initState = "待进入"
    )
    private TrainStateEnum trainState1 = TrainStateEnum.PENDING_ENTRY;

    /**
     * 文本1（方圆属性成员原名：文本1） 默认值：false
     * 事件触发关系：当文本1 发生【值变化(v)】 且满足线上判断 = true 时，启动过程「3转换为行驶」
     */
    @Schema(
            title = "文本1"
    )
    @GromaMeta(
            id = "GKzjQYuz4zLjBQGdWxy",
            zhName = "文本1",
            kind = "FIELD"
    )
    private boolean text1 = false;

    /**
     * 文本2（方圆属性成员原名：文本2）
     * 事件触发关系：当文本2 发生【对象出现(a)】 时，启动过程「1转换为停站」
     */
    @Schema(
            title = "文本2"
    )
    @GromaMeta(
            id = "Cwaojup8iZOpBuisiCL",
            zhName = "文本2",
            kind = "FIELD"
    )
    private String text2 = "";

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public Scene1Entity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "Scene1";
    }

    @Override
    public String getTypeZhName() {
        return "场景1";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public TrainStateEnum getTrainState1() {
        return this.trainState1;
    }

    /**
     * 写入列车状态1并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrainState1(TrainStateEnum value, GromaWorldContext ctx) {
        if (this.trainState1 == value) {
            return;
        }
        TrainStateEnum before = this.trainState1;
        this.trainState1 = value;
        markDirty(0);
        ctx.onMemberChanged(this, "xuFANgtC3Dzw6jUE1H4", before, value);
    }

    /**
     * 直接写入列车状态1（不发事件；仅用于初始化与对象创建期）
     */
    public void setTrainState1(TrainStateEnum value) {
        this.trainState1 = value;
    }

    @Override
    public boolean isText1() {
        return this.text1;
    }

    /**
     * 写入文本1并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setText1(boolean value, GromaWorldContext ctx) {
        if (this.text1 == value) {
            return;
        }
        boolean before = this.text1;
        this.text1 = value;
        markDirty(1);
        ctx.onMemberChanged(this, "GKzjQYuz4zLjBQGdWxy", before, value);
    }

    /**
     * 直接写入文本1（不发事件；仅用于初始化与对象创建期）
     */
    public void setText1(boolean value) {
        this.text1 = value;
    }

    @Override
    public String getText2() {
        return this.text2;
    }

    /**
     * 写入文本2并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setText2(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.text2, value)) {
            return;
        }
        String before = this.text2;
        this.text2 = value;
        markDirty(2);
        ctx.onMemberChanged(this, "Cwaojup8iZOpBuisiCL", before, value);
    }

    /**
     * 直接写入文本2（不发事件；仅用于初始化与对象创建期）
     */
    public void setText2(String value) {
        this.text2 = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "xuFANgtC3Dzw6jUE1H4" -> {
                return this.trainState1;
            }
            case "GKzjQYuz4zLjBQGdWxy" -> {
                return this.text1;
            }
            case "Cwaojup8iZOpBuisiCL" -> {
                return this.text2;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "xuFANgtC3Dzw6jUE1H4" -> {
                this.setTrainState1(value == null ? null : GromaValues.toEnum(TrainStateEnum.class, value), ctx);
            }
            case "GKzjQYuz4zLjBQGdWxy" -> {
                this.setText1(GromaValues.toBoolean(value), ctx);
            }
            case "Cwaojup8iZOpBuisiCL" -> {
                this.setText2(GromaValues.toText(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public Scene1Entity copy(GromaWorldContext ctx) {
        Scene1Entity copy = new Scene1Entity(ctx.nextInstanceId("Scene1"));
        copy.openCreation();
        copy.setTrainState1(this.getTrainState1());
        copy.setText1(this.isText1());
        copy.setText2(this.getText2());
        ctx.register(copy);
        return copy;
    }
}
