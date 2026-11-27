package com.groma.base.connector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 覆盖 CK-BE-SEC-002-1/2、CK-BE-EXT-004-1/2、CK-BE-EXT-005-1/2、CK-BE-EXT-006-1、CK-BE-PER-002-1~3
 */
class ConnectorSecurityAndBatchPersistTest {

    @TempDir
    Path tempDir;

    @Test
    void expressionSandboxRejectsJavaInteropAndHandlesNull() {
        ExpressionSandboxEvaluator sandbox = new ExpressionSandboxEvaluator(true);
        Assertions.assertThrows(SecurityException.class,
                () -> sandbox.evaluateBoolean("Java.type('java.lang.Runtime').getRuntime()", Map.of()));
        Assertions.assertFalse(sandbox.evaluateBoolean(null, Map.of()));
        Assertions.assertTrue(sandbox.evaluateBoolean("waterDepth >= 0.5", Map.of("waterDepth", 0.8D)));
    }

    @Test
    void externalProgramRejectsPathTraversalAndNetworkSource() {
        ExternalProgramConnector prog = new ExternalProgramConnector(
                Map.of("python", Path.of("/usr/bin/python3")),
                tempDir,
                false,
                Set.of(),
                Duration.ofSeconds(2)
        );
        Assertions.assertThrows(SecurityException.class,
                () -> prog.executeScript("python", "../../etc/passwd", List.of()));
        Assertions.assertThrows(SecurityException.class,
                () -> prog.executeScript("python", "/etc/passwd", List.of()));
        Assertions.assertThrows(SecurityException.class,
                () -> prog.validateNetworkSourceUri("http://169.254.169.254/evil.java"));
    }

    @Test
    void anyTableRejectsSqlInjectionAndUnconfiguredDataSource() {
        AnyTableQueryConnector db = new AnyTableQueryConnector();
        Assertions.assertThrows(SecurityException.class,
                () -> db.selectSafely("unregistered_ds", "users", List.of("*"), Map.of(), null, true, 10));

        db.registerConfiguredDataSource(new AnyTableQueryConnector.DataSourceRegistration(
                "readonly_ds", null, true, 5, 100));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> db.selectSafely("readonly_ds", "users; DROP TABLE x", List.of("*"), Map.of(), null, true, 10));
        Assertions.assertThrows(SecurityException.class,
                () -> db.executeWriteIfPermitted("readonly_ds", "users", Map.of("name", "a")));
    }

    @Test
    void mcpClientBlocksMetadataAndInternalAddresses() {
        AllowlistedMcpClientConnector mcp = new AllowlistedMcpClientConnector(Set.of("mcp.internal.trusted"));
        Assertions.assertThrows(SecurityException.class, () -> mcp.validateTargetEndpoint("http://169.254.169.254/"));
        Assertions.assertThrows(SecurityException.class, () -> mcp.validateTargetEndpoint("http://127.0.0.1:6379/"));
        Assertions.assertEquals("mcp.internal.trusted", mcp.validateTargetEndpoint("https://mcp.internal.trusted/mcp").getHost());
    }

    @Test
    void immutableBatchesNeverCorruptAndAckOnlyAfterPersist() throws Exception {
        // CK-BE-PER-002-1 & 2：模拟落库耗时超过刷新间隔，连续 10 批并发刷盘，每条恰好落库一次且落库后才 Ack
        List<Integer> persistedItems = new CopyOnWriteArrayList<>();
        AtomicInteger ackedCount = new AtomicInteger(0);

        try (ImmutableBatchPersistenceHandler<Integer> handler = new ImmutableBatchPersistenceHandler<>(
                1000, 5, 1,
                batch -> {
                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException ignored) {
                    }
                    persistedItems.addAll(batch);
                },
                dlq -> {})) {

            for (int i = 1; i <= 50; i++) {
                handler.offer(i, ackedCount::incrementAndGet);
            }
            Assertions.assertEquals(0, ackedCount.get(), "落库完成前严禁提前执行 Ack");

            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (int b = 0; b < 10; b++) {
                futures.add(handler.flushNextBatchAsync());
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            Assertions.assertEquals(50, persistedItems.size());
            Assertions.assertEquals(50, Set.copyOf(persistedItems).size(), "50 条记录必须不丢不重");
            Assertions.assertEquals(50, ackedCount.get());
        }
    }
}
