package com.groma.base.connector;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 外部脚本（Python / Node / R）安全沙盒调用器（落实 BE-EXT-004：路径穿越拦截 + 网络源码默认关闭 + 超时强杀 + 临时文件清理）
 */
public final class ExternalProgramConnector {

    private final Map<String, Path> configuredExecutables;
    private final Path allowlistedScriptDir;
    private final boolean networkSourceEnabled;
    private final Set<String> allowlistedNetworkHosts;
    private final Duration executionTimeout;

    public ExternalProgramConnector(
            Map<String, Path> configuredExecutables,
            Path allowlistedScriptDir,
            boolean networkSourceEnabled,
            Set<String> allowlistedNetworkHosts,
            Duration executionTimeout) {
        this.configuredExecutables = Map.copyOf(configuredExecutables);
        this.allowlistedScriptDir = allowlistedScriptDir.toAbsolutePath().normalize();
        this.networkSourceEnabled = networkSourceEnabled;
        this.allowlistedNetworkHosts = Set.copyOf(allowlistedNetworkHosts);
        this.executionTimeout = executionTimeout == null ? Duration.ofSeconds(30) : executionTimeout;
    }

    /**
     * 收割块 26（方案 F）：JSON stdin/stdout 双向受控执行——inputs 以 JSON 写入脚本标准输入，
     * 脚本把结果 JSON 写标准输出并由本方法解析返回。既有安全语义全部保留：
     * 运行时白名单（configuredExecutables）、执行超时 destroyForcibly 强杀、独立临时工作目录。
     * 脚本文件路径须是绝对路径（由调用方从模块资源提取到受控临时文件，见宿主 ScriptIoBridgeConfiguration）。
     */
    public java.util.Map<String, Object> runScriptWithJsonIo(String runtimeKey, java.nio.file.Path scriptFile,
                                                             java.util.Map<String, Object> inputs) {
        java.nio.file.Path executable = configuredExecutables.get(runtimeKey);
        if (executable == null) {
            throw new SecurityException("[EXT-PROG-001] 未配置的外部运行时: " + runtimeKey);
        }
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        java.nio.file.Path isolatedWorkDir = null;
        try {
            isolatedWorkDir = Files.createTempDirectory("groma-script-sandbox-");
            Process proc = new ProcessBuilder(executable.toString(), scriptFile.toAbsolutePath().toString())
                    .directory(isolatedWorkDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            try (var stdin = proc.getOutputStream()) {
                stdin.write(mapper.writeValueAsBytes(inputs == null ? Map.of() : inputs));
                stdin.flush();
            }
            boolean finished = proc.waitFor(executionTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                proc.destroyForcibly();
                proc.waitFor(2, TimeUnit.SECONDS);
                throw new IllegalStateException("[EXT-PROG-003] 外部算法脚本执行超时 (" + executionTimeout.toSeconds() + "s)，已强制终止子进程");
            }
            byte[] stdoutBytes = proc.getInputStream().readAllBytes();
            if (proc.exitValue() != 0) {
                String errText = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("[EXT-PROG-004] 外部脚本执行失败(exit=" + proc.exitValue() + "): " + errText);
            }
            if (stdoutBytes.length == 0) {
                return Map.of();
            }
            java.util.Map<String, Object> parsed = mapper.readValue(stdoutBytes,
                    new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() { });
            return parsed == null ? Map.of() : java.util.Collections.unmodifiableMap(parsed);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return java.util.Collections.emptyMap();
            }
            throw new IllegalStateException("[EXT-PROG-005] 外部脚本 IO 错误: " + ex.getMessage(), ex);
        } finally {
            if (isolatedWorkDir != null) {
                try (java.util.stream.Stream<Path> walk = Files.walk(isolatedWorkDir)) {
                    for (Path p2 : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(p2);
                    }
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响主流程
                }
            }
        }
    }

    public String executeScript(String runtimeKey, String relativeScriptPath, List<String> args) throws IOException, InterruptedException {
        Path executable = configuredExecutables.get(runtimeKey);
        if (executable == null) {
            throw new SecurityException("[EXT-PROG-001] 未配置的外部运行时: " + runtimeKey);
        }
        if (relativeScriptPath == null || relativeScriptPath.isBlank()) {
            throw new IllegalArgumentException("脚本路径不能为空");
        }
        Path rawPath = Path.of(relativeScriptPath);
        if (rawPath.isAbsolute()) {
            throw new SecurityException("[EXT-PROG-002] 禁止传入绝对脚本路径: " + relativeScriptPath);
        }
        Path resolvedScript = allowlistedScriptDir.resolve(rawPath).normalize();
        if (!resolvedScript.startsWith(allowlistedScriptDir)) {
            throw new SecurityException("[EXT-PROG-002] 检测到脚本路径越界穿越: " + relativeScriptPath);
        }
        if (!Files.isRegularFile(resolvedScript)) {
            throw new IllegalArgumentException("白名单目录内未找到脚本文件: " + relativeScriptPath);
        }

        Path isolatedWorkDir = Files.createTempDirectory("groma-ext-work-");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(executable.toString());
            cmd.add(resolvedScript.toString());
            if (args != null) {
                cmd.addAll(args);
            }
            Process proc = new ProcessBuilder(cmd)
                    .directory(isolatedWorkDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = proc.waitFor(executionTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                proc.destroyForcibly();
                proc.waitFor(2, TimeUnit.SECONDS);
                throw new IllegalStateException("[EXT-PROG-003] 外部脚本执行超时 (" + executionTimeout.toSeconds() + "s)，已强制终止子进程");
            }
            byte[] out = proc.getInputStream().readAllBytes();
            return new String(out, StandardCharsets.UTF_8);
        } finally {
            deleteDirQuietly(isolatedWorkDir);
        }
    }

    public URI validateNetworkSourceUri(String rawUri) {
        if (!networkSourceEnabled) {
            throw new SecurityException("[EXT-PROG-004] 从网络地址读取源码执行的能力默认关闭");
        }
        URI uri = URI.create(rawUri);
        String host = uri.getHost();
        if (host == null || !allowlistedNetworkHosts.contains(host.toLowerCase())) {
            throw new SecurityException("[EXT-PROG-004] 网络源码主机不在白名单中: " + host);
        }
        return uri;
    }

    private static void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
        }
    }
}
