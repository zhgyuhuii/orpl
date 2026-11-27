package com.groma.base.common.util;

/**
 * 确定性跨平台空间几何测距工具（全程使用 StrictMath 消除跨 CPU 架构浮点分叉）
 */
public final class GeoSpatialUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8D;

    private GeoSpatialUtils() {
    }

    public static double haversineMeters(double lon1Deg, double lat1Deg, double lon2Deg, double lat2Deg) {
        double lat1 = StrictMath.toRadians(lat1Deg);
        double lat2 = StrictMath.toRadians(lat2Deg);
        double dLat = StrictMath.toRadians(lat2Deg - lat1Deg);
        double dLon = StrictMath.toRadians(lon2Deg - lon1Deg);
        double a = StrictMath.sin(dLat * 0.5D) * StrictMath.sin(dLat * 0.5D)
                + StrictMath.cos(lat1) * StrictMath.cos(lat2)
                * StrictMath.sin(dLon * 0.5D) * StrictMath.sin(dLon * 0.5D);
        double c = 2.0D * StrictMath.atan2(StrictMath.sqrt(a), StrictMath.sqrt(1.0D - a));
        return EARTH_RADIUS_METERS * c;
    }
}
