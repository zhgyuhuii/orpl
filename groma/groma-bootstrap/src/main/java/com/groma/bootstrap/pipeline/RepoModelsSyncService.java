package com.groma.bootstrap.pipeline;

import com.groma.studio.codegen.GeneratedModuleBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 生成模型仓内合包同步（code4.html E2 收割，双轨割裂收口）：
 * 流水线构建成功后把 workspace 模块镜像进仓库 {@code groma/groma-models/<moduleDir>}，
 * 使 {@code mvn clean package} 一键合包出内置模型的宿主 JAR（说明书 1.3 模式一）。
 * <p>
 * 同源多版本只保留仓内最新一份：按稳定 artifactId（{@code model-{基础码}}，剥离语料短哈希）
 * 归属，同步前先删除同基础名的旧版本目录，再重写 groma-models/pom.xml 的默认 profile
 * modules 清单为现存生成模块全集。配置 {@code groma.pipeline.repo-models-root} 缺省关闭
 * （单测与临时工作区路径不落仓），真机部署时指向仓库 groma-models 目录开启。
 * </p>
 */
@Service
public class RepoModelsSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(RepoModelsSyncService.class);

    /** 目录名的稳定基础名（剥离 -{8位小写十六进制} 语料短哈希后缀） */
    private static final Pattern HASH_SUFFIX = Pattern.compile("^(model-.+)-[0-9a-f]{8}$");

    private final Path repoModelsRoot;

    public RepoModelsSyncService(@Value("${groma.pipeline.repo-models-root:}") String repoModelsRoot) {
        this.repoModelsRoot = repoModelsRoot == null || repoModelsRoot.isBlank()
                ? null : Path.of(repoModelsRoot).toAbsolutePath().normalize();
    }

    public boolean enabled() {
        return repoModelsRoot != null;
    }

    /**
     * 同步一个构建成功的生成模块进仓库合包槽位，并重写聚合 pom 的模块清单。
     *
     * @return 仓内模块目录；未启用同步时返回 null
     */
    public Path syncModule(String moduleDirName, Path workspaceModuleDir) throws IOException {
        if (!enabled() || !Files.isDirectory(workspaceModuleDir)) {
            return null;
        }
        Files.createDirectories(repoModelsRoot);
        String stableBase = stableBaseOf(moduleDirName);
        // 1. 同基础名旧版本出仓（同 artifactId 在 reactor 中不可重复）
        try (Stream<Path> children = Files.list(repoModelsRoot)) {
            for (Path child : children.toList()) {
                String name = child.getFileName().toString();
                if (Files.isDirectory(child) && name.startsWith("model-") && !name.equals(moduleDirName)
                        && stableBaseOf(name).equals(stableBase)) {
                    deleteRecursively(child);
                    LOG.info("[RepoSync] 同基础名旧版本出仓: {}", name);
                }
            }
        }
        // 2. 镜像模块目录（先删后拷，保证仓内与 workspace 一致）
        Path target = repoModelsRoot.resolve(moduleDirName);
        deleteRecursively(target);
        copyRecursively(workspaceModuleDir, target);
        // 3. 重写聚合 pom 的默认 profile modules 清单（现存生成模块全集）
        rewriteAggregatorPom();
        LOG.info("[RepoSync] 生成模块已同步进仓内合包槽位: {} → {}", moduleDirName, target);
        return target;
    }

    /** 重写 groma-models/pom.xml 默认 profile 的 modules 清单为现存 model-* 目录 */
    private void rewriteAggregatorPom() throws IOException {
        Path pom = repoModelsRoot.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return;
        }
        List<String> moduleDirs = new ArrayList<>();
        try (Stream<Path> children = Files.list(repoModelsRoot)) {
            children.filter(Files::isDirectory)
                    .filter(d -> d.getFileName().toString().startsWith("model-"))
                    .map(d -> d.getFileName().toString())
                    .sorted()
                    .forEach(moduleDirs::add);
        }
        StringBuilder modulesXml = new StringBuilder();
        for (String dir : moduleDirs) {
            modulesXml.append("                <module>").append(dir).append("</module>\n");
        }
        String text = Files.readString(pom, StandardCharsets.UTF_8);
        // 兼容空清单自闭合 <modules/> 与展开 <modules>…</modules> 两种形态
        Pattern block = Pattern.compile(
                "(<id>multi-models-default</id>\\s*<activation>\\s*<activeByDefault>true</activeByDefault>\\s*</activation>\\s*)"
                        + "(?:<modules\\s*/>|<modules>.*?</modules>)",
                Pattern.DOTALL);
        Matcher matcher = block.matcher(text);
        String rewritten = matcher.find()
                ? matcher.replaceFirst("$1<modules>\n" + modulesXml + "                </modules>")
                : text;
        // XML 有效性自检（交接纪律：pom 改动必须校验），失败则放弃重写保留原文件
        try {
            javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(rewritten)));
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException ex) {
            LOG.warn("[RepoSync] 聚合 pom 重写结果 XML 校验失败，保留原文件: {}", ex.getMessage());
            return;
        }
        Files.writeString(pom, rewritten, StandardCharsets.UTF_8);
    }

    private static String stableBaseOf(String moduleDirName) {
        Matcher matcher = HASH_SUFFIX.matcher(moduleDirName);
        return matcher.matches() ? matcher.group(1) : moduleDirName;
    }

    private static void copyRecursively(Path from, Path to) throws IOException {
        try (Stream<Path> stream = Files.walk(from)) {
            for (Path source : stream.sorted().toList()) {
                Path dest = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // 尽力而为：出仓失败留给下次同步重试
                }
            });
        }
    }
}
