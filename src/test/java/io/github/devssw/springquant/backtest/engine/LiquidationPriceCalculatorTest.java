package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiquidationPriceCalculatorTest {
    private static final double DELTA = 1e-9;
    private final LiquidationPriceCalculator calculator = new LiquidationPriceCalculator();

    @Test
    void 롱강제청산가격계산() {
        double liquidationPrice = calculator.calculate(PositionSide.LONG, 100.0, 10);
        assertEquals(90.0, liquidationPrice, DELTA);
    }

    @Test
    void 숏강제청산가격계산() {
        double liquidationPrice = calculator.calculate(PositionSide.SHORT, 100.0, 10);
        assertEquals(110.0, liquidationPrice, DELTA);
    }

    @Test
    void 레버리지가유효하지않으면예외() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(PositionSide.LONG, 100.0, 0));
    }
}