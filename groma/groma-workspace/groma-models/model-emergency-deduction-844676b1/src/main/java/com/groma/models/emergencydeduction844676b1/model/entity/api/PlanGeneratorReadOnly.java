package com.groma.models.emergencydeduction844676b1.model.entity.api;

import com.groma.models.emergencydeduction844676b1.model.entity.AreaEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.DisposalPlanEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.StationEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.TrainEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取方案生成器，编译期禁止误调写 Setter
 */
public interface PlanGeneratorReadOnly {
    String getInstanceId();

    /**
     * 备选方案列表
     */
    List<DisposalPlanEntity> getCandidatePlanList();

    /**
     * 降雨
     */
    RainfallEntity getRainfall();

    /**
     * 地面积水区
     */
    AreaEntity getSurfaceWaterloggingArea();

    /**
     * 挡水墙
     */
    StationEntity getFloodWall();

    /**
     * 地铁区间
     */
    MetroSectionEntity getMetroSection();

    /**
     * 列车
     */
    TrainEntity getTrain();
}
