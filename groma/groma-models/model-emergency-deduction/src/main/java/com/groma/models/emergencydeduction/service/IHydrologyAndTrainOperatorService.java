package com.groma.models.emergencydeduction.service;

import com.groma.base.engine.AbstractWorldSession;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;

public interface IHydrologyAndTrainOperatorService {
    void computeAreaRainfallRunoff(AbstractWorldSession ctx, AreaEntity area, double rainfallMmPerHour, double stepSeconds);
    void exchangeAreaAndStationWater(AbstractWorldSession ctx, AreaEntity area, StationEntity station, double stepSeconds);
    double computeConnectionFlowM3PerSec(double elevationA, double elevationB, double controlElev, double weirWidth);
    void exchangeStationAndSectionWater(AbstractWorldSession ctx, StationEntity station, MetroSectionEntity section, double stepSeconds);
}
