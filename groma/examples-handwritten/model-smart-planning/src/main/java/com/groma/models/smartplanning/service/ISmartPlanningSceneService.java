package com.groma.models.smartplanning.service;

import com.groma.models.smartplanning.model.dto.SmartPlanningRequestDTO;
import com.groma.models.smartplanning.model.vo.SmartPlanningSnapshotVO;

public interface ISmartPlanningSceneService {
    SmartPlanningSnapshotVO runFullPlanningPipeline(SmartPlanningRequestDTO dto);
}
