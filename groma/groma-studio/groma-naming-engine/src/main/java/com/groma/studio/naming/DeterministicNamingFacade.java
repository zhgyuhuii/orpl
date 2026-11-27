package com.groma.studio.naming;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 四级确定性命名总门面（支持 Tier 1 显式英文 -> Tier 2 词典分词 -> Tier 3 异步 LLM 预译未锁定项回填 -> Tier 4 音节兜底）
 */
public final class DeterministicNamingFacade {

    private final SymbolLockTable lockTable;
    private final DomainGlossaryTokenizer tokenizer;
    private final Map<String, List<String>> llmPreTranslatedCache = new LinkedHashMap<>();

    public DeterministicNamingFacade(SymbolLockTable lockTable, DomainGlossaryTokenizer tokenizer) {
        this.lockTable = lockTable;
        this.tokenizer = tokenizer;
    }

    public String resolveEntityClassName(String entityId, String explicitEncode, String zhLabel, String suffix) {
        String scope = "CLASS";
        String lockKey = scope + ":" + entityId + ":" + suffix;
        if (lockTable.isLocked(lockKey)) {
            return lockTable.getLocked(lockKey).orElseThrow();
        }
        SanitizedLabel sanitized = selectTierInput(entityId, explicitEncode, zhLabel, null);
        List<String> words = resolveWords(entityId, sanitized.coreChineseOrEnText());
        String base = AlibabaScopeSymbolGuard.formatUpperCamelClass(words, sanitized.trailingNumberToken(), suffix);
        String unique = deduplicateInScope(scope, base);
        lockTable.lockIfAbsent(lockKey, unique);
        return unique;
    }

    public String resolveFieldName(String ownerTypeId, String fieldEntityId, String explicitEncode,
                                   String zhLabel, String alias, boolean isBoolean) {
        String scope = "FIELD_" + ownerTypeId;
        String lockKey = scope + ":" + fieldEntityId;
        if (lockTable.isLocked(lockKey)) {
            return lockTable.getLocked(lockKey).orElseThrow();
        }
        String strippedZh = (isBoolean && zhLabel != null && zhLabel.startsWith("是否") && zhLabel.length() > 2)
                ? zhLabel.substring(2)
                : zhLabel;
        SanitizedLabel sanitized = selectTierInput(fieldEntityId, explicitEncode, strippedZh, alias);
        List<String> words = resolveWords(fieldEntityId, sanitized.coreChineseOrEnText());
        String base = AlibabaScopeSymbolGuard.formatLowerCamelMember(words, sanitized.trailingNumberToken(), isBoolean);
        String unique = deduplicateInScope(scope, base);
        lockTable.lockIfAbsent(lockKey, unique);
        return unique;
    }

    /**
     * Tier 3 大模型异步预译回填（严格遵守 NP-NAM-001 第 4 条：只作用于尚未写进锁文件的符号，已锁定符号绝不改名！）
     */
    public boolean backfillFromAsyncLlm(String entityId, String scope, String suffixOrEmpty, List<String> translatedEnglishWords) {
        String lockKey = suffixOrEmpty == null || suffixOrEmpty.isEmpty()
                ? scope + ":" + entityId
                : scope + ":" + entityId + ":" + suffixOrEmpty;
        if (lockTable.isLocked(lockKey)) {
            return false; // 已锁定符号拒绝覆盖，100% 保证稳定性
        }
        llmPreTranslatedCache.put(entityId, List.copyOf(translatedEnglishWords));
        return true;
    }

    /**
     * 枚举状态项 / 定值常量名解析（全大写下划线蛇形，非法时确定性回退并带序号消歧）
     */
    public String resolveEnumConstant(String zhLabel, int fallbackIdx) {
        SanitizedLabel sanitized = IdentifierSanitizer.sanitize(zhLabel, null);
        List<String> words = resolveWords(zhLabel, sanitized.coreChineseOrEnText());
        return AlibabaScopeSymbolGuard.formatUpperSnakeConstant(words, fallbackIdx > 0 ? String.valueOf(fallbackIdx + 1) : "");
    }

    private SanitizedLabel selectTierInput(String entityId, String explicitEncode, String zhLabel, String alias) {
        // Tier 1：若建模人员手工填写了有意义的合法英文词（排除存量自动生成的 wb1 / dz+hex 乱码），优先采用
        if (explicitEncode != null && explicitEncode.matches("^[A-Za-z][A-Za-z0-9_]{3,}$")
                && !explicitEncode.matches("^(dz|wb|gccy|dxlx)[0-9a-fA-F]+$")) {
            return IdentifierSanitizer.sanitize(explicitEncode, alias);
        }
        return IdentifierSanitizer.sanitize(zhLabel, alias);
    }

    private List<String> resolveWords(String entityId, String cleanedText) {
        if (llmPreTranslatedCache.containsKey(entityId)) {
            return llmPreTranslatedCache.get(entityId);
        }
        return tokenizer.tokenize(cleanedText);
    }

    private String deduplicateInScope(String scope, String baseCandidate) {
        String candidate = baseCandidate;
        int seq = 2;
        while (lockTable.containsSymbolInScope(scope, candidate)) {
            candidate = baseCandidate + seq++;
        }
        return candidate;
    }
}
