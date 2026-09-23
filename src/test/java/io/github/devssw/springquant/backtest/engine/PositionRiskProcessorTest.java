package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.BacktestTestFixtures;
import io.github.devssw.springquant.backtest.model.*;
import io.github.devssw.springquant.backtest.result.BacktestExit;
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
        return new RiskManagementConfig(10.0, List.of(new TakeProfitLevel(50.0, 1.0)), StopPromotionMode.NONE);
    }

    @Test
    void 롱포지션손절() {
        // 시작 시간, 포지션 방향, 진입가, 진입 수량, 진입 수수료, 위험 관리
        // 진입가 100에 진입 수량은 10 => 총 가치는 1000, 손절 비율이 10%이므로 손절이 발생 할 시 -100의 손해가 생김 (아래의 숏 테스트도 동일한 조건)
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 1000.0, 0.0, riskManagement());
        Candle candle = new Candle(2000L, 100.0, 105.0, 89.0, 95.0);
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, BacktestTestFixtures.liquidationConfig());
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
        OpenPosition position = new OpenPosition(1000L, PositionSide.SHORT, 100.0, 10.0, 1000.0, 0.0, riskManagement());
        Candle candle = new Candle(2000L, 100.0, 111.0, 95.0, 105.0);
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, BacktestTestFixtures.liquidationConfig());
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
        RiskManagementConfig riskManagement = new RiskManagementConfig(
                10.0,
                List.of(new TakeProfitLevel(5.0, 0.5), new TakeProfitLevel(50.0, 0.5)),
                StopPromotionMode.BE_AFTER_TP1
        );

        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 1000.0, 0.0, riskManagement);

        // 고가가 본전 손절 승격 기준인 105에 도달
        Candle triggerCandle = new Candle(2000L, 100.0, 105.0, 91.0, 105.0);
        double firstPnl = processor.process(position, triggerCandle, 0.0, 0.0, BacktestTestFixtures.liquidationConfig());

        assertEquals(25.0, firstPnl, DELTA);
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
        assertTrue(position.hasPendingStopPromotion());
        assertEquals(0, position.getPendingStopPromotionTakeProfitIndex());
        assertFalse(position.isClosed());

        // 다음 캔들에서 변경된 손절가 100에 도달
        Candle stopCandle = new Candle(3000L, 104.0, 104.0, 99.0, 100.0);
        double secondPnl = processor.process(position, stopCandle, 0.0, 0.0, BacktestTestFixtures.liquidationConfig());

        assertEquals(0.0, secondPnl, DELTA);
        assertTrue(position.isClosed());
        assertEquals(2, position.getExits().size());

        BacktestExit takeProfitExit = position.getExits().get(0);
        assertEquals(105.0, takeProfitExit.exitPrice(), DELTA);
        assertEquals(5.0, takeProfitExit.quantity(), DELTA);
        assertEquals(ExitReason.TAKE_PROFIT, takeProfitExit.exitReason());

        BacktestExit exit = position.getExits().get(1);

        assertEquals(100.0, exit.exitPrice(), DELTA);
        assertEquals(5.0, exit.quantity(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
    }

    @Test
    void 손절가보다강제청산가가먼저도달하면강제청산() {
        // 손절가는 50%로 가격이 50이 되면 손절이지만, LiquidationPriceCalculator 클래스에서 계산된 청산가는 90이 되므로 청산가가 먼저 도달하도록 설정
        RiskManagementConfig riskManagement = new RiskManagementConfig(50.0, List.of(new TakeProfitLevel(50.0, 1.0)), StopPromotionMode.NONE);
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 100.0, 0.0, riskManagement);
        Candle candle = new Candle(2000L, 100.0, 101.0, 89.0, 90.0);

        double realizedPnl = processor.process(position, candle, 0.0, 0.0, BacktestTestFixtures.liquidationConfig());

        assertTrue(position.isClosed());
        double expectedExitPrice = (10.0 * 100.0 - 100.0) / (10.0 * (1.0 - 0.01));
        double expectedPnl = (expectedExitPrice - 100.0) * 10.0;

        assertEquals(expectedPnl, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);
        assertEquals(expectedExitPrice, exit.exitPrice(), DELTA);
        assertEquals(ExitReason.LIQUIDATION, exit.exitReason());
    }

    @Test
    void 부분청산후남은증거금을계산한다() {
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 100.0, 0.0, riskManagement());
        position.closeQuantity(2000L, 110.0, 4.0, 0.0, ExitReason.TAKE_PROFIT);

        assertEquals(6.0, position.getRemainingQuantity(), DELTA);
        assertEquals(60.0, position.getRemainingMargin(), DELTA);
    }

    // ======================== 손절가 승격이 연결되었는지 테스트합니다 ====================== //
    @Test
    void 익절후다음캔들에서승격된손절가로청산한다() {
        RiskManagementConfig riskManagement = new RiskManagementConfig(
                10.0,
                List.of(
                        new TakeProfitLevel(10.0, 0.5),
                        new TakeProfitLevel(20.0, 0.5)
                ),
                StopPromotionMode.BE_AFTER_TP1
        );

        LiquidationConfig liquidationConfig = new LiquidationConfig(0.0, 0.0, false, 0.0);
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 100.0, 0.0, riskManagement);

        // TP1 가격 110에 도달하지만 기존 손절가 90에는 도달하지 않습니다.
        Candle takeProfitCandle = new Candle(2000L, 100.0, 111.0, 95.0, 110.0);

        double firstRealizedPnl = processor.process(position, takeProfitCandle, 0.0, 0.0, liquidationConfig);

        // 초기 수량 10의 절반인 5가 TP1 가격 110에서 청산됩니다.
        assertEquals(50.0, firstRealizedPnl, DELTA);
        assertEquals(5.0, position.getRemainingQuantity(), DELTA);

        // TP 체결 봉에서는 아직 기존 손절가를 유지합니다.
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
        assertTrue(position.hasPendingStopPromotion());

        // 다음 캔들은 105에서 시작하고 저가는 99까지 내려갑니다.
        Candle nextCandle = new Candle(3000L, 105.0, 106.0, 99.0, 100.0);

        // 시가 105가 승격 후보 100을 침범하지 않았으므로
        // 손절가가 진입가 100으로 승격됩니다.
        double openSafetyPnl = processor.processOpenSafetyExit(position, nextCandle, 0.0, 0.0, liquidationConfig);

        assertEquals(0.0, openSafetyPnl, DELTA);
        assertEquals(100.0, position.getCurrentStopPrice(), DELTA);
        assertFalse(position.hasPendingStopPromotion());

        // 같은 캔들의 저가 99가 승격된 손절가 100에 도달하므로
        // 남은 수량이 본절 가격에서 청산됩니다.
        double secondRealizedPnl = processor.process(position, nextCandle, 0.0, 0.0, liquidationConfig);

        assertEquals(0.0, secondRealizedPnl, DELTA);
        assertTrue(position.isClosed());
        assertEquals(2, position.getExits().size());

        BacktestExit takeProfitExit = position.getExits().get(0);
        BacktestExit stopLossExit = position.getExits().get(1);

        assertEquals(ExitReason.TAKE_PROFIT, takeProfitExit.exitReason());
        assertEquals(110.0, takeProfitExit.exitPrice(), DELTA);
        assertEquals(5.0, takeProfitExit.quantity(), DELTA);
        assertEquals(ExitReason.STOP_LOSS, stopLossExit.exitReason());
        assertEquals(100.0, stopLossExit.exitPrice(), DELTA);
        assertEquals(5.0, stopLossExit.quantity(), DELTA);
    }

    // ============================= 캔들에서 우선순위에 따른 청산이 되는지 테스트 ====================== //
    @Test
    void 같은캔들에서손절과익절에도달하면손절을우선한다() {
        RiskManagementConfig riskManagement = new RiskManagementConfig(
                10.0,
                List.of(new TakeProfitLevel(10.0, 1.0)),
                StopPromotionMode.NONE
        );

        LiquidationConfig liquidationConfig = new LiquidationConfig(0.0, 0.0, false, 0.0);
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 500.0, 0.0, riskManagement);

        /*
         * 손절가: 90
         * 익절가: 110
         *
         * 저가가 89이므로 손절 가능
         * 고가가 111이므로 익절 가능
         */
        Candle candle = new Candle(2000L, 100.0, 111.0, 89.0, 100.0);
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, liquidationConfig);

        assertTrue(position.isClosed());
        assertEquals(-100.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);

        assertEquals(ExitReason.STOP_LOSS, exit.exitReason());
        assertEquals(90.0, exit.exitPrice(), DELTA);
        assertEquals(10.0, exit.quantity(), DELTA);
        assertFalse(position.hasPendingStopPromotion());
    }

    @Test
    void 같은캔들에서강제청산과익절에도달하면강제청산을우선한다() {
        RiskManagementConfig riskManagement = new RiskManagementConfig(
                10.0,
                List.of(new TakeProfitLevel(10.0, 1.0)),
                StopPromotionMode.NONE
        );

        LiquidationConfig liquidationConfig = new LiquidationConfig(0.0, 0.0, false, 0.0);
        OpenPosition position = new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 50.0, 0.0, riskManagement);

        /*
         * 포지션 가치: 100 × 10 = 1,000
         * 격리 증거금: 50
         * 강제청산가: (1,000 - 50) / 10 = 95
         *
         * 손절가: 90
         * 익절가: 110
         */
        Candle candle = new Candle(2000L, 100.0, 111.0, 94.0, 100.0);
        double realizedPnl = processor.process(position, candle, 0.0, 0.0, liquidationConfig);

        assertTrue(position.isClosed());
        assertEquals(-50.0, realizedPnl, DELTA);
        assertEquals(1, position.getExits().size());

        BacktestExit exit = position.getExits().get(0);

        assertEquals(ExitReason.LIQUIDATION, exit.exitReason());
        assertEquals(95.0, exit.exitPrice(), DELTA);
        assertEquals(10.0, exit.quantity(), DELTA);
        assertFalse(position.hasPendingStopPromotion());
    }

    // 기존 코드에서는 processOpenSafetyExit() 호출 이후, process() 내부에서 processOpenSafetyExit()이 한 번 더 호출되었음
    // 첫 번째에서 승격 성공시 예약이 삭제되므로, 두 번째는 아무 작업도 하지 않는 것과 마찬가지이므로 삭제 -> 테스트 코드로 검증
    @Test
    void process는예약된손절가승격을처리하지않는다() {
        RiskManagementConfig riskManagement = new RiskManagementConfig(
                10.0,
                List.of(
                        new TakeProfitLevel(10.0, 0.5),
                        new TakeProfitLevel(20.0, 0.5)
                ),
                StopPromotionMode.BE_AFTER_TP1
        );

        LiquidationConfig liquidationConfig = new LiquidationConfig(0.0, 0.0, false, 0.0);

        OpenPosition position = new OpenPosition(
                1000L,
                PositionSide.LONG,
                100.0,
                10.0,
                100.0,
                0.0,
                riskManagement
        );

        // TP1 체결 후 손절가 승격이 예약됩니다.
        Candle takeProfitCandle = new Candle(2000L, 100.0, 111.0, 95.0, 110.0);

        processor.process(
                position,
                takeProfitCandle,
                0.0,
                0.0,
                liquidationConfig
        );

        assertTrue(position.hasPendingStopPromotion());
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);

        // 다음 캔들의 내부 위험관리만 실행합니다.
        Candle nextCandle = new Candle(3000L, 105.0, 106.0, 101.0, 104.0);

        processor.process(
                position,
                nextCandle,
                0.0,
                0.0,
                liquidationConfig
        );

        // process()가 시가 승격을 중복 처리하면 안 됩니다.
        assertTrue(position.hasPendingStopPromotion());
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
    }
}
