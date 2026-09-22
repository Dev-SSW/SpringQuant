package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEventFactoryTest {
    private static final double DELTA = 1e-9;

    private BacktestTrade tradeWithPartialExits() {
        BacktestExit takeProfitExit = new BacktestExit(2000L, 110.0, 4.0, 40.0, 0.4, ExitReason.TAKE_PROFIT);
        BacktestExit stopLossExit = new BacktestExit(3000L, 95.0, 6.0, -30.0, 0.3, ExitReason.STOP_LOSS);

        return new BacktestTrade(
                1000L,
                PositionSide.LONG,
                100.0,
                10.0,
                1.0,
                List.of(
                        takeProfitExit,
                        stopLossExit
                ),
                10008.3
        );
    }

    @Test
    void 거래내역을시간순이벤트로변환한다() {
        BacktestTrade trade = tradeWithPartialExits();
        List<BacktestEvent> events = BacktestEventFactory.create(List.of(trade));

        assertEquals(3, events.size());

        BacktestEvent entryEvent = events.get(0);

        assertEquals(BacktestEventType.ENTRY, entryEvent.type());
        assertEquals(1000L, entryEvent.timeMs());
        assertEquals(PositionSide.LONG, entryEvent.side());
        assertEquals(100.0, entryEvent.price(), DELTA);
        assertEquals(10.0, entryEvent.quantity(), DELTA);

        BacktestEvent takeProfitEvent = events.get(1);

        assertEquals(BacktestEventType.TAKE_PROFIT, takeProfitEvent.type());
        assertEquals(2000L, takeProfitEvent.timeMs());
        assertEquals(110.0, takeProfitEvent.price(), DELTA);
        assertEquals(4.0, takeProfitEvent.quantity(), DELTA);

        BacktestEvent stopLossEvent = events.get(2);

        assertEquals(BacktestEventType.STOP_LOSS, stopLossEvent.type());
        assertEquals(3000L, stopLossEvent.timeMs());
        assertEquals(95.0, stopLossEvent.price(), DELTA);
        assertEquals(6.0, stopLossEvent.quantity(), DELTA);
    }

    @Test
    void 백테스트결과에이벤트를포함한다() {
        // 실제로 이벤트 목록이 결과에 포함되는지 확인
        BacktestTrade trade = tradeWithPartialExits();
        BacktestResult result = BacktestResult.from(10000.0, List.of(trade));

        assertEquals(1, result.totalTrades());
        assertEquals(3, result.events().size());

        assertEquals(BacktestEventType.ENTRY, result.events().get(0).type());
        assertEquals(BacktestEventType.TAKE_PROFIT, result.events().get(1).type());
        assertEquals(BacktestEventType.STOP_LOSS, result.events().get(2).type());
    }

    @Test
    void 각청산사유를이벤트타입으로변환한다() {
        // 각 청산 사유로 메세지가 올바르게 변환되는지 확인
        assertExitEventType(ExitReason.TAKE_PROFIT, BacktestEventType.TAKE_PROFIT);
        assertExitEventType(ExitReason.STOP_LOSS, BacktestEventType.STOP_LOSS);
        assertExitEventType(ExitReason.SUPER_TREND, BacktestEventType.SUPER_TREND_EXIT);
        assertExitEventType(ExitReason.LIQUIDATION, BacktestEventType.LIQUIDATION);
        assertExitEventType(ExitReason.END_OF_DATA, BacktestEventType.END_OF_DATA_EXIT);
    }

    private void assertExitEventType(ExitReason exitReason, BacktestEventType expectedType) {
        BacktestExit exit = new BacktestExit(2000L, 105.0, 10.0, 50.0, 0.0, exitReason);
        BacktestTrade trade = new BacktestTrade(1000L, PositionSide.LONG, 100.0, 10.0, 0.0, List.of(exit), 10050.0);
        List<BacktestEvent> events = BacktestEventFactory.create(List.of(trade));

        assertEquals(2, events.size());
        assertEquals(expectedType, events.get(1).type());
    }
}
