package com.groma.base.mcp.manifest;

import java.util.List;
import java.util.Map;

/**
 * 编译期生成的 META-INF/groma/mcp/manifest.json 数据结构
 */
public record GromaToolManifest(
        String manifestFormatVersion,
        String modelCode,
        String modelVersion,
        String modelLabel,
        String sourceDigest,
        List<ProcessToolDef> processes
) {
    public record ProcessToolDef(
            String ref,
            String entityId,
            String name,
            String description,
            String kind,
            boolean timeConsuming,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            List<String> preconditions
    ) {}
}
