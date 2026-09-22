package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.ExitReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BacktestExitTest {
    @Test
    void 실현손익계산() {
        BacktestExit exit = new BacktestExit(2000L, 110.0, 2.0, 20.0, 0.5, ExitReason.TAKE_PROFIT);
        assertEquals(19.5, exit.realizedPnl(), 1e-12);
    }

    @Test
    void 수량예외() {
        assertThrows(IllegalArgumentException.class, () -> new BacktestExit(2000L, 110.0, 0.0, 0.0, 0.0, ExitReason.TAKE_PROFIT));
    }
}
