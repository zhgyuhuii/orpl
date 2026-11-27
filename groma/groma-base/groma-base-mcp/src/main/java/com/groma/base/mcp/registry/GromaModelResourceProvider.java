package com.groma.base.mcp.registry;

import com.groma.base.mcp.manifest.GromaToolManifest;
import com.groma.base.mcp.port.GromaMcpEnginePort;
import com.groma.base.mcp.protocol.GromaMcpException;
import com.groma.base.mcp.protocol.McpErrorCode;
import org.springframework.ai.mcp.annotation.McpResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 七类只读模型结构与双语语义资源提供器（落实 03-mcp-analysis.md 第 4.4 节 & NP-MCP-008，无需创建推演会话即可读取）
 */
public class GromaModelResourceProvider {

    private final GromaMcpEnginePort enginePort;
    private final ObjectMapper jsonMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public GromaModelResourceProvider(GromaMcpEnginePort enginePort) {
        this.enginePort = enginePort;
    }

    // 1. groma://models/{modelCode}/index -> 直接返回生成的 GROMA_MODEL_INDEX.md
    @McpResource(
            uri = "groma://models/{modelCode}/index",
            name = "Model Semantic Index",
            description = "模型全局中英文符号对照、场景拓扑与过程契约总索引（Markdown）",
            mimeType = "text/markdown"
    )
    public String readModelIndex(String modelCode) {
        return requireModel(modelCode).indexMarkdown();
    }

    // 2. groma://models/{modelCode}/types -> 全部对象类型清单
    @McpResource(
            uri = "groma://models/{modelCode}/types",
            name = "Model Object Types",
            description = "模型内全部领域对象类型概览清单",
            mimeType = "application/json"
    )
    public String readAllTypes(String modelCode) {
        return toJson(requireModel(modelCode).typesCatalog());
    }

    // 3. groma://models/{modelCode}/types/{typeCode} -> 单个对象类型的成员、状态、关联与自有过程
    @McpResource(
            uri = "groma://models/{modelCode}/types/{typeCode}",
            name = "Single Object Type Detail",
            description = "单个对象类型的属性成员、状态枚举、关联关系与过程详情",
            mimeType = "application/json"
    )
    public String readSingleType(String modelCode, String typeCode) {
        GromaMcpEnginePort.MountedModelMetadata meta = requireModel(modelCode);
        return meta.typesCatalog().stream()
                .filter(t -> typeCode.equalsIgnoreCase(String.valueOf(t.get("code")))
                        || typeCode.equals(String.valueOf(t.get("name"))))
                .findFirst()
                .map(this::toJson)
                .orElseThrow(() -> new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "未找到对象类型: " + typeCode));
    }

    // 4. groma://models/{modelCode}/processes -> 全部已发布过程摘要
    @McpResource(
            uri = "groma://models/{modelCode}/processes",
            name = "Model Processes",
            description = "模型清单中全部可调用过程列表",
            mimeType = "application/json"
    )
    public String readAllProcesses(String modelCode) {
        return toJson(requireModel(modelCode).toolManifest().processes());
    }

    // 5. groma://models/{modelCode}/processes/{processRef} -> 单个过程的完整语义与输入输出结构
    @McpResource(
            uri = "groma://models/{modelCode}/processes/{processRef}",
            name = "Single Process Contract",
            description = "单个过程的入参结构、出参结构、类别与前置条件完整定义",
            mimeType = "application/json"
    )
    public String readSingleProcess(String modelCode, String processRef) {
        GromaToolManifest manifest = requireModel(modelCode).toolManifest();
        return manifest.processes().stream()
                .filter(p -> processRef.equals(p.ref()) || processRef.equals(p.name()))
                .findFirst()
                .map(this::toJson)
                .orElseThrow(() -> new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "未找到过程: " + processRef));
    }

    // 6. groma://models/{modelCode}/scenes -> 全部权威场景清单
    @McpResource(
            uri = "groma://models/{modelCode}/scenes",
            name = "Model Scenes",
            description = "模型内全部可启动运行场景列表",
            mimeType = "application/json"
    )
    public String readAllScenes(String modelCode) {
        return toJson(requireModel(modelCode).scenesCatalog());
    }

    // 7. groma://models/{modelCode}/scenes/{sceneCode} -> 单个场景的实例构成与默认初值
    @McpResource(
            uri = "groma://models/{modelCode}/scenes/{sceneCode}",
            name = "Single Scene Detail",
            description = "单个场景的初始成员实例构成与默认启动参数",
            mimeType = "application/json"
    )
    public String readSingleScene(String modelCode, String sceneCode) {
        GromaMcpEnginePort.MountedModelMetadata meta = requireModel(modelCode);
        Map<String, Object> found = meta.scenesCatalog().stream()
                .filter(s -> sceneCode.equalsIgnoreCase(String.valueOf(s.get("code")))
                        || sceneCode.equals(String.valueOf(s.get("name"))))
                .findFirst()
                .orElseThrow(() -> new GromaMcpException(McpErrorCode.SCENE_NOT_FOUND, "未找到场景: " + sceneCode));
        return toJson(found);
    }

    private GromaMcpEnginePort.MountedModelMetadata requireModel(String modelCode) {
        return enginePort.findMountedModel(modelCode, null)
                .orElseThrow(() -> new GromaMcpException(McpErrorCode.MODEL_NOT_FOUND, "模型未挂载: " + modelCode));
    }

    private String toJson(Object obj) {
        return jsonMapper.writeValueAsString(obj);
    }
}
