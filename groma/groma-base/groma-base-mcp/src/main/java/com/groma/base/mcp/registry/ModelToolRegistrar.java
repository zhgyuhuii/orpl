package com.groma.base.mcp.registry;

import com.groma.base.mcp.config.GromaMcpProperties;
import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.manifest.ManifestSchemaValidator;
import com.groma.base.mcp.observability.GromaMcpMetricsRecorder;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.protocol.McpToolEnvelope;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 编译期模型工具清单动态注册与热更新通知器（落实 03-mcp-analysis.md 第 6 节 & NP-MCP-003）
 */
public final class ModelToolRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ModelToolRegistrar.class);

    private final McpSyncServer mcpSyncServer;
    private final GromaMcpEnginePort enginePort;
    private final GromaMcpProperties properties;
    private final GromaMcpMetricsRecorder metrics;
    private final ObjectMapper jsonMapper = JsonMapper.builder().build();
    private final ExecutorService registrationExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<String, List<String>> registeredToolsByModel = new ConcurrentHashMap<>();

    public ModelToolRegistrar(
            McpSyncServer mcpSyncServer,
            GromaMcpEnginePort enginePort,
            GromaMcpProperties properties,
            GromaMcpMetricsRecorder metrics) {
        this.mcpSyncServer = mcpSyncServer;
        this.enginePort = enginePort;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 异步挂载模型工具清单（不占用 Web 请求线程，CK-NP-MCP-003 建设标准第 4 条）
     */
    public CompletableFuture<List<String>> onModelMountedAsync(GromaToolManifest manifest) {
        ManifestSchemaValidator.validateManifest(manifest);
        return CompletableFuture.supplyAsync(() -> mountInternal(manifest), registrationExecutor);
    }

    /**
     * 同步挂载入口（供启动初始化或契约测试精确断言使用）
     */
    public List<String> onModelMountedSync(GromaToolManifest manifest) {
        ManifestSchemaValidator.validateManifest(manifest);
        return mountInternal(manifest);
    }

    private List<String> mountInternal(GromaToolManifest manifest) {
        // 若此前已挂载过同名模型的旧版本，先移除旧工具避免残留
        removeExistingToolsSilently(manifest.modelCode());

        if (!properties.isPerProcessToolEnabled() || manifest.processes() == null || manifest.processes().isEmpty()) {
            registeredToolsByModel.put(manifest.modelCode(), List.of());
            return List.of();
        }

        String normalizedPrefix = manifest.modelCode().replace('-', '_').toLowerCase();
        List<String> addedNames = new ArrayList<>();

        for (GromaToolManifest.ProcessToolDef proc : manifest.processes()) {
            String toolName = properties.isPrefixToolWithModelCode()
                    ? normalizedPrefix + "__run_" + proc.ref()
                    : "run_" + proc.ref();

            McpSchema.Tool toolDef = McpSchema.Tool.builder()
                    .name(toolName)
                    .description("[" + manifest.modelLabel() + " v" + manifest.modelVersion() + "] " + proc.description())
                    .inputSchema(proc.inputSchema())
                    .build();

            McpServerFeatures.SyncToolSpecification spec = McpServerFeatures.SyncToolSpecification.builder()
                    .tool(toolDef)
                    .callHandler((exchange, callReq) -> handleDynamicToolCall(
                            toolName, manifest.modelCode(), proc, callReq != null ? callReq.arguments() : Map.of()))
                    .build();

            if (mcpSyncServer != null) {
                mcpSyncServer.addTool(spec);
            }
            addedNames.add(toolName);
        }

        List<String> immutableNames = List.copyOf(addedNames);
        registeredToolsByModel.put(manifest.modelCode(), immutableNames);

        if (mcpSyncServer != null && !immutableNames.isEmpty()) {
            mcpSyncServer.notifyToolsListChanged();
        }
        log.info("[MCP-Registrar] 模型 {}@{} 动态注册了 {} 个过程工具并已广播变更通知",
                manifest.modelCode(), manifest.modelVersion(), immutableNames.size());
        return immutableNames;
    }

    /**
     * 模型卸载时移除其全部动态工具并广播 tools/list_changed（落实 CK-NP-MCP-003-1 & 2）
     */
    public List<String> onModelUnmounted(String modelCode) {
        List<String> removed = removeExistingToolsSilently(modelCode);
        if (mcpSyncServer != null && !removed.isEmpty()) {
            mcpSyncServer.notifyToolsListChanged();
        }
        log.info("[MCP-Registrar] 模型 {} 已卸载，移除了 {} 个动态工具", modelCode, removed.size());
        return removed;
    }

    public Set<String> listRegisteredToolNamesForModel(String modelCode) {
        List<String> list = registeredToolsByModel.get(modelCode);
        return list == null ? Collections.emptySet() : Set.copyOf(list);
    }

    private List<String> removeExistingToolsSilently(String modelCode) {
        List<String> oldTools = registeredToolsByModel.remove(modelCode);
        if (oldTools == null || oldTools.isEmpty()) {
            return List.of();
        }
        if (mcpSyncServer != null) {
            for (String tName : oldTools) {
                try {
                    mcpSyncServer.removeTool(tName);
                } catch (Exception ex) {
                    log.debug("移除动态工具 {} 忽略异常: {}", tName, ex.getMessage());
                }
            }
        }
        return oldTools;
    }

    private McpSchema.CallToolResult handleDynamicToolCall(
            String toolName,
            String modelCode,
            GromaToolManifest.ProcessToolDef procDef,
            Map<String, Object> rawArgs) {

        McpToolEnvelope<GromaMcpEnginePort.ProcessExecutionOutcome> env = metrics.executeTracked(
                toolName,
                modelCode,
                () -> {
                    Map<String, Object> safeArgs = rawArgs == null ? Map.of() : rawArgs;
                    ManifestSchemaValidator.validateArgumentsAgainstSchema(procDef.inputSchema(), safeArgs);
                    return enginePort.executeProcessStateless(
                            modelCode, procDef.ref(), safeArgs, properties.getStatelessProcessTimeoutMs());
                }
        );

        String serialized = jsonMapper.writeValueAsString(env);
        return McpSchema.CallToolResult.builder()
                .addTextContent(serialized)
                .isError(env.isError())
                .build();
    }
}
