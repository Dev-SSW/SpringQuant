package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageTest {
    // 종가로 고정
    private Candle candle(long index, double close) {
        return new Candle(
                index * 1000L,
                close,
                close,
                close,
                close
        );
    }

    @Test
    void 평균이동계산() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0),
                candle(2, 30.0),
                candle(3, 40.0),
                candle(4, 50.0)
        );

        // period를 3으로
        double[] result = SimpleMovingAverage.calculate(candles, 3);
        assertEquals(5, result.length);

        // 5개이므로 1, 2번째는 NaN이어야 함
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
        // 허용 오차
        assertEquals(20.0, result[2], 1e-12);
        assertEquals(30.0, result[3], 1e-12);
        assertEquals(40.0, result[4], 1e-12);
    }

    @Test
    void 계산되지않은캔들() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0)
        );

        double[] result = SimpleMovingAverage.calculate(candles, 3);

        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
    }

    @Test
    void period_1일때() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0),
                candle(2, 30.0)
        );

        // period를 1로
        double[] result = SimpleMovingAverage.calculate(candles, 1);

        assertEquals(10.0, result[0], 1e-12);
        assertEquals(20.0, result[1], 1e-12);
        assertEquals(30.0, result[2], 1e-12);
    }

    @Test
    void period_예외() {
        List<Candle> candles = List.of(
                candle(0, 10.0)
        );

        assertThrows(IllegalArgumentException.class, () -> SimpleMovingAverage.calculate(candles, 0));
    }
}
