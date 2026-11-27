package com.groma.bootstrap.pipeline;

import com.groma.base.engine.archive.ArchiveCheckpointCoordinator;
import com.groma.base.engine.archive.SimulationArchiveBundle;
import com.groma.base.engine.runtime.GromaSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 会话租约第三级（阶段 2-D2）：并发超限时把最久未访问的冷会话**先存档再回收**，
 * 存档落盘后可按需经 REST 重新载入续跑（与手动 maxSteps / 空闲回收两级并存）。
 */
@Service
public class SessionArchiveService {
    private static final Logger LOG = LoggerFactory.getLogger(SessionArchiveService.class);

    private final GromaSessionManager sessions;
    private final GromaModelRegistry registry;
    private final ArchiveCheckpointCoordinator coordinator = new ArchiveCheckpointCoordinator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path archiveRoot;

    public SessionArchiveService(GromaSessionManager sessions,
                                 GromaModelRegistry registry,
                                 @Value("${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        this.sessions = sessions;
        this.registry = registry;
        this.archiveRoot = Path.of(workspaceRoot, "archives");
    }

    /** 存档元数据（供 REST 列表） */
    public record ArchiveEntry(String file, String sessionId, long tickIndex, long savedAtEpochMs) {}

    /**
     * 把指定会话存档落盘并停掉会话（冷会话回收），返回存档文件路径
     */
    public String archiveAndEvict(String sessionId) {
        var handle = sessions.require(sessionId);
        SimulationArchiveBundle bundle = coordinator.createCheckpointAtStepBoundary(handle.session());
        try {
            Files.createDirectories(archiveRoot);
            Path file = archiveRoot.resolve(sessionId + ".json");
            Files.writeString(file, mapper.writeValueAsString(bundle), StandardCharsets.UTF_8);
            sessions.stopSession(sessionId);
            LOG.info("[Archive] 会话 {} 已存档至 {}（tick={}）", sessionId, file, bundle.tickIndex());
            return file.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException("会话存档落盘失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 载入落盘存档：以存档内的模型/场景重新开桌并恢复到存档拍，返回新会话 id。
     * 模型解析统一走 {@link GromaModelRegistry}（热挂载模型同样可恢复）。
     */
    public String restoreArchived(Path archiveFile, long maxStepsOnWallClockMs) {
        try {
            SimulationArchiveBundle bundle = mapper.readValue(Files.readString(archiveFile, StandardCharsets.UTF_8),
                    SimulationArchiveBundle.class);
            var header = bundle.taskPart();
            String sessionId = registry.startSession(header.modelCode(), header.sceneCode(), java.util.Map.of(),
                    header.rootSeed(), true);
            sessions.require(sessionId).session().restoreFromArchive(bundle);
            return sessionId;
        } catch (IOException ex) {
            throw new UncheckedIOException("存档读取失败: " + ex.getMessage(), ex);
        }
    }

    /** 全部落盘存档列表 */
    public List<ArchiveEntry> listArchives() {
        List<ArchiveEntry> out = new ArrayList<>();
        if (!Files.isDirectory(archiveRoot)) {
            return out;
        }
        try (var files = Files.list(archiveRoot)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try {
                    SimulationArchiveBundle bundle = mapper.readValue(
                            Files.readString(file, StandardCharsets.UTF_8), SimulationArchiveBundle.class);
                    out.add(new ArchiveEntry(file.toString(), bundle.taskPart().modelCode(),
                            bundle.tickIndex(), Files.getLastModifiedTime(file).toMillis()));
                } catch (IOException corrupt) {
                    out.add(new ArchiveEntry(file.toString(), file.getFileName().toString(), -1L,
                            Files.getLastModifiedTime(file).toMillis()));
                }
            }
        } catch (IOException ex) {
            LOG.warn("[Archive] 存档目录读取失败: {}", ex.getMessage());
        }
        return out;
    }

    /**
     * 并发超限时的第三级回收：按最近访问时间挑最冷会话存档回收（供建会话前调用）。
     * 驱逐优先级：最后访问时间最早者优先；同访问时间下非 RUNNING（已暂停/空闲）优先。
     * 修复原实现按 tickIndex 升序挑选导致"刚创建的活跃会话被当最冷会话驱逐"的逻辑反转缺陷。
     *
     * @return 被回收的会话 id；无可回收会话返回 null
     */
    public String evictColdestIfOverCapacity(int maxConcurrentSessions) {
        var summaries = sessions.listSessions(null);
        if (summaries.size() < maxConcurrentSessions) {
            return null;
        }
        String coldest = summaries.stream()
                .min(Comparator.comparingLong(GromaSessionManager.SessionSummary::lastAccessEpochMs)
                        .thenComparing(s -> s.status().equals("RUNNING") ? 1L : 0L))
                .map(GromaSessionManager.SessionSummary::sessionId)
                .orElse(null);
        if (coldest == null) {
            return null;
        }
        archiveAndEvict(coldest);
        return coldest;
    }
}
