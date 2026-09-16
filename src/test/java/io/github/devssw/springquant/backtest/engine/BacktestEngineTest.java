package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.*;
import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {
    // 허용 오차
    private static final double DELTA = 1e-9;
    private final BacktestEngine engine = new BacktestEngine();

    private Candle candle(long index, double open, double high, double low, double close) {
        return new Candle(index * 1000L, open, high, low, close);
    }

    private BacktestConfig config() {
        return config(inactiveRiskManagement());
    }

    // 청산 관리 설정
    private BacktestConfig config(RiskManagementConfig riskManagement) {
        return new BacktestConfig(10000.0, 0.0, 1, 0.1, 2, 2.0, 0.0, 2, 0.1, EntryMode.ST_FLIP, 0.0, 0.0, true, riskManagement);
    }

    // 기존 ST 테스트에서 청산관리설정이 개입하지 않도록 만들어 둔 설정 (손절, 익절 조건을 굉장히 멀리 둠)
    private RiskManagementConfig inactiveRiskManagement() {
        return new RiskManagementConfig(50.0, List.of(new TakeProfitLevel(50.0, 1.0)), false, .0);
    }

    // 청산관리설정을 동작하도록 만든 설정 (첫 번째 익절에는 절반 청산, 두 번째는 닿지 못하도록)
    private RiskManagementConfig partialTakeProfitRiskManagement() {
        return new RiskManagementConfig(50.0, List.of(new TakeProfitLevel(1.0, 0.5), new TakeProfitLevel(50.0, 0.5)), false, 0.0);
    }

    @Test
    void 캔들하나만주입() {
        List<Candle> candles = List.of(candle(0, 100.0, 101.0, 99.0, 100.0));
        BacktestResult result = engine.run(candles, config());

        // 캔들 하나이므로 거래 수 0, 잔고 그대로, 최종 잔고 그대로, 수익률 0 이어야 함
        assertEquals(0, result.totalTrades());
        assertEquals(10000.0, result.initialBalance(), DELTA);
        assertEquals(10000.0, result.finalBalance(), DELTA);
        assertEquals(0.0, result.totalReturnPercent(), DELTA);
    }

    @Test
    void ST추세변화시() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 101.0, 99.0, 100.0),
                candle(1, 100.0, 101.0, 99.0, 100.0),
                candle(2, 100.0, 100.0, 89.0, 90.0),
                candle(3, 90.0, 111.0, 89.0, 110.0),
                candle(4, 110.0, 111.0, 109.0, 110.0)
        );

        // ST Length와 Factor는 각각 2, 0.1로 설정하여 급격한 변화를 만듦
        BacktestResult result = engine.run(candles, config());
        // 인덱스 1 -> 인덱스 2 캔들에서 가격 하락하여 SHORT 신호 -> 인덱스 3 캔들에서 SHORT진입
        // 인덱스 2 -> 인덱스 3 캔들에서 가격 상승하여 LONG 신호 -> 인덱스 4 캔들에서 거래 종료
        // 인덱스 4 캔들에서 LONG 진입 -> 마지막 캔들에서 청산
        assertEquals(2, result.totalTrades());

        // 잔고 10000, 사용 비율 10%, 레버리지 1, 수수료 0%으로 위에서 설정했음
        BacktestTrade shortTrade = result.trades().get(0);
        assertEquals(PositionSide.SHORT, shortTrade.side());
        assertEquals(90.0, shortTrade.entryPrice(), DELTA);
        assertEquals(110.0, shortTrade.exitPrice(), DELTA);
        assertEquals(ExitReason.SUPER_TREND, shortTrade.exitReason());
        assertEquals(1, shortTrade.exits().size());

        BacktestTrade longTrade = result.trades().get(1);
        assertEquals(PositionSide.LONG, longTrade.side());
        assertEquals(110.0, longTrade.entryPrice(), DELTA);
        assertEquals(110.0, longTrade.exitPrice(), DELTA);
        assertEquals(ExitReason.END_OF_DATA, longTrade.exitReason());
        assertEquals(1, longTrade.exits().size());

        // 예상되는 최종 잔고
        double expectedFinalBalance = 10000.0 - 20.0 * (1000.0 / 90.0);
        assertEquals(expectedFinalBalance, result.finalBalance(), DELTA);
    }

    @Test
    void 일부익절후나머지수량을ST전환으로청산() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 101.0, 99.0, 100.0),
                candle(1, 100.0, 101.0, 99.0, 100.0),
                candle(2, 100.0, 100.0, 89.0, 90.0),
                candle(3, 90.0, 111.0, 89.0, 110.0),
                candle(4, 110.0, 111.0, 109.0, 110.0)
        );

        BacktestResult result = engine.run(candles, config(partialTakeProfitRiskManagement()));
        BacktestTrade shortTrade = result.trades().get(0);

        // 첫 번째 분할 익절과 ST 청산이 기록되어야 함
        assertEquals(2, shortTrade.exits().size());
        BacktestExit takeProfitExit = shortTrade.exits().get(0);
        BacktestExit superTrendExit = shortTrade.exits().get(1);

        double halfQuantity = shortTrade.quantity() * 0.5;

        assertEquals(ExitReason.TAKE_PROFIT, takeProfitExit.exitReason());
        assertEquals(89.1, takeProfitExit.exitPrice(), DELTA);
        assertEquals(halfQuantity, takeProfitExit.quantity(), DELTA);

        assertEquals(ExitReason.SUPER_TREND, superTrendExit.exitReason());
        assertEquals(110.0, superTrendExit.exitPrice(), DELTA);
        assertEquals(halfQuantity, superTrendExit.quantity(), DELTA);

        // 두 청산 가격을 수량에 따라 평균 낸 결과입니다.
        assertEquals(99.55, shortTrade.exitPrice(), DELTA);
    }

    @Test
    void 시가갭손절이SuperTrend청산보다우선한다() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 101.0, 99.0, 100.0),
                candle(1, 100.0, 101.0, 99.0, 100.0),
                candle(2, 100.0, 100.0, 89.0, 90.0),
                candle(3, 90.0, 111.0, 89.0, 110.0),
                candle(4, 140.0, 141.0, 139.0, 140.0)
        );

        BacktestResult result = engine.run(candles, config(inactiveRiskManagement()));
        assertEquals(1, result.totalTrades());

        BacktestTrade trade = result.trades().get(0);
        assertEquals(PositionSide.SHORT, trade.side());
        assertEquals(ExitReason.STOP_LOSS, trade.exitReason());
        assertEquals(140.0, trade.exitPrice(), DELTA);
    }
}