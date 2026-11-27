package com.groma.studio.naming;

public record SanitizedLabel(
        String coreChineseOrEnText,
        String extractedUnit,
        String trailingNumberToken
) {}
