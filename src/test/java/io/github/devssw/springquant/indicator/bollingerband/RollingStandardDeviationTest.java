package io.github.devssw.springquant.indicator.bollingerband;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RollingStandardDeviationTest {
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
    void 표준편차계산() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0),
                candle(2, 30.0),
                candle(3, 40.0)
        );

        double[] result = RollingStandardDeviation.calculate(candles, 3);
        // 표준편차 미리 계산 (period가 3일 때)
        double expected = Math.sqrt(200.0 / 3.0);

        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
        // 허용 오차
        assertEquals(expected, result[2], 1e-12);
        assertEquals(expected, result[3], 1e-12);
    }

    @Test
    void 모든종가가같을때() {
        List<Candle> candles = List.of(
                candle(0, 100.0),
                candle(1, 100.0),
                candle(2, 100.0)
        );

        double[] result = RollingStandardDeviation.calculate(candles, 3);
        // 모든 종가가 같을 때 표준편차가 0이 되는지 확인 (300/3 한 후 각 종가에서 이동평균을 빼서 편차를 계산하므로, 0이 되어야 함)
        assertEquals(0.0, result[2], 1e-12);
    }

    @Test
    void 계산되지않은캔들() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0)
        );

        double[] result = RollingStandardDeviation.calculate(candles, 3);

        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
    }

    @Test
    void period_예외() {
        List<Candle> candles = List.of(
                candle(0, 10.0)
        );

        assertThrows(IllegalArgumentException.class, () -> RollingStandardDeviation.calculate(candles, 0));
    }
}
