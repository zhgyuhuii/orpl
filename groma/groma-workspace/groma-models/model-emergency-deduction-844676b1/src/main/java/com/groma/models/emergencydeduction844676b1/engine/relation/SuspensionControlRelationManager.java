package com.groma.models.emergencydeduction844676b1.engine.relation;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.annotation.SemanticTag;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import java.lang.String;
import java.util.Set;

/**
 * 停运控制 强类型双向关联管理器（R08/R10）
 * <p>左端：处置方案（DisposalPlan，约束 zeroOrMore）--(控制)--> 右端：地铁区间（MetroSection，约束 one）</p>
 * <p>基数上限：左端每实例最多 2147483647 个右端、右端每实例最多 2147483647 个左端；bind 时经 ctx 派发 ra/ar（重指向 rv/vr），unbind 派发 rx/xr。</p>
 */
@GromaMeta(
        id = "kFr6nPxVVgIlStfwOCS",
        zhName = "停运控制",
        kind = "ASSOCIATION_MANAGER",
        relations = "控制 / 受控"
)
@SemanticTag(
        relationId = "kFr6nPxVVgIlStfwOCS",
        targetType = "6iap5H5evnoP56yto1c",
        forwardLabel = "控制",
        reverseLabel = "受控",
        bidirectional = true
)
public final class SuspensionControlRelationManager extends BidirectionalTie<String, String> {
    public static final int MAX_RIGHT_PER_LEFT = 2147483647;

    public static final int MAX_LEFT_PER_RIGHT = 2147483647;

    public void bind(DisposalPlanEntity left, MetroSectionEntity right, GromaWorldContext ctx) {
        if (left == null || right == null) {
            return;
        }
        Set<String> prevRights = super.getRights(left.getInstanceId());
        boolean retargeting = !prevRights.isEmpty() && MAX_RIGHT_PER_LEFT == 1 && !prevRights.contains(right.getInstanceId());
        if (retargeting) {
            for (String oldRightId : java.util.List.copyOf(prevRights)) {
                super.unbind(left.getInstanceId(), oldRightId);
            }
        }
        if (super.getRights(left.getInstanceId()).size() >= MAX_RIGHT_PER_LEFT) {
            ctx.recordPublicProblem("[停运控制] 左端关联数量超出上限: " + left.getInstanceId());
            return;
        }
        if (super.getLefts(right.getInstanceId()).size() >= MAX_LEFT_PER_RIGHT) {
            ctx.recordPublicProblem("[停运控制] 右端关联数量超出上限: " + right.getInstanceId());
            return;
        }
        super.bind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationLinked("kFr6nPxVVgIlStfwOCS", left, "QCg39qqOAAmSMQKtnOq", right, "yxNQg49WcLIlXYaYnu4", retargeting);
        }
    }

    public void unbind(DisposalPlanEntity left, MetroSectionEntity right, GromaWorldContext ctx) {
        if (left == null || right == null) {
            return;
        }
        super.unbind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationUnlinked("kFr6nPxVVgIlStfwOCS", left, "QCg39qqOAAmSMQKtnOq", right, "yxNQg49WcLIlXYaYnu4");
        }
    }
}
