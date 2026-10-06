package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.backtest.model.RiskManagementConfig;
import io.github.devssw.springquant.backtest.model.StopPromotionMode;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StopPromotionProcessorTest {
    private static final double DELTA = 1e-9;
    private final StopPromotionProcessor processor = new StopPromotionProcessor();

    private RiskManagementConfig riskManagement(StopPromotionMode mode) {
        return new RiskManagementConfig(10.0,
                List.of(
                        new TakeProfitLevel(10.0, 0.5),
                        new TakeProfitLevel(20.0, 0.5)
                ),
                mode
        );
    }

    private OpenPosition longPosition(StopPromotionMode mode) {
        return new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, 100.0, 0.0, riskManagement(mode));
    }

    @Test
    void 익절체결시손절가승격을예약한다() {
        OpenPosition position = longPosition(StopPromotionMode.BE_AFTER_TP1);
        // 승격 예약을 확인합니다
        processor.scheduleAfterTakeProfit(position, 0);

        assertTrue(position.hasPendingStopPromotion());
        assertEquals(0, position.getPendingStopPromotionTakeProfitIndex());

        // 익절이 체결된 현재 캔들에서는 손절가를 변경하지 않습니다.
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
    }

    @Test
    void 다음캔들시가에서손절가를승격한다() {
        OpenPosition position = longPosition(StopPromotionMode.BE_AFTER_TP1);
        processor.scheduleAfterTakeProfit(position, 0);
        processor.tryApplyPendingAtOpen(position, 105.0);

        assertEquals(100.0, position.getCurrentStopPrice(), DELTA);
        assertFalse(position.hasPendingStopPromotion());
    }

    @Test
    void 시가가승격가를침범하면승격을보류한다() {
        OpenPosition position = longPosition(StopPromotionMode.BE_AFTER_TP1);
        processor.scheduleAfterTakeProfit(position, 0);

        // 롱 포지션의 승격 후보는 진입가 100입니다.
        // 시가 99는 이미 후보 가격 아래이므로 승격하지 않습니다.
        processor.tryApplyPendingAtOpen(position, 99.0);

        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
        assertTrue(position.hasPendingStopPromotion());
    }

    @Test
    void 보류된승격은다음캔들에서다시시도한다() {
        OpenPosition position = longPosition(StopPromotionMode.BE_AFTER_TP1);
        processor.scheduleAfterTakeProfit(position, 0);

        // 첫 번째 시도는 시가가 승격가 아래이므로 보류됩니다.
        processor.tryApplyPendingAtOpen(position, 99.0);

        // 다음 캔들의 시가가 승격가보다 위에 있으므로 적용됩니다.
        processor.tryApplyPendingAtOpen(position, 105.0);

        assertEquals(100.0, position.getCurrentStopPrice(), DELTA);
        assertFalse(position.hasPendingStopPromotion());
    }

    @Test
    void 여러익절이체결되면가장높은단계를예약한다() {
        OpenPosition position = longPosition(StopPromotionMode.BE_TP1_AFTER_TP2);

        processor.scheduleAfterTakeProfit(position, 0);
        processor.scheduleAfterTakeProfit(position, 1);

        assertEquals(1, position.getPendingStopPromotionTakeProfitIndex());

        // TP2 체결 후 손절가 후보는 TP1 가격인 110입니다.
        processor.tryApplyPendingAtOpen(position, 120.0);

        assertEquals(110.0, position.getCurrentStopPrice(), DELTA);
        assertFalse(position.hasPendingStopPromotion());
    }

    @Test
    void 승격방식이없으면예약하지않는다() {
        OpenPosition position = longPosition(StopPromotionMode.NONE);
        processor.scheduleAfterTakeProfit(position, 0);

        assertFalse(position.hasPendingStopPromotion());
        assertEquals(90.0, position.getCurrentStopPrice(), DELTA);
    }

}