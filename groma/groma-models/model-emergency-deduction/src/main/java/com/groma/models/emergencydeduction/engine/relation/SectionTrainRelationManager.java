package com.groma.models.emergencydeduction.engine.relation;

import com.groma.base.common.container.BidirectionalTie;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;

/**
 * 双向关联管理器：区间与运行列车（对应 ASSOCIATION-TYPE: 列车通行, eid=OYsAlP9v1talH7UAmOD）
 */
public final class SectionTrainRelationManager extends BidirectionalTie<MetroSectionEntity, TrainEntity> {
}
