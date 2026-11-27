package com.groma.models.emergencydeduction.model.entity.api;

import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import java.lang.String;
import java.util.List;

/**
 * 只读输入视图契约（对应 R16 / R17 单向输入影响）：过程体内只能通过本接口读取挡水防护，编译期禁止误调写 Setter
 */
public interface FloodProtectionReadOnly {
    String getInstanceId();

    /**
     * 防护挡水墙
     */
    List<StationEntity> getProtectiveFloodWall();

    /**
     * 防护区间
     */
    MetroSectionEntity getProtectedSection();
}
