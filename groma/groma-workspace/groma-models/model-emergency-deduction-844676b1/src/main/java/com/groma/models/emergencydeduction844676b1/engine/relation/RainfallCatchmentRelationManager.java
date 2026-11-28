package com.groma.models.emergencydeduction844676b1.engine.relation;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.annotation.SemanticTag;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import java.lang.String;
import java.util.Set;

/**
 * 降雨汇水 强类型双向关联管理器（R08/R10）
 * <p>左端：降雨（Rainfall，约束 zeroOrMore）--(汇水)--> 右端：区域（Area，约束 one）</p>
 * <p>基数上限：左端每实例最多 2147483647 个右端、右端每实例最多 2147483647 个左端；bind 时经 ctx 派发 ra/ar（重指向 rv/vr），unbind 派发 rx/xr。</p>
 */
@GromaMeta(
        id = "E32hWqA7cw2F3sNSc8K",
        zhName = "降雨汇水",
        kind = "ASSOCIATION_MANAGER",
        relations = "汇水 / 接收"
)
@SemanticTag(
        relationId = "E32hWqA7cw2F3sNSc8K",
        targetType = "eIdrNAXcjZ1OqUVr3LB",
        forwardLabel = "汇水",
        reverseLabel = "接收",
        bidirectional = true
)
public final class RainfallCatchmentRelationManager extends BidirectionalTie<String, String> {
    public static final int MAX_RIGHT_PER_LEFT = 2147483647;

    public static final int MAX_LEFT_PER_RIGHT = 2147483647;

    public void bind(RainfallEntity left, AreaEntity right, GromaWorldContext ctx) {
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
            ctx.recordPublicProblem("[降雨汇水] 左端关联数量超出上限: " + left.getInstanceId());
            return;
        }
        if (super.getLefts(right.getInstanceId()).size() >= MAX_LEFT_PER_RIGHT) {
            ctx.recordPublicProblem("[降雨汇水] 右端关联数量超出上限: " + right.getInstanceId());
            return;
        }
        super.bind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationLinked("E32hWqA7cw2F3sNSc8K", left, "ba15DPUPM07QgiJWWfb", right, "Shu45x1YIGfSadlCre9", retargeting);
        }
    }

    public void unbind(RainfallEntity left, AreaEntity right, GromaWorldContext ctx) {
        if (left == null || right == null) {
            return;
        }
        super.unbind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationUnlinked("E32hWqA7cw2F3sNSc8K", left, "ba15DPUPM07QgiJWWfb", right, "Shu45x1YIGfSadlCre9");
        }
    }
}
