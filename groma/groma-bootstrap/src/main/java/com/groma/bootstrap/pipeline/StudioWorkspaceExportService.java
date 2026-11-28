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

    /** 导出清单校验结果（runtimeFlavor=true 为纯运行态精简交付） */
    public record ExportPlan(List<String> modelModules, List<Path> modelDirs, boolean runtimeFlavor) {}

    /**
     * 校验所选模型（workspace 生成模块必须已构建落盘），返回可导出清单。
     * 模型名可传 {@code model-a} 或 {@code a}，统一归一为 {@code model-a}。
     */
    public ExportPlan plan(List<String> models) {
        return plan(models, false);
    }

    /** 同上，runtimeFlavor=true 时校验运行态交付前提（宿主可执行 JAR 存在） */
    public ExportPlan plan(List<String> models, boolean runtimeFlavor) {
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
        if (runtimeFlavor && hostExecutableJar() == null) {
            throw new IllegalArgumentException(
                    "EXPORT_RUNTIME_JAR_MISSING: 运行态交付需宿主以可执行 JAR 启动（未定位到运行中的 fat jar；"
                            + "IDE/exploded classes 运行时请用 flavor=full 源码工程导出）");
        }
        return new ExportPlan(List.copyOf(modules), List.copyOf(dirs), runtimeFlavor);
    }

    /** 流式写出导出 ZIP（不落中间文件） */
    public void exportZip(ExportPlan plan, OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            if (plan.runtimeFlavor()) {
                exportRuntimeZip(zip, plan);
            } else {
                exportFullZip(zip, plan);
            }
        }
        LOG.info("[Export] 源码工程 ZIP 导出完成：模型 {} flavor={}", plan.modelModules(),
                plan.runtimeFlavor() ? "runtime" : "full");
    }

    /** 完整源码工程导出（全量：base + studio + 所选模型 + bootstrap 注入版 pom） */
    private void exportFullZip(ZipOutputStream zip, ExportPlan plan) throws IOException {
        putFile(zip, repoRoot.resolve("pom.xml"), "pom.xml");
        putTree(zip, repoRoot.resolve("groma-base"), "groma-base");
        putTree(zip, repoRoot.resolve("groma-studio"), "groma-studio");
        putModelsAggregator(zip, plan.modelModules());
        for (int i = 0; i < plan.modelDirs().size(); i++) {
            putTree(zip, plan.modelDirs().get(i), "groma-models/" + plan.modelModules().get(i));
        }
        putBootstrap(zip, plan.modelModules());
    }

    /**
     * 纯运行态精简导出（code4.html E4 收割，说明书 1.3 模式二"单模型精简独立交付"）：
     * <ul>
     *   <li>{@code dist/groma-bootstrap.jar}：正在运行的宿主可执行 JAR 原样随包（建模编译器源码零泄露）；</li>
     *   <li>{@code groma-base/} + {@code groma-models/<所选>} + 裁剪版根 pom（modules=groma-base,groma-models）：
     *       解压后 {@code mvn clean package} 仅重建底座与模型源码（二次开发零 groma-studio 泄露）；</li>
     *   <li>{@code README-RUNTIME.txt}：运行与模型二次开发热挂载指引。</li>
     * </ul>
     */
    private void exportRuntimeZip(ZipOutputStream zip, ExportPlan plan) throws IOException {
        java.nio.file.Path jar = hostExecutableJar();
        putFile(zip, jar, "dist/groma-bootstrap.jar");
        putText(zip, "pom.xml", trimmedRootPom());
        putTree(zip, repoRoot.resolve("groma-base"), "groma-base");
        putModelsAggregator(zip, plan.modelModules());
        for (int i = 0; i < plan.modelDirs().size(); i++) {
            putTree(zip, plan.modelDirs().get(i), "groma-models/" + plan.modelModules().get(i));
        }
        putText(zip, "README-RUNTIME.txt", runtimeReadme(plan.modelModules()));
    }

    /** 裁剪版根 pom：模块仅 groma-base + groma-models（运行态二次开发重建用） */
    private String trimmedRootPom() throws IOException {
        String root = Files.readString(repoRoot.resolve("pom.xml"), StandardCharsets.UTF_8);
        return root.replaceAll("(?s)<modules>.*?</modules>",
                """
                        <modules>
                            <module>groma-base</module>
                            <module>groma-models</module>
                        </modules>""");
    }

    private String runtimeReadme(List<String> modules) {
        String names = String.join("、", modules);
        return """
                Groma 单模型精简独立交付包（flavor=runtime）
                ================================================

                内容：
                  dist/groma-bootstrap.jar   宿主可执行 JAR（内置 REST/WS 推演接口、MCP 服务与模型热挂载）
                  groma-base/                公共底座源码（模型编译依赖）
                  groma-models/              所选模型源码（%s）
                  pom.xml                    根 POM（modules 仅 groma-base 与 groma-models）

                快速运行：
                  java -jar dist/groma-bootstrap.jar
                  （若模型未内置，先经 POST /api/v1/studio/models/upload 上传语料构建热挂载）

                模型二次开发：
                  1. mvn clean package          （只重建底座与模型源码，无建模编译器泄露）
                  2. 修改 groma-models/<模型>/src 下源码或直接上传新语料；
                  3. 经宿主 /api/v1/studio/models/build|upload 重新构建热挂载（多版本短哈希并存）。
                """.formatted(names);
    }

    /** 定位正在运行的宿主可执行 JAR（fat jar 直启时存在；IDE exploded 运行为 null） */
    private Path hostExecutableJar() {
        try {
            org.springframework.boot.system.ApplicationHome home =
                    new org.springframework.boot.system.ApplicationHome(StudioWorkspaceExportService.class);
            java.io.File source = home.getSource();
            if (source != null && source.isFile() && source.getName().endsWith(".jar")) {
                return source.toPath().toAbsolutePath().normalize();
            }
        } catch (RuntimeException ignored) {
            // 非 jar 运行（IDE/exploded）：运行态交付不可用
        }
        return null;
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
