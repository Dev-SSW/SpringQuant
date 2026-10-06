package io.github.devssw.springquant.indicator.supertrend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WilderMovingAverageTest {
    @Test
    void RMA계산() {
        // 들어올 TrueRange 값 주입
        double[] values = {
                20.0,
                11.0,
                17.0,
                20.0
        };

        // 값 리스트, period
        double[] result = WilderMovingAverage.calculate(values, 3);

        // 나올 결과 리스트 갯수는 4개
        assertEquals(4, result.length);
        // 처음 두 개는 계산값 없음
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));

        // 최초 RMA = (20 + 11 + 17) / 3 = 16
        // 다음 RMA = 16 + (20 - 16) / 3 => 52.0 / 3.0
        assertEquals(16.0, result[2], 1e-12);
        assertEquals(52.0 / 3.0, result[3], 1e-12);
    }

    @Test
    void 모든TrueRange같을때() {
        double[] values = {
                10.0,
                10.0,
                10.0,
                10.0
        };

        double[] result = WilderMovingAverage.calculate(values, 3);
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
        assertEquals(10.0, result[2], 1e-12);
        assertEquals(10.0, result[3], 1e-12);
    }

    @Test
    void 충분하지않은개수() {
        double[] values = {
                10.0,
                20.0
        };

        double[] result = WilderMovingAverage.calculate(values, 3);

        assertEquals(2, result.length);
        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
    }

    @Test
    void period_예외() {
        double[] values = {
                10.0
        };

        assertThrows(IllegalArgumentException.class, () -> WilderMovingAverage.calculate(values, 0));
    }
}