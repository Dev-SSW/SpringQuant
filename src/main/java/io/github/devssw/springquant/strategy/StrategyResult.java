package io.github.devssw.springquant.strategy;

public record StrategyResult (
        long signalCandleTimestamp,
        TradingSignal signal,
        double superTrend,
        double bollingerBasis,
        double bollingerWidthPercent
) {
}
