package com.groma.studio.sync;

import java.util.List;

/**
 * 工作区文件增量同步执行报告（落实 NP-HOT-003 三项检查判据）
 *
 * @param moduleDirName              子模块目录名（如 model-emergency-deduction）
 * @param writtenFiles               内容发生实际变化并原子写盘的相对路径列表
 * @param skippedUnchangedFiles      SHA-256 一致而跳过写盘的相对路径列表
 * @param preservedUserCodeRegionIds 成功提取并无损合并回新源码的 @GromaUserCode 区域 ID 列表
 * @param outsideOverwriteWarnings   检测到工程师修改了保护区之外的代码而被覆盖的告警列表
 */
public record SyncReport(
        String moduleDirName,
        List<String> writtenFiles,
        List<String> skippedUnchangedFiles,
        List<String> preservedUserCodeRegionIds,
        List<String> outsideOverwriteWarnings
) {}
