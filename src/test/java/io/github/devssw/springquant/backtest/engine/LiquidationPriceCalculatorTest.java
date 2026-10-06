package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.BacktestTestFixtures;
import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiquidationPriceCalculatorTest {
    private static final double DELTA = 1e-9;
    private final LiquidationPriceCalculator calculator = new LiquidationPriceCalculator();

    @Test
    void 롱강제청산가격계산() {
        double liquidationPrice = calculator.calculate(PositionSide.LONG, 100.0, 10.0, 100.0, BacktestTestFixtures.liquidationConfig());
        double expected = (10.0 * 100.0 - 100.0) / (10.0 * (1.0 - 0.01));

        assertEquals(expected, liquidationPrice, DELTA);
    }

    @Test
    void 숏강제청산가격계산() {
        double liquidationPrice = calculator.calculate(PositionSide.SHORT, 100.0, 10.0, 100.0, BacktestTestFixtures.liquidationConfig());
        double expected = (10.0 * 100.0 + 100.0) / (10.0 * (1.0 + 0.01));

        assertEquals(expected, liquidationPrice, DELTA);
    }

    @Test
    void 레버리지가유효하지않으면예외() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(PositionSide.LONG, 100.0, 0.0, 100.0, BacktestTestFixtures.liquidationConfig()));
    }

    @Test
    void 유지증거금을반영한롱강제청산가격계산() {
        LiquidationConfig config = BacktestTestFixtures.liquidationConfig();
        double result = calculator.calculate(PositionSide.LONG, 100.0, 10.0, 100.0, config);
        double expected = (10.0 * 100.0 - 100.0) / (10.0 * (1.0 - 0.01));

        assertEquals(expected, result, DELTA);
    }

    @Test
    void 유지증거금을반영한숏강제청산가격계산() {
        LiquidationConfig config = BacktestTestFixtures.liquidationConfig();
        double result = calculator.calculate(PositionSide.SHORT, 100.0, 10.0, 100.0, config);
        double expected = (10.0 * 100.0 + 100.0) / (10.0 * (1.0 + 0.01));

        assertEquals(expected, result, DELTA);
    }
}
