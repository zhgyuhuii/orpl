package com.groma.models.smartplanning.service;

import com.groma.models.smartplanning.model.dto.SyncStrikeForceInputDTO;
import com.groma.models.smartplanning.model.vo.RoutePlanItemVO;

import java.util.List;

public interface IRoutePlanningSyncArrivalService {
    List<RoutePlanItemVO> computeSynchronizedRoutes(long hHourSec, List<SyncStrikeForceInputDTO> forces);
}
