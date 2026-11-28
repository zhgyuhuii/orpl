package com.groma.models.smartplanning.service.impl;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.models.smartplanning.model.dto.SyncStrikeForceInputDTO;
import com.groma.models.smartplanning.model.vo.RoutePlanItemVO;
import com.groma.models.smartplanning.service.IRoutePlanningSyncArrivalService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoutePlanningSyncArrivalServiceImpl implements IRoutePlanningSyncArrivalService {

    @Override
    @GromaMeta(id = "b094d31f83d44a2886a714bd999e414e", zhName = "航路规划与同步到达算法", kind = "INDEPENDENT_OPERATOR")
    public List<RoutePlanItemVO> computeSynchronizedRoutes(long hHourSec, List<SyncStrikeForceInputDTO> forces) {
        if (forces == null || forces.isEmpty()) {
            return List.of();
        }
        List<RoutePlanItemVO> results = new ArrayList<>(forces.size());
        for (SyncStrikeForceInputDTO f : forces) {
            double distKm = planarCosineCorrectedDistKm(
                    f.baseLatDeg(), f.baseLonDeg(), f.targetLatDeg(), f.targetLonDeg());
            double speedKmh = Math.max(100.0D, f.cruiseSpeedKmh());
            long flySec = Math.max(1L, StrictMath.round((distKm / speedKmh) * 3600.0D));
            long takeoffSec = hHourSec - flySec;
            long etaSec = takeoffSec + flySec;
            results.add(new RoutePlanItemVO(
                    f.unitName(),
                    f.baseName(),
                    f.targetName(),
                    StrictMath.round(distKm * 100.0D) / 100.0D,
                    flySec,
                    takeoffSec,
                    etaSec,
                    etaSec - hHourSec
            ));
        }
        results.sort(Comparator.comparingLong(RoutePlanItemVO::computedTakeoffSec));
        return List.copyOf(results);
    }

    /**
     * 对齐原模型脚本 dist_km(a, b)：平面近似 + 经度按平均纬度余弦修正
     */
    private static double planarCosineCorrectedDistKm(double lat1, double lon1, double lat2, double lon2) {
        double dLatKm = (lat1 - lat2) * 111.0D;
        double meanLatRad = StrictMath.toRadians((lat1 + lat2) * 0.5D);
        double dLonKm = (lon1 - lon2) * 111.0D * StrictMath.cos(meanLatRad);
        return StrictMath.hypot(dLatKm, dLonKm);
    }
}
