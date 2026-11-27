package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取列车通行，编译期禁止误调写 Setter
 */
public interface TrainPassingReadOnly {
    String getInstanceId();

    /**
     * 通行列车
     */
    List<TrainEntity> getPassingTrain();

    /**
     * 通行区间
     */
    MetroSectionEntity getPassingSection();
}
