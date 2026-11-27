package com.groma.models.emergencydeduction.service.impl;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.engine.process.LineWithTrainCreateRelationProcessTask;
import com.groma.models.emergencydeduction.engine.process.LineWithTrainFormSectionTrainRelationProcessTask;
import com.groma.models.emergencydeduction.engine.process.LineWithTrainFormStationTrainRelationProcessTask;
import com.groma.models.emergencydeduction.engine.process.LineWithTrainGenerateRelationProcessTask;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.service.ILineWithTrainService;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 线路与列车 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class LineWithTrainServiceImpl implements ILineWithTrainService {
    @Override
    public boolean formStationTrainRelation(GromaWorldContext ctx, LineWithTrainEntity self,
            StationWithParkedTrainEntity stationWithParkedTrain) {
        // @GromaUserCode-START ["eJ4DHDmPNpDiyA8MPzb"]
        // 过程「形成车站与列车的关联」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["eJ4DHDmPNpDiyA8MPzb"]

        return new LineWithTrainFormStationTrainRelationProcessTask(ctx, self, stationWithParkedTrain).startTopLevel();
    }

    @Override
    public boolean formSectionTrainRelation(GromaWorldContext ctx, LineWithTrainEntity self,
            SectionWithTrainEntity sectionWithTrain) {
        // @GromaUserCode-START ["Ls7rMbkit4rP36FTZiY"]
        // 过程「形成区间与列车的关联」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["Ls7rMbkit4rP36FTZiY"]

        return new LineWithTrainFormSectionTrainRelationProcessTask(ctx, self, sectionWithTrain).startTopLevel();
    }

    @Override
    public boolean createRelation(GromaWorldContext ctx, LineWithTrainEntity self) {
        // @GromaUserCode-START ["9gvX0IJx27OFUEQmGbX"]
        // 过程「创建关联」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["9gvX0IJx27OFUEQmGbX"]

        return new LineWithTrainCreateRelationProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean generateRelation(GromaWorldContext ctx, LineWithTrainEntity self) {
        // @GromaUserCode-START ["da4nTtpT9A3JrWQc21n"]
        // 过程「生成关联」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["da4nTtpT9A3JrWQc21n"]

        return new LineWithTrainGenerateRelationProcessTask(ctx, self).startTopLevel();
    }
}
