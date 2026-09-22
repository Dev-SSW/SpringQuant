package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktestTradeTest {

    @Test
    void 부분청산계산() {
        List<BacktestExit> exits = List.of(
                new BacktestExit(2000L, 110.0, 6.0, 60.0, 0.5, ExitReason.TAKE_PROFIT),
                new BacktestExit(3000L, 90.0, 4.0, -40.0, 0.4, ExitReason.STOP_LOSS)
        );

        BacktestTrade trade = new BacktestTrade(1000L, PositionSide.LONG, 100.0, 10.0, 1.0, exits, 1018.1);

        assertEquals(3000L, trade.exitTimeMs());
        assertEquals(102.0, trade.exitPrice(), 1e-12);
        assertEquals(20.0, trade.grossPnl(), 1e-12);
        assertEquals(0.9, trade.exitFee(), 1e-12);
        assertEquals(1.9, trade.totalFee(), 1e-12);
        assertEquals(18.1, trade.netPnl(), 1e-12);
        assertEquals(ExitReason.STOP_LOSS, trade.exitReason());
        assertTrue(trade.isWin());
    }

    @Test
    void 완전종료되지않은포지션예외() {
        List<BacktestExit> exits = List.of(
                new BacktestExit(2000L, 110.0, 5.0, 50.0, 0.5, ExitReason.TAKE_PROFIT)
        );

        // 포지션 수량이 아직 남아 있으므로, BacktestTrade가 생성되어서는 안됩니다.
        assertThrows(IllegalArgumentException.class, () -> new BacktestTrade(1000L, PositionSide.LONG, 100.0, 10.0, 1.0, exits, 1000.0));
    }
}
