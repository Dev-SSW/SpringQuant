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
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, 1);
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
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, 1);
        assertTrue(position.isClosed());
        assertEquals(-100.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);
        assertEquals(110.0, exit.exitPrice(), DELTA);
        assertEquals(10.0, exit.quantity(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
    }

    @Test
    void 수익구간도달후다음캔들에서본전손절() {
        RiskManagementConfig riskManagement = new RiskManagementConfig(10.0, List.of(new TakeProfitLevel(50.0, 1.0)), true, 5.0);

        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 0.0, riskManagement);

        // 고가가 본전 손절 승격 기준인 105에 도달
        Candle triggerCandle = new Candle(2000L, 100.0, 106.0, 95.0, 105.0);
        double firstPnl = processor.process(position, triggerCandle, 0.0, 0.0, 1);

        assertEquals(0.0, firstPnl, DELTA);
        assertEquals(100.0, position.getCurrentStopPrice(), DELTA);
        assertFalse(position.isClosed());

        // 다음 캔들에서 변경된 손절가 100에 도달
        Candle stopCandle = new Candle(3000L, 105.0, 106.0, 99.0, 100.0);
        double secondPnl = processor.process(position, stopCandle, 0.0, 0.0, 1);

        assertEquals(0.0, secondPnl, DELTA);
        assertTrue(position.isClosed());
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);

        assertEquals(100.0, exit.exitPrice(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
    }

    @Test
    void 손절가보다강제청산가가먼저도달하면강제청산() {
        // 손절가는 50%로 가격이 50이 되면 손절이지만, LiquidationPriceCalculator 클래스에서 계산된 청산가는 90이 되므로 청산가가 먼저 도달하도록 설정
        RiskManagementConfig riskManagement = new RiskManagementConfig(50.0, List.of(new TakeProfitLevel(50.0, 1.0)), false, 0.0);
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 0.0, riskManagement);
        Candle candle = new Candle(2000L, 100.0, 101.0, 89.0, 90.0);

        double realizedPnl = processor.process(position, candle, 0.0, 0.0, 10);

        assertTrue(position.isClosed());
        assertEquals(-100.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);
        assertEquals(90.0, exit.exitPrice(), DELTA);
        assertEquals(ExitReason.LIQUIDATION, exit.exitReason());
    }
}
