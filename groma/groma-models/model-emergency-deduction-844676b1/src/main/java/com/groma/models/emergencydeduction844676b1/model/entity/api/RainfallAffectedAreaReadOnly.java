package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.String;
import java.util.Map;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取降雨与受影响区域，编译期禁止误调写 Setter
 */
public interface RainfallAffectedAreaReadOnly {
    String getInstanceId();

    /**
     * 降雨
     */
    RainfallEntity getRainfall();

    /**
     * 区域
     */
    AreaEntity getArea();

    /**
     * 当前汇水流量
     */
    double getCurrentCatchmentFlow();

    /**
     * 本步进汇水量
     */
    double getStepCatchmentVolume();

    /**
     * 当前雨强
     */
    double getCurrentRainIntensity();

    /**
     * 累计雨量
     */
    double getCumulativeRainfall();

    /**
     * 雨量序列
     */
    Map<Long, Double> getRainfallSeries();
}
