package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.TrainAdvanceAlongSectionProcessTask;
import com.groma.models.emergencydeduction.engine.process.TrainDepartProcessTask;
import com.groma.models.emergencydeduction.engine.process.TrainEnterStationProcessTask;
import com.groma.models.emergencydeduction.engine.process.TrainSectionRunProcessTask;
import com.groma.models.emergencydeduction.engine.process.TrainStoppedAtStationProcessTask;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import com.groma.models.emergencydeduction.model.entity.api.MetroSectionReadOnly;
import com.groma.models.emergencydeduction.model.entity.api.StationReadOnly;
import com.groma.models.emergencydeduction.service.ITrainService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 列车 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class TrainServiceImpl implements ITrainService {
    @Override
    public boolean advanceAlongSection(GromaWorldContext ctx, TrainEntity self,
            MetroSectionReadOnly metroSection, StationReadOnly targetStation, double travelInterval,
            double travelledDistance, boolean alongIncreasingMileage, double currentLineMileage) {
        // @GromaUserCode-START ["7rSlUlS0cSWjxGaiErg"]
        // 过程「沿区间前进」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["7rSlUlS0cSWjxGaiErg"]

        return new TrainAdvanceAlongSectionProcessTask(ctx, self, metroSection, targetStation, travelInterval, travelledDistance, alongIncreasingMileage, currentLineMileage).startTopLevel();
    }

    @Override
    public boolean depart(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain) {
        // @GromaUserCode-START ["wjMiAj779lB5502MLmW"]
        // 过程「离站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["wjMiAj779lB5502MLmW"]

        return new TrainDepartProcessTask(ctx, self, stationWithParkedTrain).startTopLevel();
    }

    @Override
    public GromaProcessTask sectionRun(GromaWorldContext ctx, TrainEntity self,
            SectionWithTrainEntity sectionWithTrain, double currentLineMileage,
            boolean alongIncreasingMileage) {
        // @GromaUserCode-START ["u9xyHBv1BSldBPVP0Q7"]
        // 过程「区间行驶」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["u9xyHBv1BSldBPVP0Q7"]

        GromaProcessTask task = new TrainSectionRunProcessTask(ctx, self, sectionWithTrain, currentLineMileage, alongIncreasingMileage);
        ctx.launch(task);
        return task;
    }

    @Override
    public GromaProcessTask stoppedAtStation(GromaWorldContext ctx, TrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain) {
        // @GromaUserCode-START ["1rbne3IC1GpVoTNMwVu"]
        // 过程「停站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["1rbne3IC1GpVoTNMwVu"]

        GromaProcessTask task = new TrainStoppedAtStationProcessTask(ctx, self, stationWithParkedTrain);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean enterStation(GromaWorldContext ctx, TrainEntity self) {
        // @GromaUserCode-START ["zkevt3isbPbfdXv9T9I"]
        // 过程「进站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["zkevt3isbPbfdXv9T9I"]

        return new TrainEnterStationProcessTask(ctx, self).startTopLevel();
    }
}
