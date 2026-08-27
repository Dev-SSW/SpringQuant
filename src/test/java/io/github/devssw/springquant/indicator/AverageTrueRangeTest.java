package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AverageTrueRangeTest {
    private Candle candle(
            long index,
            double open,
            double high,
            double low,
            double close
    ) {
        return new Candle(
                index * 1000L,
                open,
                high,
                low,
                close
        );
    }
    @Test
    void ATR계산() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 110.0, 90.0, 105.0),
                candle(1, 105.0, 112.0, 101.0, 108.0),
                candle(2, 116.0, 125.0, 115.0, 120.0),
                candle(3, 110.0, 112.0, 100.0, 105.0)
        );

        double[] result = AverageTrueRange.calculate(candles, 3);
        assertEquals(4, result.length);
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));

        assertEquals(16.0, result[2], 1e-12);
        assertEquals(52.0 / 3.0, result[3], 1e-12);
    }

    @Test
    void period_예외() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 110.0, 90.0, 105.0)
        );

        assertThrows(IllegalArgumentException.class, () -> AverageTrueRange.calculate(candles, 0));
    }
}