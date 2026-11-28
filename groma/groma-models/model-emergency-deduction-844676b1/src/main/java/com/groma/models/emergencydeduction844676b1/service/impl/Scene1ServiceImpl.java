package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene11ConvertToStopProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene12TurnNoIntoYesProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene13ConvertToRunProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene14CreateObjectTurnYesIntoNoProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene1Procedure1ProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.Scene1Procedure1ProcessTask2;
import com.groma.models.emergencydeduction844676b1.model.entity.Scene1Entity;
import com.groma.models.emergencydeduction844676b1.service.IScene1Service;
import java.lang.Override;
import org.springframework.stereotype.Service;

/**
 * 场景1 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class Scene1ServiceImpl implements IScene1Service {
    @Override
    public boolean turnNoIntoYes2(GromaWorldContext ctx, Scene1Entity self, boolean text1) {
        // @GromaUserCode-START ["ctET0FcFZxktbhbbd9j"]
        // 过程「2把否变成是」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["ctET0FcFZxktbhbbd9j"]

        return new Scene12TurnNoIntoYesProcessTask(ctx, self, text1).startTopLevel();
    }

    @Override
    public boolean createObjectTurnYesIntoNo4(GromaWorldContext ctx, Scene1Entity self,
            boolean text1) {
        // @GromaUserCode-START ["llOioPfUfaWKZf5nX3v"]
        // 过程「4生成对象并把是变成否」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["llOioPfUfaWKZf5nX3v"]

        return new Scene14CreateObjectTurnYesIntoNoProcessTask(ctx, self, text1).startTopLevel();
    }

    @Override
    public boolean convertToRun3(GromaWorldContext ctx, Scene1Entity self) {
        // @GromaUserCode-START ["ZBZhSMCGmdAeAUO90BY"]
        // 过程「3转换为行驶」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["ZBZhSMCGmdAeAUO90BY"]

        return new Scene13ConvertToRunProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public GromaProcessTask convertToStop1(GromaWorldContext ctx, Scene1Entity self) {
        // @GromaUserCode-START ["hzatYjpxW7rcjt42TK2"]
        // 过程「1转换为停站」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["hzatYjpxW7rcjt42TK2"]

        GromaProcessTask task = new Scene11ConvertToStopProcessTask(ctx, self);
        ctx.launch(task);
        return task;
    }

    @Override
    public GromaProcessTask procedure1(GromaWorldContext ctx, Scene1Entity self) {
        // @GromaUserCode-START ["QQMUU7DuY51tlC5lIlP"]
        // 过程「过程1」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["QQMUU7DuY51tlC5lIlP"]

        GromaProcessTask task = new Scene1Procedure1ProcessTask(ctx, self);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean procedure12(GromaWorldContext ctx, Scene1Entity self) {
        // @GromaUserCode-START ["nWFZ6HM9KiTV4Fj3PPz"]
        // 过程「过程1」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["nWFZ6HM9KiTV4Fj3PPz"]

        return new Scene1Procedure1ProcessTask2(ctx, self).startTopLevel();
    }
}
