package com.groma.models.emergencydeduction.model.entity;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.api.DisposalPlanReadOnly;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

/**
 * 处置方案实体（方圆对象类型原名：处置方案）
 * <p>
 * 成员写入统一走带世界上下文的 Setter：框架按成员种类发出值变化(v)、状态进入/退出(sa/sx)、引用变化(ra/rv/rx) 事件，
 * 并维护反向引用索引，供类型设计视图里登记的事件接线派发。对象创建期间（首次被引用前）的成员赋值不产生事件。
 * </p>
 * 成员清单：
 * <ul>
 * <li>方案编号（planCode，BASE）</li>
 * <li>方案名称（planName，BASE）</li>
 * <li>关联列车（associatedTrain，REF）</li>
 * <li>列车通过所需时间（trainPassTime，BASE）</li>
 * <li>停运下达时刻（suspensionIssueTime，BASE）</li>
 * <li>停止放行时刻（releaseStopTime，BASE）</li>
 * </ul>
 */
@Schema(
        title = "处置方案",
        description = "方圆对象类型：处置方案"
)
@GromaMeta(
        id = "CxXzMPDWEghBGY5Xyl7",
        zhName = "处置方案",
        kind = "TYPE"
)
public class DisposalPlanEntity extends BaseSimEntity implements DisposalPlanReadOnly {
    private static final List<GromaMemberMeta> MEMBERS = List.of(new GromaMemberMeta("51GmDUCZUE3bvJyQAPr", "方案编号", "planCode", "BASE", "java.lang.String", ""), new GromaMemberMeta("zCymV4rfDUqj9GavE6o", "方案名称", "planName", "BASE", "java.lang.String", ""), new GromaMemberMeta("NqI2ixqqjhZg3mIgc7I", "关联列车", "associatedTrain", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("nIrxHR9S6EvG66TIyVD", "列车通过所需时间", "trainPassTime", "BASE", "double", ""), new GromaMemberMeta("ze68hlsyh2tOYLCtdWk", "停运下达时刻", "suspensionIssueTime", "BASE", "double", ""), new GromaMemberMeta("ruy5VzXmYd3iZMkNX7i", "停止放行时刻", "releaseStopTime", "BASE", "double", ""));

    /**
     * 方案编号（方圆属性成员原名：方案编号）
     */
    @Schema(
            title = "方案编号"
    )
    @GromaMeta(
            id = "51GmDUCZUE3bvJyQAPr",
            zhName = "方案编号",
            kind = "FIELD"
    )
    private String planCode = "";

    /**
     * 方案名称（方圆属性成员原名：方案名称）
     */
    @Schema(
            title = "方案名称"
    )
    @GromaMeta(
            id = "zCymV4rfDUqj9GavE6o",
            zhName = "方案名称",
            kind = "FIELD"
    )
    private String planName = "";

    /**
     * 关联列车（方圆对象成员原名：关联列车）
     */
    @Schema(
            title = "关联列车"
    )
    @GromaMeta(
            id = "NqI2ixqqjhZg3mIgc7I",
            zhName = "关联列车",
            kind = "FIELD"
    )
    private TrainEntity associatedTrain = null;

    /**
     * 列车通过所需时间（方圆属性成员原名：列车通过所需时间）
     */
    @Schema(
            title = "列车通过所需时间"
    )
    @GromaMeta(
            id = "nIrxHR9S6EvG66TIyVD",
            zhName = "列车通过所需时间",
            kind = "FIELD"
    )
    private double trainPassTime = 0.0D;

    /**
     * 停运下达时刻（方圆属性成员原名：停运下达时刻）
     */
    @Schema(
            title = "停运下达时刻"
    )
    @GromaMeta(
            id = "ze68hlsyh2tOYLCtdWk",
            zhName = "停运下达时刻",
            kind = "FIELD"
    )
    private double suspensionIssueTime = 0.0D;

    /**
     * 停止放行时刻（方圆属性成员原名：停止放行时刻）
     */
    @Schema(
            title = "停止放行时刻"
    )
    @GromaMeta(
            id = "ruy5VzXmYd3iZMkNX7i",
            zhName = "停止放行时刻",
            kind = "FIELD"
    )
    private double releaseStopTime = 0.0D;

    /**
     * @param instanceId 实例标识（命名实例取画布对象实体 id，运行期生成的对象由世界上下文分配）
     */
    public DisposalPlanEntity(String instanceId) {
        super(instanceId);
    }

    @Override
    public String getTypeCode() {
        return "DisposalPlan";
    }

    @Override
    public String getTypeZhName() {
        return "处置方案";
    }

    @Override
    public List<GromaMemberMeta> memberMetas() {
        return MEMBERS;
    }

    @Override
    public String getPlanCode() {
        return this.planCode;
    }

    /**
     * 写入方案编号并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPlanCode(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.planCode, value)) {
            return;
        }
        String before = this.planCode;
        this.planCode = value;
        markDirty(0);
        ctx.onMemberChanged(this, "51GmDUCZUE3bvJyQAPr", before, value);
    }

    /**
     * 直接写入方案编号（不发事件；仅用于初始化与对象创建期）
     */
    public void setPlanCode(String value) {
        this.planCode = value;
    }

    @Override
    public String getPlanName() {
        return this.planName;
    }

    /**
     * 写入方案名称并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setPlanName(String value, GromaWorldContext ctx) {
        if (java.util.Objects.equals(this.planName, value)) {
            return;
        }
        String before = this.planName;
        this.planName = value;
        markDirty(1);
        ctx.onMemberChanged(this, "zCymV4rfDUqj9GavE6o", before, value);
    }

    /**
     * 直接写入方案名称（不发事件；仅用于初始化与对象创建期）
     */
    public void setPlanName(String value) {
        this.planName = value;
    }

    @Override
    public TrainEntity getAssociatedTrain() {
        return this.associatedTrain;
    }

    /**
     * 写入关联列车并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setAssociatedTrain(TrainEntity value, GromaWorldContext ctx) {
        if (this.associatedTrain == value) {
            return;
        }
        TrainEntity before = this.associatedTrain;
        this.associatedTrain = value;
        markDirty(2);
        ctx.onMemberChanged(this, "NqI2ixqqjhZg3mIgc7I", before, value);
    }

    @Override
    public double getTrainPassTime() {
        return this.trainPassTime;
    }

    /**
     * 写入列车通过所需时间并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setTrainPassTime(double value, GromaWorldContext ctx) {
        if (this.trainPassTime == value) {
            return;
        }
        double before = this.trainPassTime;
        this.trainPassTime = value;
        markDirty(3);
        ctx.onMemberChanged(this, "nIrxHR9S6EvG66TIyVD", before, value);
    }

    /**
     * 直接写入列车通过所需时间（不发事件；仅用于初始化与对象创建期）
     */
    public void setTrainPassTime(double value) {
        this.trainPassTime = value;
    }

    @Override
    public double getSuspensionIssueTime() {
        return this.suspensionIssueTime;
    }

    /**
     * 写入停运下达时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setSuspensionIssueTime(double value, GromaWorldContext ctx) {
        if (this.suspensionIssueTime == value) {
            return;
        }
        double before = this.suspensionIssueTime;
        this.suspensionIssueTime = value;
        markDirty(4);
        ctx.onMemberChanged(this, "ze68hlsyh2tOYLCtdWk", before, value);
    }

    /**
     * 直接写入停运下达时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setSuspensionIssueTime(double value) {
        this.suspensionIssueTime = value;
    }

    @Override
    public double getReleaseStopTime() {
        return this.releaseStopTime;
    }

    /**
     * 写入停止放行时刻并发出对应事件（初始化静默期与对象创建期间不发事件）
     */
    public void setReleaseStopTime(double value, GromaWorldContext ctx) {
        if (this.releaseStopTime == value) {
            return;
        }
        double before = this.releaseStopTime;
        this.releaseStopTime = value;
        markDirty(5);
        ctx.onMemberChanged(this, "ruy5VzXmYd3iZMkNX7i", before, value);
    }

    /**
     * 直接写入停止放行时刻（不发事件；仅用于初始化与对象创建期）
     */
    public void setReleaseStopTime(double value) {
        this.releaseStopTime = value;
    }

    @Override
    public Object getMemberValue(String token) {
        switch (token) {
            case "51GmDUCZUE3bvJyQAPr" -> {
                return this.planCode;
            }
            case "zCymV4rfDUqj9GavE6o" -> {
                return this.planName;
            }
            case "NqI2ixqqjhZg3mIgc7I" -> {
                return this.associatedTrain;
            }
            case "nIrxHR9S6EvG66TIyVD" -> {
                return this.trainPassTime;
            }
            case "ze68hlsyh2tOYLCtdWk" -> {
                return this.suspensionIssueTime;
            }
            case "ruy5VzXmYd3iZMkNX7i" -> {
                return this.releaseStopTime;
            }
            default -> { return BaseSimEntity.MEMBER_NOT_FOUND; }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setMemberValue(String token, Object value, GromaWorldContext ctx) {
        switch (token) {
            case "51GmDUCZUE3bvJyQAPr" -> {
                this.setPlanCode(GromaValues.toText(value), ctx);
            }
            case "zCymV4rfDUqj9GavE6o" -> {
                this.setPlanName(GromaValues.toText(value), ctx);
            }
            case "NqI2ixqqjhZg3mIgc7I" -> {
                this.setAssociatedTrain((TrainEntity) GromaValues.decode(value, ctx), ctx);
            }
            case "nIrxHR9S6EvG66TIyVD" -> {
                this.setTrainPassTime(GromaValues.toDouble(value), ctx);
            }
            case "ze68hlsyh2tOYLCtdWk" -> {
                this.setSuspensionIssueTime(GromaValues.toDouble(value), ctx);
            }
            case "ruy5VzXmYd3iZMkNX7i" -> {
                this.setReleaseStopTime(GromaValues.toDouble(value), ctx);
            }
            default -> { return false; }
        }
        return true;
    }

    /**
     * 值复制（赋值运算）：基础值与枚举值逐成员复制，对象引用按引用复制；副本以新实例标识登记进世界
     */
    @Override
    public DisposalPlanEntity copy(GromaWorldContext ctx) {
        DisposalPlanEntity copy = new DisposalPlanEntity(ctx.nextInstanceId("DisposalPlan"));
        copy.openCreation();
        copy.setPlanCode(this.getPlanCode());
        copy.setPlanName(this.getPlanName());
        copy.setAssociatedTrain(this.getAssociatedTrain(), ctx);
        copy.setTrainPassTime(this.getTrainPassTime());
        copy.setSuspensionIssueTime(this.getSuspensionIssueTime());
        copy.setReleaseStopTime(this.getReleaseStopTime());
        ctx.register(copy);
        return copy;
    }
}
