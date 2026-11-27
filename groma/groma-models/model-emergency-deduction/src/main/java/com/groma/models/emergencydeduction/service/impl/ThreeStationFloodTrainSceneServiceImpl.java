package com.groma.models.emergencydeduction.service.impl;

import com.groma.models.emergencydeduction.engine.context.ThreeStationFloodTrainWorldContext;
import com.groma.models.emergencydeduction.model.dto.ThreeStationSceneStartDTO;
import com.groma.models.emergencydeduction.model.vo.ThreeStationSceneSnapshotVO;
import com.groma.models.emergencydeduction.service.IHydrologyAndTrainOperatorService;
import com.groma.models.emergencydeduction.service.IThreeStationFloodTrainSceneService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ThreeStationFloodTrainSceneServiceImpl implements IThreeStationFloodTrainSceneService {

    private final IHydrologyAndTrainOperatorService operatorService;
    private final ConcurrentHashMap<String, ThreeStationFloodTrainWorldContext> sessions = new ConcurrentHashMap<>(32);
    private final AtomicInteger seq = new AtomicInteger(0);

    public ThreeStationFloodTrainSceneServiceImpl(IHydrologyAndTrainOperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @Override
    public String startSession(ThreeStationSceneStartDTO dto) {
        ThreeStationSceneStartDTO safe = dto != null ? dto : new ThreeStationSceneStartDTO(null, 180.0D, false);
        long seed = safe.seed() != null ? safe.seed() : System.nanoTime();
        String sid = "ed_sim_" + seq.incrementAndGet();
        ThreeStationFloodTrainWorldContext ctx = new ThreeStationFloodTrainWorldContext(
                sid, System.currentTimeMillis(), 10_000L, seed, safe.effectiveRainfallMmPerHour(), operatorService);
        ctx.bindLoopThread(Thread.currentThread());
        try {
            ctx.initializeOnLoop();
        } finally {
            ctx.bindLoopThread(null);
        }
        sessions.put(sid, ctx);
        return sid;
    }

    @Override
    public ThreeStationSceneSnapshotVO stepSession(String sessionId, int steps) {
        ThreeStationFloodTrainWorldContext ctx = requireSession(sessionId);
        int n = Math.max(1, Math.min(steps, 10_000));
        ctx.bindLoopThread(Thread.currentThread());
        try {
            ctx.stepExactly(n);
            for (int i = 0; i < n; i++) {
                if (!ctx.stepOneTickOnLoop()) {
                    break;
                }
            }
            return ctx.toSceneVO();
        } finally {
            ctx.bindLoopThread(null);
        }
    }

    @Override
    public ThreeStationSceneSnapshotVO getSnapshot(String sessionId) {
        return requireSession(sessionId).toSceneVO();
    }

    @Override
    public void stopSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private ThreeStationFloodTrainWorldContext requireSession(String sid) {
        ThreeStationFloodTrainWorldContext ctx = sessions.get(sid);
        if (ctx == null) {
            throw new IllegalArgumentException("SESSION_NOT_FOUND: " + sid);
        }
        return ctx;
    }
}
