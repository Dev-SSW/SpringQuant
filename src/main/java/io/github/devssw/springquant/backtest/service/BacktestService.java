package io.github.devssw.springquant.backtest.service;

import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.engine.BacktestEngine;
import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.market.Candle;
import io.github.devssw.springquant.market.candledata.MarketDataClient;
import org.springframework.stereotype.Service;

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

    public BacktestResult run(String symbol, String interval, long startTimeMs, long endTimeMs, BacktestConfig config) {
        Objects.requireNonNull(config, "config가 null이면 안됩니다.");
        List<Candle> candles = marketDataClient.fetchCandles(symbol, interval, startTimeMs, endTimeMs);

        if (candles.isEmpty()) {
            throw new IllegalArgumentException("조회된 캔들 데이터가 없습니다.");
        }

        return backtestEngine.run(candles, config);
    }
}
