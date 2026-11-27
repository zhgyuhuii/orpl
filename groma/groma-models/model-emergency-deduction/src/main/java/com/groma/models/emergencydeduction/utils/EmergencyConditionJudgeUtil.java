package com.groma.models.emergencydeduction.utils;

import com.groma.models.emergencydeduction.model.enums.WarningLevelEnum;

/**
 * 应急推演线上逻辑判断原生比较工具
 */
public final class EmergencyConditionJudgeUtil {

    private EmergencyConditionJudgeUtil() {
    }

    public static boolean shouldFloodWallFail(double areaSurfaceElevationM, double wallTopElevationM) {
        return areaSurfaceElevationM > wallTopElevationM + 0.15D;
    }

    public static boolean shouldForceStopTrain(double sectionWaterDepthM, double thresholdM) {
        return sectionWaterDepthM >= thresholdM;
    }

    public static WarningLevelEnum evaluateWarningLevel(double maxSectionWaterDepthM) {
        if (maxSectionWaterDepthM >= 0.35D) {
            return WarningLevelEnum.RED;
        }
        if (maxSectionWaterDepthM >= 0.15D) {
            return WarningLevelEnum.ORANGE;
        }
        if (maxSectionWaterDepthM >= 0.05D) {
            return WarningLevelEnum.YELLOW;
        }
        return WarningLevelEnum.BLUE;
    }
}
