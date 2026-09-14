package io.github.devssw.springquant.indicator.supertrend;

import io.github.devssw.springquant.market.Candle;

import java.util.List;

// ATR (TrueRange와 WilderMovingAverage (RMA 계산) 활용)
public final class AverageTrueRange {
    private AverageTrueRange() {
    }

    public static double[] calculate(List<Candle> candles, int period) {
        double[] trueRanges = TrueRange.calculate(candles);
        double[] atr = WilderMovingAverage.calculate(trueRanges, period);
        return atr;
    }
}
