package io.github.devssw.springquant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.strategy")
public record StrategyProperties(
        int supertrendLength,
        double supertrendFactor,
        String entryMode,
        boolean oneEntryPerTrend,
        double maTolerancePct,
        double bbTolerancePct,
        double bbWidthMinPct,
        double stopLossPct,
        String stopPromotion,
        int leverage,
        double marginUsage
) {
}
