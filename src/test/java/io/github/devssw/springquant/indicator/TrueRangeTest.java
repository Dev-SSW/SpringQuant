package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrueRangeTest {
    // 이전에는 종가만 필요해서 고정하여 생성했지만, TrueRange는 OHLC 모두가 필요하기에 분리가 필요함
    private Candle candle(
            long index,
            double open,
            double high,
            double low,
            double close
    ) {
        return new Candle(
                index * 1_000L,
                open,
                high,
                low,
                close
        );
    }

    @Test
    void TrueRange계산() {
        List<Candle> candles = List.of(
                candle(0, 100.0, 110.0, 90.0, 105.0),
                candle(1, 105.0, 112.0, 101.0, 108.0),
                candle(2, 116.0, 125.0, 115.0, 120.0),
                candle(3, 110.0, 112.0, 100.0, 105.0)
        );

        double[] result = TrueRange.calculate(candles);

        // Ex) result[0] : 110 - 90 = 20, result[1] : 112 - 101 | 112 - 105 | 101 - 105 중 가장 큰 수
        assertArrayEquals(new double[]{20.0, 11.0, 17.0, 20.0}, result, 1e-12);
    }
}