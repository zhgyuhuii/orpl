package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.container.ObservableEntityList;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.SuspensionControlReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * 停运控制实体（方圆对象类型原名：停运控制）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>决策方案（decisionPlan，REF）</li>
 * <li>控制区间（controlSection，LIST）</li>
 * </ul>
 */
@Schema(
        title = "停运控制",
        description = "方圆对象类型：停运控制"
)
@GromaMeta(
        id = "kFr6nPxVVgIlStfwOCS",
        zhName = "停运控制",
        kind = "TYPE"
)
public class SuspensionControlEntity extends BaseSimEntity implements SuspensionControlReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("QCg39qqOAAmSMQKtnOq", "决策方案", "decisionPlan", "REF", "com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity", ""), new GromaMemberMeta("yxNQg49WcLIlXYaYnu4", "控制区间", "controlSection", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.MetroSectionEntity>", ""));

    /**
     * 决策方案（方圆对象成员原名：决策方案）
     */
    @Schema(
            title = "决策方案"
    )
    @GromaMeta(
            id = "QCg39qqOAAmSMQKtnOq",
            zhName = "决策方案",
            kind = "FIELD"
    )
    private DisposalPlanEntity decisionPlan = null;

    /**
     * 控制区间（方圆属性成员原名：控制区间）
     */
    @Schema(
            title = "控制区间"
    )
    @GromaMeta(
            id = "yxNQg49WcLIlXYaYnu4",
            zhName = "控制区间",
            kind = "FIELD"
    )
    private List<MetroSectionEntity> controlSection = new ObservableEntityList<>(new ArrayList<>(16), null);

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public SuspensionControlEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "SuspensionControl";
    }

    @Override
    public String getTypeZhName() {
        return "停运控制";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public DisposalPlanEntity getDecisionPlan() {
        return this.decisionPlan;
    }

    /**
     * 写入决策方案并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setDecisionPlan(DisposalPlanEntity value, GromaWorldContext ctx) {
        if (this.decisionPlan == value) {
            return;
        }
        DisposalPlanEntity before = this.decisionPlan;
        this.decisionPlan = value;
        markDirty(0);
        ctx.onMemberChanged(this, "QCg39qqOAAmSMQKtnOq", before, value);
    }

    @Override
    public List<MetroSectionEntity> getControlSection() {
        return this.controlSection;
    }

    /**
     * 写入控制区间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setControlSection(List<MetroSectionEntity> value, GromaWorldContext ctx) {
        if (this.controlSection == value) {
            return;
        }
        List<MetroSectionEntity> before = this.controlSection;
        this.controlSection = ObservableEntityList.observed(value, ctx.collectionSink(this, "yxNQg49WcLIlXYaYnu4"));
        markDirty(1);
        ctx.onMemberChanged(this, "yxNQg49WcLIlXYaYnu4", before, value);
    }

    /**
     * 直接写入控制区间（不发事件；仅用于初始化与对象创建期）
     */
    public void setControlSection(List<MetroSectionEntity> value) {
        this.controlSection = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "QCg39qqOAAmSMQKtnOq" -> {
                return this.decisionPlan;
            }
            case "yxNQg49WcLIlXYaYnu4" -> {
                return this.controlSection;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "QCg39qqOAAmSMQKtnOq" -> {
                this.setDecisionPlan((DisposalPlanEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "yxNQg49WcLIlXYaYnu4" -> {
                this.setControlSection((List<MetroSectionEntity>) GromaValues.decode(value, ctx), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public SuspensionControlEntity copy(GromaWorldContext ctx) {
        SuspensionControlEntity copy = new SuspensionControlEntity(ctx.nextInstanceId("SuspensionControl"));
        copy.openCreation();
        copy.setDecisionPlan(this.getDecisionPlan(), ctx);
        copy.setControlSection(this.getControlSection());
        ctx.register(copy);
        return copy;
    }
}
