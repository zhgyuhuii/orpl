package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 落盘存档管理 REST（会话租约第三级）：列表 / 按需恢复。
 */
@RestController
@RequestMapping("/api/v1/studio/archives")
public class SessionArchiveController {

    private final SessionArchiveService archives;

    public SessionArchiveController(SessionArchiveService archives) {
        this.archives = archives;
    }

    @GetMapping
    public Result<List<SessionArchiveService.ArchiveEntry>> list() {
        return Result.ok(archives.listArchives());
    }

    /** 把在线会话存档落盘并回收（冷会话回收第三级） */
    @PostMapping("/sessions/{sessionId}/evict")
    public Result<Map<String, Object>> evict(@PathVariable String sessionId) {
        String file = archives.archiveAndEvict(sessionId);
        return Result.ok(Map.of("archivedFile", file, "evicted", sessionId));
    }

    /** 从落盘存档恢复为新会话（恢复到存档拍，可继续推步） */
    @PostMapping("/{archiveFileName}/restore")
    public Result<Map<String, Object>> restore(@PathVariable String archiveFileName) {
        Path file = Path.of(archiveFileName);
        if (!Files.isRegularFile(file)) {
            return Result.fail("ARCHIVE_NOT_FOUND", "存档不存在: " + archiveFileName, null);
        }
        String sessionId = archives.restoreArchived(file, 0L);
        return Result.ok(Map.of("sessionId", sessionId));
    }
}
