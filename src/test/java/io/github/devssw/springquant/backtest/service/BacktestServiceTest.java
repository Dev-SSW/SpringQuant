package io.github.devssw.springquant.backtest.service;

import io.github.devssw.springquant.backtest.BacktestTestFixtures;
import io.github.devssw.springquant.backtest.api.BacktestChartPoint;
import io.github.devssw.springquant.backtest.api.BacktestResponse;
import io.github.devssw.springquant.backtest.engine.BacktestEngine;
import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.backtest.model.EntryMode;
import io.github.devssw.springquant.market.Candle;
import io.github.devssw.springquant.market.candledata.MarketDataClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BacktestServiceTest {
    @Mock private MarketDataClient marketDataClient;
    @Mock private BacktestEngine backtestEngine;
    private BacktestService backtestService;

    @BeforeEach
    void setUp() {
        backtestService = new BacktestService(marketDataClient, backtestEngine);
    }

    private BacktestConfig createConfig() {
        return new BacktestConfig(10000.0, 0.0004, 0.0, 1, 1.0, 10, 1.0, 0.0, 10, 1.0, EntryMode.MA_BOUNCE, 0.0, 0.0, true, BacktestTestFixtures.riskManagementConfig(), BacktestTestFixtures.liquidationConfig());
    }

    private Candle candle(long index, double price) {
        return new Candle(index * 1000L, price, price, price, price);
    }

    @Test
     void 캔들조회와백테스트실행() {
        BacktestConfig config = createConfig();
        List<Candle> candles = List.of(
                candle(0, 100.0),
                candle(1, 110.0),
                candle(2, 120.0)
        );
        // 예상되는 결과 생성
        BacktestResult expectedResult = BacktestResult.from(config.initialBalance(), List.of());
        when(marketDataClient.fetchCandles("BTCUSDT", "4h", 1000L, 10000L)).thenReturn(candles);
        when(backtestEngine.run(candles, config)).thenReturn(expectedResult);
        // 실제 결과와 비교
        BacktestResponse response = backtestService.run("BTCUSDT", "4h", 1000L, 10000L, config);
        assertEquals("BTCUSDT", response.symbol());
        assertEquals("4h", response.interval());
        assertEquals(1000L, response.startTimeMs());
        assertEquals(10000L, response.endTimeMs());

        assertSame(expectedResult, response.result());
        assertEquals(3, response.chartPoints().size());

        BacktestChartPoint firstPoint = response.chartPoints().get(0);

        assertEquals(0L, firstPoint.timeMs());
        assertEquals(100.0, firstPoint.open());
        assertEquals(100.0, firstPoint.high());
        assertEquals(100.0, firstPoint.low());
        assertEquals(100.0, firstPoint.close());

        // 지표 기간이 10인데 캔들이 3개뿐이므로 계산 불가
        assertNull(firstPoint.bollingerBasis());
        assertNull(firstPoint.bollingerUpper());
        assertNull(firstPoint.bollingerLower());
        assertNull(firstPoint.superTrendValue());
        assertNull(firstPoint.superTrendDirection());

        verify(marketDataClient).fetchCandles("BTCUSDT", "4h", 1000L, 10000L);
        verify(backtestEngine).run(candles, config);
    }

    @Test
    void 빈결과시예외() {
        BacktestConfig config = createConfig();
        when(marketDataClient.fetchCandles("BTCUSDT", "4h", 1000L, 10000L)).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> backtestService.run("BTCUSDT", "4h", 1000L, 10000L, config));
        verifyNoInteractions(backtestEngine);
    }

    @Test
    void 설정없을때예외() {
        assertThrows(NullPointerException.class, () -> backtestService.run("BTCUSDT", "4h", 1000L, 10000L, null));
        verifyNoInteractions(marketDataClient, backtestEngine);
    }
}
