package com.groma.models.smartplanning.model.dto;

import java.util.List;

public record SmartPlanningRequestDTO(
        Long seed,
        String ourIntent,
        String constraints,
        Long syncHHourSeconds,
        List<SyncStrikeForceInputDTO> availableForces
) {}
