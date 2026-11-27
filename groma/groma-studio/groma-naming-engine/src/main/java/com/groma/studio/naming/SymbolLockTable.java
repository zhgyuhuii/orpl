package com.groma.studio.naming;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 持久化符号锁表（读写 groma-symbols.lock.json；一旦写入锁表，后续重导入或大模型回填均不得篡改已锁定符号！）
 */
public final class SymbolLockTable {

    private final TreeMap<String, String> lockedSymbols = new TreeMap<>();
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public Optional<String> getLocked(String lockKey) {
        return Optional.ofNullable(lockedSymbols.get(lockKey));
    }

    public boolean isLocked(String lockKey) {
        return lockedSymbols.containsKey(lockKey);
    }

    public void lockIfAbsent(String lockKey, String symbol) {
        lockedSymbols.putIfAbsent(lockKey, symbol);
    }

    public boolean containsSymbolInScope(String scopePrefix, String candidateSymbol) {
        for (Map.Entry<String, String> e : lockedSymbols.entrySet()) {
            if (e.getKey().startsWith(scopePrefix + ":") && candidateSymbol.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    public void loadFromFile(Path lockFile) throws IOException {
        if (Files.exists(lockFile)) {
            Map<String, String> loaded = mapper.readValue(
                    Files.readAllBytes(lockFile),
                    new TypeReference<Map<String, String>>() {}
            );
            lockedSymbols.putAll(loaded);
        }
    }

    public void saveToFile(Path lockFile) throws IOException {
        if (lockFile.getParent() != null) {
            Files.createDirectories(lockFile.getParent());
        }
        Files.writeString(lockFile, mapper.writeValueAsString(lockedSymbols), StandardCharsets.UTF_8);
    }

    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(lockedSymbols));
    }
}
