package com.groma.bootstrap;

import com.groma.bootstrap.hygiene.HeartbeatAndWorkDirCleaner;
import com.groma.bootstrap.hygiene.TaskIdMdcSanitizingFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 覆盖 CK-BE-SEC-004-2、CK-BE-TSK-007-1/2、CK-BE-CMP-006-1/2
 */
class BootstrapGovernanceTest {

    @TempDir
    Path tempDir;

    @Test
    void taskIdWithNewlineIsSanitized() {
        String malicious = "sim_01\r\nINFO fake_log_entry";
        String clean = TaskIdMdcSanitizingFilter.sanitizeTaskId(malicious);
        Assertions.assertFalse(clean.contains("\n") || clean.contains("\r") || clean.contains(" "));
        Assertions.assertTrue(clean.matches("^[A-Za-z0-9_-]+$"));
    }

    @Test
    void heartbeatStopsAfterTimeoutAndCanBeDisabled() {
        List<String> stopped = new ArrayList<>();
        HeartbeatAndWorkDirCleaner enabledCleaner = new HeartbeatAndWorkDirCleaner(true, 60L, Duration.ofHours(1));
        enabledCleaner.touchHeartbeat("s_timeout");
        long after65Sec = System.currentTimeMillis() + 65_000L;
        Assertions.assertEquals(List.of("s_timeout"), enabledCleaner.checkExpiredHeartbeats(after65Sec, stopped::add));
        Assertions.assertEquals(List.of("s_timeout"), stopped);

        HeartbeatAndWorkDirCleaner disabledCleaner = new HeartbeatAndWorkDirCleaner(false, 60L, Duration.ofHours(1));
        disabledCleaner.touchHeartbeat("s_keep");
        Assertions.assertTrue(disabledCleaner.checkExpiredHeartbeats(after65Sec, s -> {}).isEmpty());
    }

    @Test
    void dirRemovedOnTaskRemoveAndStaleDirsSwept() throws Exception {
        HeartbeatAndWorkDirCleaner cleaner = new HeartbeatAndWorkDirCleaner(true, 60L, Duration.ofHours(24));
        Path activeDir = Files.createDirectory(tempDir.resolve("s_active"));
        Path removedDir = Files.createDirectory(tempDir.resolve("s_remove"));
        Path staleDir = Files.createDirectory(tempDir.resolve("s_stale"));
        Files.setLastModifiedTime(staleDir, FileTime.from(Instant.now().minus(Duration.ofHours(25))));

        cleaner.onSessionRemoved("s_remove", removedDir);
        Assertions.assertFalse(Files.exists(removedDir));

        int swept = cleaner.sweepStaleWorkDirs(tempDir, Instant.now(), Set.of("s_active"));
        Assertions.assertEquals(1, swept);
        Assertions.assertFalse(Files.exists(staleDir));
        Assertions.assertTrue(Files.exists(activeDir));
    }
}
