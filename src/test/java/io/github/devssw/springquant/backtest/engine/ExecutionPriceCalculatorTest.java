package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionPriceCalculatorTest {
    private static final double DELTA = 1e-9;
    private final ExecutionPriceCalculator calculator = new ExecutionPriceCalculator();

    @Test
    void 진입슬리피지() {
        double longPrice = calculator.entryPrice(PositionSide.LONG, 100.0, 0.01);
        double shortPrice = calculator.entryPrice(PositionSide.SHORT, 100.0, 0.01);

        assertEquals(101.0, longPrice, DELTA);
        assertEquals(99.0, shortPrice, DELTA);
    }

    @Test
    void 청산슬리피지() {
        double longPrice = calculator.exitPrice(PositionSide.LONG, 100.0, 0.01, ExitReason.STOP_LOSS);
        double shortPrice = calculator.exitPrice(PositionSide.SHORT, 100.0, 0.01, ExitReason.STOP_LOSS);

        assertEquals(99.0, longPrice, DELTA);
        assertEquals(101.0, shortPrice, DELTA);
    }

    @Test
    void 강제청산에는슬리피지를적용하지않는다() {
        double price = calculator.exitPrice(PositionSide.LONG, 100.0, 0.01, ExitReason.LIQUIDATION);

        assertEquals(100.0, price, DELTA);
    }
}