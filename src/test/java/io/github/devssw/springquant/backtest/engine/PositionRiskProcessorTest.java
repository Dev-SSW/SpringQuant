package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.*;
import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class PositionRiskProcessorTest {
    private static final double DELTA = 1e-9;
    private final PositionRiskProcessor processor = new PositionRiskProcessor();

    // 위험 관리 설정
    private RiskManagementConfig riskManagement() {
        // 손절 10%로 설정됨 (익절은 50%, 수량 전체 (1.0))
        return new RiskManagementConfig(10.0, List.of(new TakeProfitLevel(50.0, 1.0)), false, 0.0);
    }

    @Test
    void 롱포지션손절() {
        // 시작 시간, 포지션 방향, 진입가, 진입 수량, 진입 수수료, 위험 관리
        // 진입가 100에 진입 수량은 10 => 총 가치는 1000, 손절 비율이 10%이므로 손절이 발생 할 시 -100의 손해가 생김 (아래의 숏 테스트도 동일한 조건)
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 0.0, riskManagement());
        Candle candle = new Candle(2000L, 100.0, 105.0, 89.0, 95.0);
        double realizedPnl = processor.process(position, candle, 0.0);
        assertTrue(position.isClosed());
        assertEquals(-100.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);
        assertEquals(90.0, exit.exitPrice(), DELTA);
        assertEquals(10.0, exit.quantity(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
    }

    @Test
    void 숏포지션손절() {
        OpenPosition position = new OpenPosition(1000L, PositionSide.SHORT, 100.0, 10.0, 0.0, riskManagement());
        Candle candle = new Candle(2000L, 100.0, 111.0, 95.0, 105.0);
        double realizedPnl = processor.process(position, candle, 0.0);
        assertTrue(position.isClosed());
        assertEquals(-100.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);
        assertEquals(110.0, exit.exitPrice(), DELTA);
        assertEquals(10.0, exit.quantity(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
    }
}