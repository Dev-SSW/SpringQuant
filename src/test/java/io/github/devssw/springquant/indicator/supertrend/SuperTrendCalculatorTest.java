package io.github.devssw.springquant.indicator.supertrend;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuperTrendCalculatorTest {
    // 허용 오차
    private static final double TOLERANCE = 1e-12;

    private void assertUnavailable(SuperTrend result) {
        assertFalse(result.isAvailable());
        assertTrue(Double.isNaN(result.atr()));
        assertTrue(Double.isNaN(result.value()));
        assertTrue(Double.isNaN(result.upperBand()));
        assertTrue(Double.isNaN(result.lowerBand()));
        assertEquals(SuperTrend.Direction.UNAVAILABLE, result.direction());
        assertEquals(SuperTrend.Signal.NONE, result.signal());
        assertEquals(-1, result.trendId());
    }

    private void assertInitialUpTrend(SuperTrend result) {
        assertTrue(result.isAvailable());
        assertEquals(10.0, result.atr(), TOLERANCE);
        assertEquals(115.0, result.upperBand(), TOLERANCE);
        assertEquals(95.0, result.lowerBand(), TOLERANCE);
        assertEquals(95.0, result.value(), TOLERANCE);
        assertEquals(SuperTrend.Direction.UP, result.direction());
        assertEquals(SuperTrend.Signal.NONE, result.signal());
        assertEquals(0, result.trendId());
    }

    private void assertMaintainedUpTrend(SuperTrend result) {
        assertEquals(10.0, result.atr(), TOLERANCE);
        assertEquals(115.0, result.upperBand(), TOLERANCE);
        assertEquals(100.0, result.lowerBand(), TOLERANCE);
        assertEquals(100.0, result.value(), TOLERANCE);
        assertEquals(SuperTrend.Direction.UP, result.direction());
        assertEquals(SuperTrend.Signal.NONE, result.signal());
        assertEquals(0, result.trendId());
    }

    private void assertShortTransition(SuperTrend result) {
        assertEquals(15.5, result.atr(), TOLERANCE);
        assertEquals(111.5, result.upperBand(), TOLERANCE);
        assertEquals(100.0, result.lowerBand(), TOLERANCE);
        assertEquals(111.5, result.value(), TOLERANCE);
        assertEquals(SuperTrend.Direction.DOWN, result.direction());
        assertEquals(SuperTrend.Signal.SHORT, result.signal());
        assertEquals(1, result.trendId());
    }

    private void assertMaintainedDownTrend(SuperTrend result) {
        assertEquals(12.75, result.atr(), TOLERANCE);
        assertEquals(105.75, result.upperBand(), TOLERANCE);
        assertEquals(80.25, result.lowerBand(), TOLERANCE);
        assertEquals(105.75, result.value(), TOLERANCE);
        assertEquals(SuperTrend.Direction.DOWN, result.direction());
        assertEquals(SuperTrend.Signal.NONE, result.signal());
        assertEquals(1, result.trendId());
    }

    private void assertLongTransition(SuperTrend result) {
        assertEquals(16.375, result.atr(), TOLERANCE);
        assertEquals(105.75, result.upperBand(), TOLERANCE);
        assertEquals(88.625, result.lowerBand(), TOLERANCE);
        assertEquals(88.625, result.value(), TOLERANCE);
        assertEquals(SuperTrend.Direction.UP, result.direction());
        assertEquals(SuperTrend.Signal.LONG, result.signal());
        assertEquals(2, result.trendId());
    }

    private List<Candle> sampleCandles() {
        return List.of(
                candle(0, 100.0, 105.0, 95.0, 100.0),
                candle(1, 101.0, 110.0, 100.0, 109.0),
                candle(2, 108.0, 115.0, 105.0, 112.0),
                candle(3, 100.0, 101.0, 91.0, 95.0),
                candle(4, 95.0, 98.0, 88.0, 90.0),
                candle(5, 102.0, 110.0, 100.0, 108.0)
        );
    }

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
    void  SuperTrend동작테스트() {
        List<Candle> candles = sampleCandles();
        // 적은 수의 캔들로 테스트해보기 위해서 period를 2로 설정하였음
        List<SuperTrend> result = SuperTrendCalculator.calculate(candles, 2, 1.0);

        // 결과 길이 확인
        assertEquals(6, result.size());
        // 첫 번째 캔들은 기간 2가 채워지지 않았으므로 계산 불가 상태여야 함
        assertUnavailable(result.get(0));
        // 두 번째 캔들은 최초 ST가 상승 추세로 초기화 됐어야 함 (종가 상승으로 캔들을 넣어뒀으므로)
        assertInitialUpTrend(result.get(1));
        // 세 번째 캔들은 상승이 유지되고 있는지, 하단선이 상승했는지 확인
        assertMaintainedUpTrend(result.get(2));
        // 네 번째 캔들은 종가가 하단선보다 낮아 SHORT 전환이 발생했는지 확인 (방향은 DOWN)
        assertShortTransition(result.get(3));
        // 다섯 번째 캔들은 하락 추세가 유지인지 확인
        assertMaintainedDownTrend(result.get(4));
        // 여섯 번째 캔들은 종가가 상단선보다 높아 LONG 전환이 발생했는지 확인 (방향은 UP)
        assertLongTransition(result.get(5));
    }

    @Test
    void 충분하지않은캔들개수_ATR계산불가() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 105.0, 95.0, 100.0),
                candle(1, 100.0, 110.0, 100.0, 109.0)
        );

        List<SuperTrend> result = SuperTrendCalculator.calculate(candles, 3, 1.0);

        assertEquals(2, result.size());
        assertUnavailable(result.get(0));
        assertUnavailable(result.get(1));
    }

    @Test
    void 잘못된ATR기간_예외처리() {
        List<Candle> candles = sampleCandles();

        assertThrows(IllegalArgumentException.class, () -> SuperTrendCalculator.calculate(candles, 0, 1.0));
    }

    @Test
    void 잘못된ATR배수_예외처리() {
        List<Candle> candles = sampleCandles();

        assertThrows(IllegalArgumentException.class, () -> SuperTrendCalculator.calculate(candles, 2, 0.0));
        assertThrows(IllegalArgumentException.class, () -> SuperTrendCalculator.calculate(candles, 2, Double.NaN));
    }

    @Test
    void 캔들리스트가NULL_예외처리() {
        assertThrows(NullPointerException.class, () -> SuperTrendCalculator.calculate(null, 2, 1.0));
    }
}