package com.groma.studio.sync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动维护 groma-models/pom.xml 的 <modules> 列表（落实 NP-GEN-005：新增模型自动登记，排序确定且幂等）
 */
public final class ParentPomModuleRegistrar {

    private static final Pattern MODULES_BLOCK_RX = Pattern.compile("(?s)<modules>(.*?)</modules>");
    private static final Pattern SINGLE_MODULE_RX = Pattern.compile("<module>\\s*([^<\\s]+)\\s*</module>");

    public boolean ensureModuleRegistered(Path modelsAggregatorDir, String moduleDirName) throws IOException {
        Files.createDirectories(modelsAggregatorDir);
        Path pomPath = modelsAggregatorDir.resolve("pom.xml");

        Set<String> modules = new LinkedHashSet<>();
        String existingPom = Files.exists(pomPath) ? Files.readString(pomPath, StandardCharsets.UTF_8) : "";

        if (!existingPom.isBlank()) {
            Matcher blockMatcher = MODULES_BLOCK_RX.matcher(existingPom);
            if (blockMatcher.find()) {
                Matcher itemMatcher = SINGLE_MODULE_RX.matcher(blockMatcher.group(1));
                while (itemMatcher.find()) {
                    modules.add(itemMatcher.group(1).trim());
                }
            }
        }

        boolean added = modules.add(moduleDirName);
        if (!added && Files.exists(pomPath)) {
            return false;
        }

        List<String> sorted = new ArrayList<>(modules);
        Collections.sort(sorted);

        StringBuilder modulesXml = new StringBuilder("    <modules>\n");
        for (String m : sorted) {
            modulesXml.append("        <module>").append(m).append("</module>\n");
        }
        modulesXml.append("    </modules>");

        String updatedPom;
        if (existingPom.isBlank()) {
            updatedPom = buildDefaultAggregatorPom(modulesXml.toString());
        } else if (MODULES_BLOCK_RX.matcher(existingPom).find()) {
            updatedPom = MODULES_BLOCK_RX.matcher(existingPom)
                    .replaceFirst(Matcher.quoteReplacement(modulesXml.toString()));
        } else {
            updatedPom = existingPom.replace("</project>", modulesXml + "\n</project>");
        }

        atomicWriteString(pomPath, updatedPom);
        return true;
    }

    private String buildDefaultAggregatorPom(String modulesBlock) {
        return """
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
                    <name>Groma Compiled Models Aggregator</name>
                %s
                </project>
                """.formatted(modulesBlock);
    }

    static void atomicWriteString(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp." + System.nanoTime());
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
