package com.groma.bootstrap.pipeline;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.runtime.GromaSessionManager;
import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.registry.ModelToolRegistrar;
import com.groma.studio.compiler.VersionPinnedHotSwapManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局模型注册中心（设计书第一章 bootstrap/registry 规划，T3 落地）：
 * 统一"合包（ServiceLoader）+ 热挂载（版本钉住租约）"两条模型来源的解析入口，消除
 * {@code ModelPipelineService.hotMounted} / {@code GromaMcpEnginePortImpl.hotRuntimes} /
 * {@code SessionArchiveService.resolveRuntimeOf} 三处各自为政的注册表。
 * <p>
 * 关键语义（修复原实现的两个正确性缺陷）：
 * <ol>
 *   <li>会话级租约持有：热挂载模型的 {@code SessionModelLease} 由本中心持有到会话停止
 *       （经 {@link GromaSessionManager.SessionLifecycleListener} 回调释放），
 *       热更新挂新版本 retire 旧版本时旧类加载器因引用计数 &gt; 0 而保活，运行中会话不被提前回收；</li>
 *   <li>新会话始终解析最新版本：不再缓存 modelCode → runtime 映射，热更后新会话立即用上新版本，
 *       并回调 {@link ModelToolRegistrar#onModelMountedAsync} 刷新 MCP 动态工具清单。</li>
 * </ol>
 */
@Service
public class GromaModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(GromaModelRegistry.class);

    private final GromaSessionManager sessions;
    private final VersionPinnedHotSwapManager hotSwap;
    private final ObjectProvider<ModelToolRegistrar> registrarProvider;
    private final ObjectMapper mapper = new ObjectMapper();
    /** 会话 id → 热挂载模型租约（宿主生命周期内由会话持有，停止时释放） */
    private final ConcurrentHashMap<String, VersionPinnedHotSwapManager.SessionModelLease> sessionLeases =
            new ConcurrentHashMap<>();

    public GromaModelRegistry(GromaSessionManager sessions,
                              VersionPinnedHotSwapManager hotSwap,
                              ObjectProvider<ModelToolRegistrar> registrarProvider) {
        this.sessions = sessions;
        this.hotSwap = hotSwap;
        this.registrarProvider = registrarProvider;
        this.sessions.addLifecycleListener(this::releaseSessionLease);
    }

    /** 已挂载模型视图 */
    public record MountedModel(GromaModuleRuntime runtime, String mountKind) {
    }

    // ---------------- 模型解析 ----------------

    /** 全量已挂载模型：合包在前、热挂载最新修订在后（同 code 以热挂载为准） */
    public List<MountedModel> listMounted() {
        List<MountedModel> out = new ArrayList<>();
        for (GromaModuleRuntime runtime : ServiceLoader.load(GromaModuleRuntime.class,
                GromaModelRegistry.class.getClassLoader())) {
            out.add(new MountedModel(runtime, "bundled"));
        }
        for (VersionPinnedHotSwapManager.MountedModelRevision revision : hotSwap.latestRevisions()) {
            GromaModuleRuntime runtime = discoverRuntime(revision.classLoaderOrNull(), revision.modelCode());
            if (runtime != null) {
                out.removeIf(m -> m.runtime().modelCode().equals(runtime.modelCode()));
                out.add(new MountedModel(runtime, "hotswap@" + revision.modelVersion()));
            }
        }
        return out;
    }

    public Optional<MountedModel> find(String modelCode, String expectedVersionOrNull) {
        return listMounted().stream()
                .filter(m -> m.runtime().modelCode().equals(modelCode))
                .filter(m -> expectedVersionOrNull == null || m.runtime().modelVersion().equals(expectedVersionOrNull))
                .findFirst();
    }

    /**
     * 为新会话解析模型并创建会话：合包模型直接启动；热挂载模型申请租约并持有至会话停止。
     * sceneCode 为空时取模型首个场景（无状态过程执行的默认语义）。返回新会话 id。
     */
    public String startSession(String modelCode, String sceneCode, Map<String, Object> initialOverrides,
                               Long seed, boolean startPaused) {
        GromaModuleRuntime bundled = findBundled(modelCode);
        if (bundled != null) {
            return sessions.startSession(bundled, defaultSceneOf(bundled, sceneCode), initialOverrides, seed, startPaused, null);
        }
        VersionPinnedHotSwapManager.SessionModelLease lease = hotSwap.acquireLatestSessionLease(modelCode);
        try {
            GromaModuleRuntime runtime = discoverRuntime(lease.loader(), modelCode);
            if (runtime == null) {
                throw new IllegalStateException("MODEL_NOT_FOUND: 模型 " + modelCode + " 的热挂载 services 登记不可读");
            }
            String sessionId = sessions.startSession(runtime, defaultSceneOf(runtime, sceneCode),
                    initialOverrides, seed, startPaused, null);
            sessionLeases.put(sessionId, lease);
            return sessionId;
        } catch (RuntimeException ex) {
            // 会话创建失败（场景不存在 / 并发超限等）时立即归还租约，避免引用计数泄漏
            lease.close();
            throw ex;
        }
    }

    private String defaultSceneOf(GromaModuleRuntime runtime, String sceneCode) {
        if (sceneCode != null && !sceneCode.isBlank()) {
            return sceneCode;
        }
        return runtime.scenes().isEmpty() ? null : runtime.scenes().get(0).sceneCode();
    }

    /** 仅读取解析（存档恢复等需要运行时实例但不经 startSession 的场景一律改走 startSession） */
    public Optional<GromaModuleRuntime> peekRuntime(String modelCode) {
        return find(modelCode, null).map(MountedModel::runtime);
    }

    // ---------------- 热挂载回调 ----------------

    /**
     * 模型构建流水线热挂载完成后调用：无注册中心缓存需要失效（每次会话解析均取最新），
     * 负责读取新修订的 MCP 工具清单并异步刷新动态工具（旧版本工具先卸载再注册，见 ModelToolRegistrar）。
     */
    public void onModelMounted(String modelCode) {
        ModelToolRegistrar registrar = registrarProvider.getIfAvailable();
        if (registrar == null) {
            return;
        }
        for (VersionPinnedHotSwapManager.MountedModelRevision revision : hotSwap.latestRevisions()) {
            if (revision.modelCode().equals(modelCode)) {
                GromaToolManifest manifest = readManifest(revision.classLoaderOrNull(), modelCode);
                if (manifest != null) {
                    registrar.onModelMountedAsync(manifest);
                    LOG.info("[Registry] 模型 {}@{} 已触发 MCP 动态工具异步刷新", modelCode, revision.modelVersion());
                }
                return;
            }
        }
    }

    // ---------------- 内部 ----------------

    private void releaseSessionLease(String sessionId) {
        VersionPinnedHotSwapManager.SessionModelLease lease = sessionLeases.remove(sessionId);
        if (lease != null) {
            lease.close();
            LOG.debug("[Registry] 会话 {} 已释放模型 {}@{} 租约", sessionId, lease.modelCode(), lease.modelVersion());
        }
    }

    private GromaModuleRuntime findBundled(String modelCode) {
        for (GromaModuleRuntime runtime : ServiceLoader.load(GromaModuleRuntime.class,
                GromaModelRegistry.class.getClassLoader())) {
            if (runtime.modelCode().equals(modelCode)) {
                return runtime;
            }
        }
        return null;
    }

    /** 从隔离类加载器发现运行时入口（ServiceLoader 不可用时兜底直读 services 登记文件实例化） */
    private GromaModuleRuntime discoverRuntime(ClassLoader loader, String modelCode) {
        if (loader == null) {
            return null;
        }
        var discovered = ServiceLoader.load(GromaModuleRuntime.class, loader).iterator();
        while (discovered.hasNext()) {
            GromaModuleRuntime runtime = discovered.next();
            if (runtime.modelCode().equals(modelCode)) {
                return runtime;
            }
        }
        java.net.URL servicesFile = loader.getResource("META-INF/services/com.groma.base.engine.GromaModuleRuntime");
        if (servicesFile == null) {
            return null;
        }
        try (InputStream in = servicesFile.openStream()) {
            String fqcn = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .findFirst().orElse("");
            if (fqcn.isBlank()) {
                return null;
            }
            return (GromaModuleRuntime) loader.loadClass(fqcn).getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            LOG.warn("[Registry] 模型 {} services 兜底实例化失败: {}", modelCode, ex.getMessage());
            return null;
        }
    }

    private GromaToolManifest readManifest(ClassLoader loader, String modelCode) {
        if (loader == null) {
            return null;
        }
        try (InputStream in = loader.getResourceAsStream("META-INF/groma/mcp/manifest.json")) {
            if (in == null) {
                return null;
            }
            return mapper.readValue(new String(in.readAllBytes(), StandardCharsets.UTF_8), GromaToolManifest.class);
        } catch (Exception ex) {
            LOG.warn("[Registry] 模型 {} 工具清单读取失败: {}", modelCode, ex.getMessage());
            return null;
        }
    }
}
