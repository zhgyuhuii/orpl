package com.groma.models.emergencydeduction844676b1.service.impl;

import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeFileExistsProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeFileInitProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeIsEmptyFileProcessTask;
import com.groma.models.emergencydeduction844676b1.engine.process.FileTypeReadTextContentProcessTask;
import com.groma.models.emergencydeduction844676b1.model.entity.FileTypeEntity;
import com.groma.models.emergencydeduction844676b1.service.IFileTypeService;
import java.lang.Override;
import java.lang.String;
import org.springframework.stereotype.Service;

/**
 * 文件类型 领域服务实现：方法体由方圆模型编译生成，@GromaUserCode 保护区内的二次开发代码在重生成时无损保留
 */
@Service
public class FileTypeServiceImpl implements IFileTypeService {
    @Override
    public boolean fileExists(GromaWorldContext ctx, FileTypeEntity self, boolean exists) {
        // @GromaUserCode-START ["ae6ce890734549a29aa10b5b358bab99"]
        // 过程「判断文件是否存在」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["ae6ce890734549a29aa10b5b358bab99"]

        return new FileTypeFileExistsProcessTask(ctx, self, exists).startTopLevel();
    }

    @Override
    public boolean isEmptyFile(GromaWorldContext ctx, FileTypeEntity self, boolean empty) {
        // @GromaUserCode-START ["4da6a71b5a2943d887fd9249ef48d05c"]
        // 过程「判断是否为空文件」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["4da6a71b5a2943d887fd9249ef48d05c"]

        return new FileTypeIsEmptyFileProcessTask(ctx, self, empty).startTopLevel();
    }

    @Override
    public boolean fileInit(GromaWorldContext ctx, FileTypeEntity self) {
        // @GromaUserCode-START ["bf6717304c85425eb4e697d24ab6d95a"]
        // 过程「文件初始化」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["bf6717304c85425eb4e697d24ab6d95a"]

        return new FileTypeFileInitProcessTask(ctx, self).startTopLevel();
    }

    @Override
    public boolean readTextContent(GromaWorldContext ctx, FileTypeEntity self, String fileContent) {
        // @GromaUserCode-START ["615b216a7eba490a90e986eeae52eea9"]
        // 过程「读取文本内容」的二次开发保护区：在此编写的代码在模型重新生成源码时自动无损保留
        // @GromaUserCode-END ["615b216a7eba490a90e986eeae52eea9"]

        return new FileTypeReadTextContentProcessTask(ctx, self, fileContent).startTopLevel();
    }
}
