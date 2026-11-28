package com.groma.models.emergencydeduction844676b1.engine.relation;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.common.annotation.SemanticTag;
import com.groma.base.common.container.BidirectionalTie;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import java.lang.String;
import java.util.Set;

/**
 * 挡水防护 强类型双向关联管理器（R08/R10）
 * <p>左端：车站（Station，约束 one）--(防护)--> 右端：地铁区间（MetroSection，约束 zeroOrMore）</p>
 * <p>基数上限：左端每实例最多 2147483647 个右端、右端每实例最多 2147483647 个左端；bind 时经 ctx 派发 ra/ar（重指向 rv/vr），unbind 派发 rx/xr。</p>
 */
@GromaMeta(
        id = "a0FNVLNrTFTjGHd2bYh",
        zhName = "挡水防护",
        kind = "ASSOCIATION_MANAGER",
        relations = "防护 / 受护"
)
@SemanticTag(
        relationId = "a0FNVLNrTFTjGHd2bYh",
        targetType = "6iap5H5evnoP56yto1c",
        forwardLabel = "防护",
        reverseLabel = "受护",
        bidirectional = true
)
public final class FloodProtectionRelationManager extends BidirectionalTie<String, String> {
    public static final int MAX_RIGHT_PER_LEFT = 2147483647;

    public static final int MAX_LEFT_PER_RIGHT = 2147483647;

    public void bind(StationEntity left, MetroSectionEntity right, GromaWorldContext ctx) {
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
            ctx.recordPublicProblem("[挡水防护] 左端关联数量超出上限: " + left.getInstanceId());
            return;
        }
        if (super.getLefts(right.getInstanceId()).size() >= MAX_LEFT_PER_RIGHT) {
            ctx.recordPublicProblem("[挡水防护] 右端关联数量超出上限: " + right.getInstanceId());
            return;
        }
        super.bind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationLinked("a0FNVLNrTFTjGHd2bYh", left, "m0CoVE4j8kj040bE79P", right, "bwGKCA8ZWLc3ok0uzQz", retargeting);
        }
    }

    public void unbind(StationEntity left, MetroSectionEntity right, GromaWorldContext ctx) {
        if (left == null || right == null) {
            return;
        }
        super.unbind(left.getInstanceId(), right.getInstanceId());
        if (ctx != null) {
            ctx.emitRelationUnlinked("a0FNVLNrTFTjGHd2bYh", left, "m0CoVE4j8kj040bE79P", right, "bwGKCA8ZWLc3ok0uzQz");
        }
    }
}
