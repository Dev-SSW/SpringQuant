package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquityCurveCalculatorTest {
    private static final double DELTA = 1e-9;

    private BacktestTrade trade(
            long entryTimeMs,
            long firstExitTimeMs,
            long secondExitTimeMs,
            double balanceAfter
    ) {
        BacktestExit takeProfitExit = new BacktestExit(firstExitTimeMs, 110.0, 4.0, 40.0, 2.0,ExitReason.TAKE_PROFIT);
        BacktestExit stopLossExit = new BacktestExit(secondExitTimeMs, 95.0, 6.0, -30.0, 1.0, ExitReason.STOP_LOSS);

        return new BacktestTrade(
                entryTimeMs,
                PositionSide.LONG,
                100.0,
                10.0,
                10.0,
                List.of(takeProfitExit, stopLossExit),
                balanceAfter
        );
    }

    @Test
    void 진입수수료와부분청산손익으로자산곡선을계산() {
        BacktestTrade trade = trade(1000L, 2000L, 3000L, 9997.0);

        List<EquityPoint> result = EquityCurveCalculator.calculate(0L, 10000.0, List.of(trade));

        assertEquals(4, result.size());
        assertEquals(0L, result.get(0).timeMs());
        assertEquals(10000.0, result.get(0).balance(), DELTA);
        assertEquals(1000L, result.get(1).timeMs());
        assertEquals(9990.0, result.get(1).balance(), DELTA);
        assertEquals(2000L, result.get(2).timeMs());
        assertEquals(10028.0, result.get(2).balance(), DELTA);
        assertEquals(3000L, result.get(3).timeMs());
        assertEquals(9997.0, result.get(3).balance(), DELTA);
    }

    @Test
    void 같은시각의잔고변화는마지막값만() {
        BacktestTrade trade = trade(1000L, 1000L, 1000L, 9997.0);
        List<EquityPoint> result = EquityCurveCalculator.calculate(0L, 10000.0, List.of(trade));

        assertEquals(2, result.size());
        assertEquals(0L, result.get(0).timeMs());
        assertEquals(10000.0, result.get(0).balance(), DELTA);
        assertEquals(1000L, result.get(1).timeMs());
        assertEquals(9997.0, result.get(1).balance(), DELTA);
    }

    @Test
    void 기록된최종잔고가계산결과와다르면예외발생() {
        BacktestTrade trade = trade(1000L, 2000L, 3000L, 9998.0);

        assertThrows(IllegalStateException.class, () -> EquityCurveCalculator.calculate(0L, 10000.0, List.of(trade)));
    }

    @Test
    void 거래가없으면시작잔고만반환() {
        List<EquityPoint> result = EquityCurveCalculator.calculate(1000L, 10000.0, List.of());

        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).timeMs());
        assertEquals(10000.0, result.get(0).balance(), DELTA);
    }
}