package com.groma.base.mcp.protocol;

/**
 * MCP 业务工具层标准十类错误码（严格对应 03-mcp-analysis.md 第 4.11 节）
 */
public enum McpErrorCode {
    MODEL_NOT_FOUND("模型未挂载"),
    SCENE_NOT_FOUND("场景不存在"),
    SESSION_NOT_FOUND("会话不存在或已结束"),
    INVALID_ARGUMENT("入参不符合清单里的结构或范围约束"),
    PRECONDITION_FAILED("过程启动条件不满足"),
    MAINTAIN_BROKEN("过程运行期维持条件中断"),
    PROCESS_FAILED("过程代码执行异常或后置校验失败"),
    TIMEOUT("超过墙钟或等待超时时间"),
    LIMIT_EXCEEDED("超过单实例并发推演会话上限"),
    MODEL_VERSION_MISMATCH("指定的模型版本与当前挂载清单不一致");

    private final String defaultDescription;

    McpErrorCode(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }
}
