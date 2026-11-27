package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.PlanExecutorReadOnly;
import com.groma.models.emergencydeduction.model.enums.TaskStateEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 方案执行器实体（方圆对象类型原名：方案执行器）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>在行方案（activePlan，REF）</li>
 * <li>当前方案阶段（currentPlanStage，ENUM）</li>
 * <li>实际下达时刻（actualIssueTime，BASE）</li>
 * <li>实际生效时刻（actualEffectiveTime，BASE）</li>
 * </ul>
 */
@Schema(
        title = "方案执行器",
        description = "方圆对象类型：方案执行器"
)
@GromaMeta(
        id = "bSGKABguV3sqda7gC7w",
        zhName = "方案执行器",
        kind = "TYPE"
)
public class PlanExecutorEntity extends BaseSimEntity implements PlanExecutorReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("DtQr9wp5Yd34LeqZVp6", "在行方案", "activePlan", "REF", "com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity", ""), new GromaMemberMeta("OJJ7TO7kt5NUf79Sizn", "当前方案阶段", "currentPlanStage", "ENUM", "com.groma.models.emergencydeduction.model.enums.TaskStateEnum", ""), new GromaMemberMeta("WqYtoJd7SckcWKXUotn", "实际下达时刻", "actualIssueTime", "BASE", "double", ""), new GromaMemberMeta("uJqrJjseEN6Su0n5lQ5", "实际生效时刻", "actualEffectiveTime", "BASE", "double", ""));

    /**
     * 在行方案（方圆对象成员原名：在行方案）
     */
    @Schema(
            title = "在行方案"
    )
    @GromaMeta(
            id = "DtQr9wp5Yd34LeqZVp6",
            zhName = "在行方案",
            kind = "FIELD"
    )
    private DisposalPlanEntity activePlan = null;

    /**
     * 当前方案阶段（方圆状态成员原名：当前方案阶段） 初始状态：待下达
     */
    @Schema(
            title = "当前方案阶段"
    )
    @GromaMeta(
            id = "OJJ7TO7kt5NUf79Sizn",
            zhName = "当前方案阶段",
            kind = "FIELD",
            initState = "待下达"
    )
    private TaskStateEnum currentPlanStage = TaskStateEnum.PENDING_ISSUE;

    /**
     * 实际下达时刻（方圆属性成员原名：实际下达时刻）
     */
    @Schema(
            title = "实际下达时刻"
    )
    @GromaMeta(
            id = "WqYtoJd7SckcWKXUotn",
            zhName = "实际下达时刻",
            kind = "FIELD"
    )
    private double actualIssueTime = 0.0D;

    /**
     * 实际生效时刻（方圆属性成员原名：实际生效时刻）
     */
    @Schema(
            title = "实际生效时刻"
    )
    @GromaMeta(
            id = "uJqrJjseEN6Su0n5lQ5",
            zhName = "实际生效时刻",
            kind = "FIELD"
    )
    private double actualEffectiveTime = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public PlanExecutorEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "PlanExecutor";
    }

    @Override
    public String getTypeZhName() {
        return "方案执行器";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public DisposalPlanEntity getActivePlan() {
        return this.activePlan;
    }

    /**
     * 写入在行方案并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setActivePlan(DisposalPlanEntity value, GromaWorldContext ctx) {
        if (this.activePlan == value) {
            return;
        }
        DisposalPlanEntity before = this.activePlan;
        this.activePlan = value;
        markDirty(0);
        ctx.onMemberChanged(this, "DtQr9wp5Yd34LeqZVp6", before, value);
    }

    @Override
    public TaskStateEnum getCurrentPlanStage() {
        return this.currentPlanStage;
    }

    /**
     * 写入当前方案阶段并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setCurrentPlanStage(TaskStateEnum value, GromaWorldContext ctx) {
        if (this.currentPlanStage == value) {
            return;
        }
        TaskStateEnum before = this.currentPlanStage;
        this.currentPlanStage = value;
        markDirty(1);
        ctx.onMemberChanged(this, "OJJ7TO7kt5NUf79Sizn", before, value);
    }

    /**
     * 直接写入当前方案阶段（不发事件；仅用于初始化与对象创建期）
     */
    public void setCurrentPlanStage(TaskStateEnum value) {
        this.currentPlanStage = value;
    }

    @Override
    public double getActualIssueTime() {
        return this.actualIssueTime;
    }

    /**
     * 写入实际下达时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setActualIssueTime(double value, GromaWorldContext ctx) {
        if (this.actualIssueTime == value) {
            return;
        }
        double before = this.actualIssueTime;
        this.actualIssueTime = value;
        markDirty(2);
        ctx.onMemberChanged(this, "WqYtoJd7SckcWKXUotn", before, value);
    }

    /**
     * 直接写入实际下达时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setActualIssueTime(double value) {
        this.actualIssueTime = value;
    }

    @Override
    public double getActualEffectiveTime() {
        return this.actualEffectiveTime;
    }

    /**
     * 写入实际生效时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setActualEffectiveTime(double value, GromaWorldContext ctx) {
        if (this.actualEffectiveTime == value) {
            return;
        }
        double before = this.actualEffectiveTime;
        this.actualEffectiveTime = value;
        markDirty(3);
        ctx.onMemberChanged(this, "uJqrJjseEN6Su0n5lQ5", before, value);
    }

    /**
     * 直接写入实际生效时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setActualEffectiveTime(double value) {
        this.actualEffectiveTime = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "DtQr9wp5Yd34LeqZVp6" -> {
                return this.activePlan;
            }
            case "OJJ7TO7kt5NUf79Sizn" -> {
                return this.currentPlanStage;
            }
            case "WqYtoJd7SckcWKXUotn" -> {
                return this.actualIssueTime;
            }
            case "uJqrJjseEN6Su0n5lQ5" -> {
                return this.actualEffectiveTime;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "DtQr9wp5Yd34LeqZVp6" -> {
                this.setActivePlan((DisposalPlanEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "OJJ7TO7kt5NUf79Sizn" -> {
                this.setCurrentPlanStage(value == null ? null : GromaValues.toEnum(TaskStateEnum.class, value), ctx);
            }
            case "WqYtoJd7SckcWKXUotn" -> {
                this.setActualIssueTime(GromaValues.toDouble(value), ctx);
            }
            case "uJqrJjseEN6Su0n5lQ5" -> {
                this.setActualEffectiveTime(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public PlanExecutorEntity copy(GromaWorldContext ctx) {
        PlanExecutorEntity copy = new PlanExecutorEntity(ctx.nextInstanceId("PlanExecutor"));
        copy.openCreation();
        copy.setActivePlan(this.getActivePlan(), ctx);
        copy.setCurrentPlanStage(this.getCurrentPlanStage());
        copy.setActualIssueTime(this.getActualIssueTime());
        copy.setActualEffectiveTime(this.getActualEffectiveTime());
        ctx.register(copy);
        return copy;
    }
}
