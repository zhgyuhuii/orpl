package com.groma.models.smartplanning.service.impl;

import com.groma.models.smartplanning.engine.context.SmartPlanningWorldContext;
import com.groma.models.smartplanning.model.dto.SmartPlanningRequestDTO;
import com.groma.models.smartplanning.model.vo.SmartPlanningSnapshotVO;
import com.groma.models.smartplanning.service.IRoutePlanningSyncArrivalService;
import com.groma.models.smartplanning.service.ISmartPlanningSceneService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SmartPlanningSceneServiceImpl implements ISmartPlanningSceneService {

    private final IRoutePlanningSyncArrivalService routeService;
    private final AtomicInteger seq = new AtomicInteger(0);

    public SmartPlanningSceneServiceImpl(IRoutePlanningSyncArrivalService routeService) {
        this.routeService = routeService;
    }

    @Override
    public SmartPlanningSnapshotVO runFullPlanningPipeline(SmartPlanningRequestDTO dto) {
        SmartPlanningRequestDTO safe = dto != null
                ? dto
                : new SmartPlanningRequestDTO(null, null, null, 7_200L, List.of());
        long seed = safe.seed() != null ? safe.seed() : System.nanoTime();
        String sid = "sp_sim_" + seq.incrementAndGet();
        SmartPlanningWorldContext ctx = new SmartPlanningWorldContext(
                sid, System.currentTimeMillis(), seed, safe, routeService);
        ctx.bindLoopThread(Thread.currentThread());
        try {
            ctx.initializeOnLoop();
            for (int step = 0; step < 3; step++) {
                if (!ctx.stepOneTickOnLoop()) {
                    break;
                }
            }
            return ctx.toVO();
        } finally {
            ctx.bindLoopThread(null);
        }
    }
}
