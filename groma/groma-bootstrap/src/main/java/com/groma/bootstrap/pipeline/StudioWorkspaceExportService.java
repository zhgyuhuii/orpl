package com.groma.bootstrap.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 一键源码工程 ZIP 打包导出（落实建议 9：兑现说明书"勾选单个或多个模型模块下载完整可独立
 * {@code mvn clean package} 的源码工程 .zip"承诺）。
 * <p>
 * 打包内容：根 pom（模块已按 base/studio/models/bootstrap 四组裁剪）+ {@code groma-base/} +
 * {@code groma-studio/} + 所选<b>生成模型</b>（workspace 内含 SPI 运行时入口的模块，静态合包）+
 * {@code groma-bootstrap/}（pom 自动注入所选模型依赖，SPI 随包，启动即挂载）。
 * 产出工程解压后直接 {@code mvn clean package} 即得内置所选模型的宿主 JAR。
 * 手写示例模块（无运行时入口，E1 决策保持不动）不参与合包。
 * 排除：{@code target/}、{@code node_modules/}、{@code .git/}、运行期工作区 {@code groma-workspace/}。
 * </p>
 */
@Service
public class StudioWorkspaceExportService {

    private static final Logger LOG = LoggerFactory.getLogger(StudioWorkspaceExportService.class);

    private static final Set<String> EXCLUDED_DIR_NAMES = Set.of("target", "node_modules", ".git", ".idea", "groma-workspace");

    private final Path repoRoot;
    private final Path workspaceRoot;

