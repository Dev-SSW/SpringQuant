package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.result.BacktestResult;

import java.util.List;
import java.util.Objects;

// 백테스트 API가 화면에 반환하는 전체 결과입니다.
public record BacktestResponse(
        String symbol,
        String interval,
        long startTimeMs,
        long endTimeMs,
        List<BacktestChartPoint> chartPoints,
        BacktestResult result
) {
    public BacktestResponse {
        Objects.requireNonNull(symbol, "symbol이 null이면 안됩니다.");
        Objects.requireNonNull(interval, "interval이 null이면 안됩니다.");
        Objects.requireNonNull(chartPoints, "차트 데이터가 null이면 안됩니다.");
        Objects.requireNonNull(result, "백테스트 결과가 null이면 안됩니다.");

        chartPoints = List.copyOf(chartPoints);
    }
}
