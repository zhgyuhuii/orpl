package com.groma.studio.importer;

import java.time.Duration;

/**
 * 存量平台模型只读拉取配置（落实 NP-IMP-001 & BE-EXT-001：默认连接超时 3s、读取超时 60s、重试上限 3 次）
 */
public record ModelImportConfig(
        String platformBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxRetries
) {
    public ModelImportConfig {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            readTimeout = Duration.ofSeconds(60);
        }
        if (maxRetries < 0 || maxRetries > 3) {
            maxRetries = 3;
        }
    }

    public static ModelImportConfig defaults(String platformBaseUrl) {
        return new ModelImportConfig(platformBaseUrl, Duration.ofSeconds(3), Duration.ofSeconds(60), 3);
    }
}