    public StudioWorkspaceExportService(
            @Value("${groma.pipeline.workspace-root:groma-workspace}") String workspaceRoot) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
        this.repoRoot = locateRepoRoot(this.workspaceRoot);
    }

    /** 导出清单校验结果 */
    public record ExportPlan(List<String> modelModules, List<Path> modelDirs) {}

    /**
     * 校验所选模型（workspace 生成模块必须已构建落盘），返回可导出清单。
     * 模型名可传 {@code model-a} 或 {@code a}，统一归一为 {@code model-a}。
     */
    public ExportPlan plan(List<String> models) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("EXPORT_BAD_REQUEST: 至少选择一个模型模块");
        }
        List<String> modules = new ArrayList<>();
        List<Path> dirs = new ArrayList<>();
        Path workspaceModels = workspaceRoot.resolve("groma-models");
        for (String raw : models) {
            String name = normalizeModule(raw);
            Path dir = workspaceModels.resolve(name);
            if (!Files.isDirectory(dir.resolve("src")) || !Files.isRegularFile(dir.resolve("pom.xml"))) {
                throw new IllegalArgumentException("EXPORT_MODEL_NOT_FOUND: 模型 " + name
                        + " 尚未构建落盘（workspace 内未找到可合包模块），请先经 /api/v1/studio/models/build 构建");
            }
            modules.add(name);
            dirs.add(dir);
        }
        return new ExportPlan(List.copyOf(modules), List.copyOf(dirs));
    }

    /** 流式写出导出 ZIP（不落中间文件） */
    public void exportZip(ExportPlan plan, OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            putFile(zip, repoRoot.resolve("pom.xml"), "pom.xml");
            putTree(zip, repoRoot.resolve("groma-base"), "groma-base");
            putTree(zip, repoRoot.resolve("groma-studio"), "groma-studio");
            putModelsAggregator(zip, plan.modelModules());
            for (int i = 0; i < plan.modelModules().size(); i++) {
                putTree(zip, plan.modelDirs().get(i), "groma-models/" + plan.modelModules().get(i));
            }
            putBootstrap(zip, plan.modelModules());
        }
        LOG.info("[Export] 源码工程 ZIP 导出完成：模型 {}", plan.modelModules());
    }

    // ---------------- 内部 ----------------

    /** 从工作区向上定位仓库根（含 groma-base 与 groma-bootstrap 与根 pom 的目录） */
    private static Path locateRepoRoot(Path workspace) {
        for (Path candidate = workspace.getParent(); candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("groma-base"))
                    && Files.isDirectory(candidate.resolve("groma-bootstrap"))
                    && Files.isRegularFile(candidate.resolve("pom.xml"))) {
                return candidate;
            }
        }
        // 兜底：工作区默认落在 <repo>/groma/groma-bootstrap 下，取上两级
        Path parent = workspace.getParent();
        return parent == null || parent.getParent() == null ? workspace : parent.getParent();
    }

    private String normalizeModule(String raw) {
        String name = raw == null ? "" : raw.trim();
        return name.startsWith("model-") ? name : "model-" + name;
    }

    /** 生成 groma-models 聚合 pom：默认 profile 的 modules 裁剪为所选模型 */
    private void putModelsAggregator(ZipOutputStream zip, List<String> modules) throws IOException {
        StringBuilder modulesXml = new StringBuilder();
        for (String module : modules) {
            modulesXml.append("                <module>").append(module).append("</module>\n");
        }
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.groma</groupId>
                        <artifactId>groma</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>groma-models</artifactId>
                    <packaging>pom</packaging>
                    <name>Groma Compiled Business Models (exported bundle)</name>

                    <profiles>
                        <profile>
                            <id>multi-models-default</id>
                            <activation>
                                <activeByDefault>true</activeByDefault>
                            </activation>
                            <modules>
                %s                </modules>
                        </profile>
                    </profiles>
                </project>
                """.formatted(modulesXml);
        putText(zip, "groma-models/pom.xml", pom);
    }

    /** bootstrap：pom 写注入版（追加所选模型依赖），src 整棵打包 */
    private void putBootstrap(ZipOutputStream zip, List<String> modules) throws IOException {
        Path bootstrap = repoRoot.resolve("groma-bootstrap");
        Path originalPom = bootstrap.resolve("pom.xml");
        String injected = bootstrapPomWithModels(Files.readString(originalPom, StandardCharsets.UTF_8), modules);
        putText(zip, "groma-bootstrap/pom.xml", injected);
        putTree(zip, bootstrap.resolve("src"), "groma-bootstrap/src");
    }

    /** bootstrap pom 注入所选模型的静态合包依赖（插在 </dependencies> 之前） */
    static String bootstrapPomWithModels(String originalPom, List<String> modules) {
        StringBuilder deps = new StringBuilder();
        deps.append("        <!-- 选中模型静态合包（export-zip 自动注入；SPI 入口随包，启动即挂载） -->\n");
        for (String module : modules) {
            deps.append("        <dependency>\n")
                    .append("            <groupId>com.groma</groupId>\n")
                    .append("            <artifactId>").append(module).append("</artifactId>\n")
                    .append("            <version>${project.version}</version>\n")
                    .append("        </dependency>\n");
        }
        int insertAt = originalPom.indexOf("</dependencies>");
        if (insertAt < 0) {
            return originalPom;
        }
        return originalPom.substring(0, insertAt) + deps + originalPom.substring(insertAt);
    }

    private boolean excluded(Path dir) {
        String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
        return EXCLUDED_DIR_NAMES.contains(name);
    }

    private void putTree(ZipOutputStream zip, Path root, String prefix) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path file : stream.sorted().toList()) {
                if (file.equals(root)) {
                    continue;
                }
                if (Files.isDirectory(file)) {
                    continue; // 目录条目由文件路径隐式创建
                }
                if (isInsideExcludedDir(root, file)) {
                    continue;
                }
                putFile(zip, file, prefix + "/" + root.relativize(file));
            }
        }
    }

    /** 祖先链上任一目录命中排除名单则整棵跳过 */
    private boolean isInsideExcludedDir(Path root, Path file) {
        for (Path part : root.relativize(file)) {
            if (excluded(part)) {
                return true;
            }
        }
        return false;
    }

    private void putFile(ZipOutputStream zip, Path file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private void putText(ZipOutputStream zip, String entryName, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
