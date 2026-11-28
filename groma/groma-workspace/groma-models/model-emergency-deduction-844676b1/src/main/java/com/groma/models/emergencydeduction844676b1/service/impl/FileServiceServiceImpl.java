package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceFileExistsProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceFileInitProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceIsFileEmptyProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceReadFileLineByLineProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileServiceReadTextContentProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction844676b1.model.entity.FileServiceEntity;
import com.groma.models.emergencydeduction844676b1.service.IFileServiceService;
import java.lang.Override;
import java.lang.String;
import org.springframework.stereotype.Service;

/**
 * 文件服务 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class FileServiceServiceImpl implements IFileServiceService {
    @Override
    public boolean readTextContent(GromaWorldContext ctx, FileServiceEntity self,
            String pathIdentifier, boolean succeeded, String fileContent) {
        // @GromaUserCode-START ["3UULlXsUgqgKXOiinlG"]
        // 过程「读取文本内容」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["3UULlXsUgqgKXOiinlG"]

        return new FileServiceReadTextContentProcessTask(ctx, self, pathIdentifier, succeeded, fileContent).startTopLevel();
    }

    @Override
    public boolean isFileEmpty(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean succeeded, boolean empty) {
        // @GromaUserCode-START ["IeK32FL7HMYxvSd4PnP"]
        // 过程「判断文件是否为空」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["IeK32FL7HMYxvSd4PnP"]

        return new FileServiceIsFileEmptyProcessTask(ctx, self, pathIdentifier, succeeded, empty).startTopLevel();
    }

    @Override
    public boolean fileInit(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean succeeded, FileInfoEntity fileInfo, String errorMessage) {
        // @GromaUserCode-START ["Ne04mUfbnKpBEjTlARK"]
        // 过程「文件初始化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["Ne04mUfbnKpBEjTlARK"]

        return new FileServiceFileInitProcessTask(ctx, self, pathIdentifier, succeeded, fileInfo, errorMessage).startTopLevel();
    }

    @Override
    public GromaProcessTask readFileLineByLine(GromaWorldContext ctx, FileServiceEntity self,
            String pathIdentifier, long batchNo, String fileContent) {
        // @GromaUserCode-START ["R4yssk5cauVoZXGtqkM"]
        // 过程「逐行读取文件内容」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["R4yssk5cauVoZXGtqkM"]

        GromaProcessTask task = new FileServiceReadFileLineByLineProcessTask(ctx, self, pathIdentifier, batchNo, fileContent);
        ctx.launch(task);
        return task;
    }

    @Override
    public boolean fileExists(GromaWorldContext ctx, FileServiceEntity self, String pathIdentifier,
            boolean exists, boolean succeeded) {
        // @GromaUserCode-START ["rxdnGDCWlbpOGBwd3RG"]
        // 过程「判断文件是否存在」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["rxdnGDCWlbpOGBwd3RG"]

        return new FileServiceFileExistsProcessTask(ctx, self, pathIdentifier, exists, succeeded).startTopLevel();
    }
}
