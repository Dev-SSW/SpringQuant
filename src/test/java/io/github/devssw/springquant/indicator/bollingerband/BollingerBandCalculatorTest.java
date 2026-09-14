package io.github.devssw.springquant.indicator.bollingerband;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BollingerBandCalculatorTest {
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
    void 볼린저밴드계산() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 20.0),
                candle(2, 30.0)
        );

        // 볼린저밴드에는 표준편차 배수 적용
        List<BollingerBand> result = BollingerBandCalculator.calculate(candles, 3, 2.0);
        // 결과 개수는 3개
        assertEquals(3, result.size());
        // 앞의 두 결과는 유한한 숫자
        assertFalse(result.get(0).isAvailable());
        assertFalse(result.get(1).isAvailable());

        BollingerBand band = result.get(2);

        // 단순 평균 이동
        double expectedBasis = 20.0;
        // 표준 편차 (종가 - 이동 평균 -> 제곱 -> 분산 -> 제곱근)
        double expectedStandardDeviation = Math.sqrt(200.0 / 3.0);
        // 편차 범위 (표준 편차 * multiplier )
        double expectedDeviation = expectedStandardDeviation * 2.0;
        // 상단 / 하단
        double expectedUpper = expectedBasis + expectedDeviation;
        double expectedLower = expectedBasis - expectedDeviation;
        // 밴드 폭 : (상단값 - 하단값) / 이동평균값 * 100
        double expectedWidthPercent = (expectedUpper - expectedLower) / expectedBasis * 100.0;

        assertTrue(band.isAvailable());

        // 평균 이동과 상단/하단, 밴드 폭 검증 (허용 오차 포함)
        assertEquals(expectedBasis, band.basis(), 1e-12);
        assertEquals(expectedUpper, band.upper(), 1e-12);
        assertEquals(expectedLower, band.lower(), 1e-12);
        assertEquals(expectedWidthPercent, band.widthPercent(), 1e-12);
    }

    @Test
    void 모든종가가같을때() {
        List<Candle> candles = List.of(
                candle(0, 10.0),
                candle(1, 10.0),
                candle(2, 10.0)
        );

        List<BollingerBand> result = BollingerBandCalculator.calculate(candles, 3, 2.0);
        BollingerBand band = result.get(2);

        // 종가가 같으면 밴드폭은 0이 되어야 함
        assertTrue(band.isAvailable());
        assertEquals(10.0, band.basis(), 1e-12);
        assertEquals(10.0, band.upper(), 1e-12);
        assertEquals(10.0, band.lower(), 1e-12);
        assertEquals(0.0, band.widthPercent(), 1e-12);
    }

    @Test
    void multiplier_예외() {
        List<Candle> candles = List.of(
                candle(0, 10.0)
        );

        assertThrows(IllegalArgumentException.class, () -> BollingerBandCalculator.calculate(candles, 3, 0.0));
    }
}
