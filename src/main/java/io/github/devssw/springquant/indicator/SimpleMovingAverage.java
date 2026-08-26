package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// 단순 이동 평균 (캔들의 종가를 더한 후 기간으로 나눈 값)
// 상속 할 수 없는 클래스로 구현
public final class SimpleMovingAverage {
    // 지표 계산이므로 상태가 필요없어 객체가 필요하지 않음
    private SimpleMovingAverage() { }
    // period : 이동 평균을 계산 할 기간
    public static double[] calculate(List<Candle> candles, int period) {
        Objects.requireNonNull(candles, "리스트에 담긴 캔들이 Null이어서는 안됩니다.");
        if (period <= 0) {
            throw new IllegalArgumentException("기간은 반드시 0보다 커야합니다.");
        }

        // 초기 결과를 NaN으로 채웁니다 (기본은 0이므로 없는 값이 사용 될 수 있으므로 NaN로 채우기)
        double[] result = new double[candles.size()];
        Arrays.fill(result, Double.NaN);

        // 슬라이딩 합 계산 ( (캔들1 + 캔들 2 + 캔들 3) / period )
        double rollingSum = 0.0;

        for (int i = 0; i < candles.size(); i++) {
            // 캔들 가져오기
            Candle currentCandle = Objects.requireNonNull(candles.get(i), "candles[" + i + "] 가 null이어서는 안됩니다.");
            // 캔들 종가 더하기
            rollingSum += currentCandle.close();

            // i는 캔들 순번 (오래된 캔들 -> 최신 캔들 순서)
            // i가 기간보다 커지면, 오래된 캔들은 합계에서 지우고 최신 캔들까지만 합계에 더한 후 기간으로 나눠서 평균을 구해야함
            // -> 오래된 캔들 종가를 합계에서 제거하기
            if (i >= period) {
                rollingSum -= candles.get(i - period).close();
            }

            // 기간이 20이라면, 계산 가능한 첫 인덱스는 19
            if (i >= period - 1) {
                result[i] = rollingSum / period;
            }
        }
        // 각 캔들 별 이동평균선의 값을 저장
        return result;
    }
}
