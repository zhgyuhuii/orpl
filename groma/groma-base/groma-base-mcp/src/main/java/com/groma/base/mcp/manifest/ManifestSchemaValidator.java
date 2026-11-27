package com.groma.base.mcp.manifest;

import com.groma.base.mcp.protocol.GromaMcpException;
import com.groma.base.mcp.protocol.McpErrorCode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MCP 工具清单合法性校验器与运行时 JSON Schema 入参校验器（落实 CK-NP-MCP-002-1 & CK-NP-MCP-006-2）
 */
public final class ManifestSchemaValidator {

    private static final Pattern VALID_TOOL_NAME = Pattern.compile("^[a-z0-9_]{1,64}$");

    private ManifestSchemaValidator() {
    }

    /**
     * 校验整个模型的 manifest.json 是否满足 NP-MCP-002 建设标准第 4、5 条
     */
    public static void validateManifest(GromaToolManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("MCP 清单不能为空");
        }
        if (isBlank(manifest.modelCode()) || isBlank(manifest.modelVersion())) {
            throw new IllegalArgumentException("MCP 清单必须包含 modelCode 与 modelVersion");
        }
        String prefix = manifest.modelCode().replace('-', '_').toLowerCase();
        Set<String> seenNames = new HashSet<>();

        if (manifest.processes() == null) {
            return;
        }
        for (GromaToolManifest.ProcessToolDef p : manifest.processes()) {
            if (isBlank(p.ref()) || !VALID_TOOL_NAME.matcher(p.ref()).matches()) {
                throw new IllegalArgumentException("非法的工具 ref 命名（仅允许小写字母、数字、下划线且长度<=64）: " + p.ref());
            }
            String fullPrefixedName = prefix + "__run_" + p.ref();
            if (fullPrefixedName.length() > 64 || !VALID_TOOL_NAME.matcher(fullPrefixedName).matches()) {
                throw new IllegalArgumentException("带前缀的完整工具名超出 64 字符或含非法字符: " + fullPrefixedName);
            }
            if (!seenNames.add(p.ref())) {
                throw new IllegalArgumentException("同一模型清单内存在重复的工具 ref: " + p.ref());
            }
            if (isBlank(p.description())) {
                throw new IllegalArgumentException("工具描述 description 不能为空: " + p.ref());
            }
            validateSchemaObject(p.ref() + ".inputSchema", p.inputSchema());
            validateSchemaObject(p.ref() + ".outputSchema", p.outputSchema());
        }
    }

    /**
     * 运行时根据 JSON Schema 严格校验智能体传入的实参 Map，不合法直接抛出 INVALID_ARGUMENT
     */
    @SuppressWarnings("unchecked")
    public static void validateArgumentsAgainstSchema(Map<String, Object> schema, Map<String, Object> args) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        Map<String, Object> safeArgs = args == null ? Map.of() : args;

        Object reqObj = schema.get("required");
        if (reqObj instanceof Collection<?> requiredList) {
            for (Object r : requiredList) {
                String reqKey = String.valueOf(r);
                if (!safeArgs.containsKey(reqKey) || safeArgs.get(reqKey) == null) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "缺少必填入参字段: " + reqKey);
                }
            }
        }

        Object propsObj = schema.get("properties");
        if (!(propsObj instanceof Map<?, ?> rawProps)) {
            return;
        }
        for (Map.Entry<String, Object> argEntry : safeArgs.entrySet()) {
            String key = argEntry.getKey();
            Object val = argEntry.getValue();
            Object propDefObj = rawProps.get(key);
            if (propDefObj == null) {
                throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT, "存在未在工具清单声明的多余入参字段: " + key);
            }
            if (val != null && propDefObj instanceof Map<?, ?> propSchema) {
                validateSingleValue(key, val, (Map<String, Object>) propSchema);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateSingleValue(String fieldPath, Object value, Map<String, Object> propSchema) {
        String expectedType = String.valueOf(propSchema.getOrDefault("type", "string"));
        switch (expectedType) {
            case "integer" -> {
                if (!(value instanceof Integer || value instanceof Long || value instanceof Short)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 integer 类型，实际收到: " + value.getClass().getSimpleName());
                }
                checkNumericBounds(fieldPath, ((Number) value).doubleValue(), propSchema);
            }
            case "number" -> {
                if (!(value instanceof Number num)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 number 类型，实际收到: " + value.getClass().getSimpleName());
                }
                checkNumericBounds(fieldPath, num.doubleValue(), propSchema);
            }
            case "boolean" -> {
                if (!(value instanceof Boolean)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 boolean 类型，实际收到: " + value.getClass().getSimpleName());
                }
            }
            case "string" -> {
                if (!(value instanceof String strVal)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 string 类型，实际收到: " + value.getClass().getSimpleName());
                }
                Object enumObj = propSchema.get("enum");
                if (enumObj instanceof List<?> allowedEnums && !allowedEnums.contains(strVal)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 取值 '" + strVal + "' 不在允许的状态枚举范围内: " + allowedEnums);
                }
            }
            case "array" -> {
                if (!(value instanceof List<?>)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 array 类型");
                }
            }
            case "object" -> {
                if (!(value instanceof Map<?, ?> nestedMap)) {
                    throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                            "字段 [" + fieldPath + "] 期望 object 类型");
                }
                validateArgumentsAgainstSchema(propSchema, (Map<String, Object>) nestedMap);
            }
            default -> {
            }
        }
    }

    private static void checkNumericBounds(String fieldPath, double val, Map<String, Object> propSchema) {
        Object minObj = propSchema.get("minimum");
        if (minObj instanceof Number minNum && val < minNum.doubleValue()) {
            throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                    "字段 [" + fieldPath + "] 值 " + val + " 小于下限 minimum=" + minNum);
        }
        Object maxObj = propSchema.get("maximum");
        if (maxObj instanceof Number maxNum && val > maxNum.doubleValue()) {
            throw new GromaMcpException(McpErrorCode.INVALID_ARGUMENT,
                    "字段 [" + fieldPath + "] 值 " + val + " 大于上限 maximum=" + maxNum);
        }
    }

    private static void validateSchemaObject(String location, Map<String, Object> schema) {
        if (schema == null || !"object".equals(schema.get("type"))) {
            throw new IllegalArgumentException("清单 Schema 根节点必须声明 type=object: " + location);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
