package io.github.devssw.springquant.backtest.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawdownCalculatorTest {
    private static final double DELTA = 1e-9;

    @Test
    void 자산곡선에서최대낙폭을계산() {
        List<EquityPoint> equityCurve = List.of(
                new EquityPoint(1000L, 10000.0),
                new EquityPoint(2000L, 11000.0),
                new EquityPoint(3000L, 8800.0),
                new EquityPoint(4000L, 12000.0),
                new EquityPoint(5000L, 10800.0)
        );

        double result = DrawdownCalculator.calculateMaxDrawdownPercent(equityCurve);

        assertEquals(20.0, result, DELTA);
    }

    @Test
    void 자산이계속증가하면최대낙폭은0() {
        List<EquityPoint> equityCurve = List.of(
                new EquityPoint(1000L, 10000.0),
                new EquityPoint(2000L, 11000.0),
                new EquityPoint(3000L, 12000.0)
        );

        double result = DrawdownCalculator.calculateMaxDrawdownPercent(equityCurve);

        assertEquals(0.0, result, DELTA);
    }

    @Test
    void 빈자산곡선이면최대낙폭은0() {
        double result = DrawdownCalculator.calculateMaxDrawdownPercent(List.of());

        assertEquals(0.0, result, DELTA);
    }

    @Test
    void 자산곡선이시간순서가아니면예외() {
        List<EquityPoint> equityCurve = List.of(
                new EquityPoint(2000L, 10000.0),
                new EquityPoint(1000L, 9000.0)
        );

        assertThrows(IllegalArgumentException.class, () -> DrawdownCalculator.calculateMaxDrawdownPercent(equityCurve));
    }
}