package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// 볼린저밴드 (표준편차 - 상단/하단)
public final class RollingStandardDeviation {
    private RollingStandardDeviation() { }
    public static double[] calculate(List<Candle> candles, int period) {
        /**
         * 각 종가에서의 해당 구간의 이동평균 빼서 편차 구함 ->
         * 각 편차를 제곱 (그냥 더하면 양/음 상쇄) ->
         * 제곱의 합을 데이터 개수로 나누어 분산 구함 ->
         * 분산에 제곱근 적용하여 표준 편차 계산
         * */

        Objects.requireNonNull(candles, "리스트에 담긴 캔들이 Null이어서는 안됩니다.");
        if (period <= 0) {
            throw new IllegalArgumentException("기간은 반드시 0보다 커야합니다.");
        }

        // 이동 평균 계산 (표준 편차 계산을 위해)
        double[] movingAverages = SimpleMovingAverage.calculate(candles, period);

        // 초기 결과를 NaN으로 채웁니다 (기본은 0이므로 없는 값이 사용 될 수 있으므로 NaN로 채우기)
        double[] result = new double[candles.size()];
        Arrays.fill(result, Double.NaN);

        // 계산할 수 없는 초기 구간을 제외하기 위해 period-1 부터 시작 (기간이 3이면 계산 가능한 첫 인덱스는 2, 기간이 20이면 계산 가능한 첫 인덱스는 19)
        for (int i = period - 1; i < candles.size(); i++) {
            // 편차 합계
            double differenceSum = 0.0;

            // period를 3으로 가정하면, 시작 인덱스 0 -> 1 -> 2 -> ...
            int windowStartIndex = i - period + 1;
            // period를 3으로 가정하면, 한 칸씩 이동하며 그 사이의 인덱스를 조회 (0,1,2 -> 1,2,3 -> 2,3,4 -> ...)
            for (int j = windowStartIndex; j <= i; j++) {
                // i : 현재 어느 캔들 시점의 표준 편차를 계산하는지
                // j : 현재 이동 구간에 포함된 캔들을 하나씩 읽는 인덱스
                // j번째 인덱스 종가 가져오기
                double close = candles.get(j).close();
                // j번째 인덱스 종가 - i번째 인덱스 평균이동값 -> 편차
                double difference = close - movingAverages[i];

                // 양수와 음수가 더해지면 상쇄되므로, 제곱 후 더함
                differenceSum += difference * difference;
            }

            // 분산 계산 (데이터의 갯수로 나눔, period)
            double variance = differenceSum / period;
            // 표준 편차 계산 (제곱근 적용)
            result[i] = Math.sqrt(variance);
        }

        return result;
    }
}
