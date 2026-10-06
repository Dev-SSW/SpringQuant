package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.backtest.model.RiskManagementConfig;
import io.github.devssw.springquant.backtest.model.StopPromotionMode;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionEntryValidatorTest {
    private final PositionEntryValidator validator = new PositionEntryValidator();

    private RiskManagementConfig riskManagement() {
        return new RiskManagementConfig(10.0, List.of(new TakeProfitLevel(50.0, 1.0)), StopPromotionMode.NONE);
    }

    private OpenPosition longPosition(double margin) {
        return new OpenPosition(1000L, PositionSide.LONG, 100.0, 10.0, margin, 0.0, riskManagement());
    }

    @Test
    void 강제청산가가손절가보다가까우면진입을거부한다() {
        OpenPosition position = longPosition(50.0);
        LiquidationConfig config = new LiquidationConfig(0.0, 0.0, true, 0.0);

        assertFalse(validator.canEnter(position, config));
    }

    @Test
    void 손절가가강제청산가보다가까우면진입을허용한다() {
        OpenPosition position = longPosition(200.0);
        LiquidationConfig config = new LiquidationConfig(0.0, 0.0, true, 0.0);

        assertTrue(validator.canEnter(position, config));
    }

    @Test
    void 진입거부설정이꺼져있으면진입을허용한다() {
        OpenPosition position = longPosition(50.0);
        LiquidationConfig config = new LiquidationConfig(0.0, 0.0, false, 0.0);

        assertTrue(validator.canEnter(position, config));
    }

    @Test
    void 안전거리까지충족해야진입을허용한다() {
        OpenPosition position = longPosition(105.0);
        LiquidationConfig noBuffer = new LiquidationConfig(0.0, 0.0, true, 0.0);
        LiquidationConfig withBuffer = new LiquidationConfig(0.0, 0.0, true, 1.0);

        assertTrue(validator.canEnter(position, noBuffer));
        assertFalse(validator.canEnter(position, withBuffer));
    }
}
