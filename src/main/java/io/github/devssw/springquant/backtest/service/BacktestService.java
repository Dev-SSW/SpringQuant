package io.github.devssw.springquant.backtest.service;

import io.github.devssw.springquant.backtest.api.BacktestChartPoint;
import io.github.devssw.springquant.backtest.api.BacktestResponse;
import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.engine.BacktestEngine;
import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.indicator.bollingerband.BollingerBand;
import io.github.devssw.springquant.indicator.bollingerband.BollingerBandCalculator;
import io.github.devssw.springquant.indicator.supertrend.SuperTrend;
import io.github.devssw.springquant.indicator.supertrend.SuperTrendCalculator;
import io.github.devssw.springquant.market.Candle;
import io.github.devssw.springquant.market.candledata.MarketDataClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 마켓 캔들 데이터와 백테스트 엔진을 끌어와 백테스트를 진행합니다
@Service
public class BacktestService {
    private final MarketDataClient marketDataClient;
    private final BacktestEngine backtestEngine;

    public BacktestService(MarketDataClient marketDataClient, BacktestEngine backtestEngine) {
        this.marketDataClient = marketDataClient;
        this.backtestEngine = backtestEngine;
    }

    public BacktestResponse run(String symbol, String interval, long startTimeMs, long endTimeMs, BacktestConfig config) {
        Objects.requireNonNull(config, "config가 null이면 안됩니다.");
        // 차트 데이터를 끌어옵니다
        List<Candle> candles = marketDataClient.fetchCandles(symbol, interval, startTimeMs, endTimeMs);

        if (candles.isEmpty()) {
            throw new IllegalArgumentException("조회된 캔들 데이터가 없습니다.");
        }
        // 거래를 실행한 후 결과를 받습니다
        BacktestResult result = backtestEngine.run(candles, config);

        // 볼린저 밴드 계산
        List<BollingerBand> bands = BollingerBandCalculator.calculate(
                candles,
                config.bollingerPeriod(),
                config.bollingerMultiplier()
        );

        // 슈퍼 트렌드 계산
        List<SuperTrend> superTrends = SuperTrendCalculator.calculate(
                candles,
                config.superTrendLength(),
                config.superTrendFactor()
        );

        // 캔들과 지표를 같은 인덱스로 결합
        List<BacktestChartPoint> chartPoints = createChartPoints(
                candles,
                bands,
                superTrends
        );

        // 결과값 반환
        return new BacktestResponse(
                symbol,
                interval,
                startTimeMs,
                endTimeMs,
                chartPoints,
                result
        );
    }

    // 캔들과 지표를 같은 인덱스로 결합
    private List<BacktestChartPoint> createChartPoints(
            List<Candle> candles,
            List<BollingerBand> bands,
            List<SuperTrend> superTrends
    ) {
        if (candles.size() != bands.size() || candles.size() != superTrends.size()) {
            throw new IllegalStateException("캔들과 지표 결과의 개수가 일치하지 않습니다.");
        }

        List<BacktestChartPoint> chartPoints = new ArrayList<>(candles.size());

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            BollingerBand band = bands.get(i);
            SuperTrend superTrend = superTrends.get(i);

            chartPoints.add(new BacktestChartPoint(
                    candle.openTimeMs(),
                    candle.open(),
                    candle.high(),
                    candle.low(),
                    candle.close(),
                    availableBasis(band),
                    availableUpper(band),
                    availableLower(band),
                    availableSuperTrendValue(superTrend),
                    availableSuperTrendDirection(superTrend)
                    )
            );
        }

        return List.copyOf(chartPoints);
    }

    /** ======== 지표값 반환 메서드 ========  (초기 계산 불가 구간에는 지표값이 NaN이므로 null로 치환해줍니다) */
    private Double availableBasis(BollingerBand band) {
        return band.isAvailable() ? band.basis() : null;
    }

    private Double availableUpper(BollingerBand band) {
        return band.isAvailable() ? band.upper() : null;
    }

    private Double availableLower(BollingerBand band) {
        return band.isAvailable() ? band.lower() : null;
    }

    private Double availableSuperTrendValue(SuperTrend superTrend) {
        return superTrend.isAvailable() ? superTrend.value() : null;
    }

    private String availableSuperTrendDirection(SuperTrend superTrend) {
        return superTrend.isAvailable() ? superTrend.direction().name() : null;
    }
}
