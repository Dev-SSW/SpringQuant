package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEventTest {
    @Test
    void 백테스트이벤트를생성한다() {
        BacktestEvent event = new BacktestEvent(
                1000L,
                BacktestEventType.ENTRY,
                PositionSide.LONG,
                100.0,
                5.0,
                "LONG 포지션에 진입했습니다."
        );

        assertEquals(1_000L, event.timeMs());
        assertEquals(BacktestEventType.ENTRY, event.type());
        assertEquals(PositionSide.LONG, event.side());
        assertEquals(100.0, event.price());
        assertEquals(5.0, event.quantity());
        assertEquals("LONG 포지션에 진입했습니다.", event.message());
    }

    @Test
    void 수량이음수이면예외가발생한다() {
        assertThrows(IllegalArgumentException.class, () -> new BacktestEvent(
                1000L,
                BacktestEventType.TAKE_PROFIT,
                PositionSide.LONG,
                100.0,
                -1.0,
                "익절이 체결되었습니다.")
        );
    }

    @Test
    void 메시지가비어있으면예외가발생한다() {
        assertThrows(IllegalArgumentException.class, () -> new BacktestEvent(
                1000L,
                BacktestEventType.STOP_PROMOTION,
                PositionSide.LONG,
                100.0,
                0.0,
                " ")
        );
    }
}
