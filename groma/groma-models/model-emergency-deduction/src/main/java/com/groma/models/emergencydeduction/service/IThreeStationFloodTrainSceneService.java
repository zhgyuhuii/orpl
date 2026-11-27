package com.groma.models.emergencydeduction.service;

import com.groma.models.emergencydeduction.model.dto.ThreeStationSceneStartDTO;
import com.groma.models.emergencydeduction.model.vo.ThreeStationSceneSnapshotVO;

public interface IThreeStationFloodTrainSceneService {
    String startSession(ThreeStationSceneStartDTO dto);
    ThreeStationSceneSnapshotVO stepSession(String sessionId, int steps);
    ThreeStationSceneSnapshotVO getSnapshot(String sessionId);
    void stopSession(String sessionId);
}
