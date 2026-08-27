package io.github.devssw.springquant.indicator;

import java.util.Arrays;
import java.util.Objects;

// ATR (TrueRange의 급격한 변동성을 평균내어 변동성을 부드럽게 표현하기 위함)
public final class WilderMovingAverage {
    private WilderMovingAverage() {
    }
    // values에 들어오는 값은 TrueRange의 값들
    public static double[] calculate(double[] values, int period) {
        Objects.requireNonNull(values, "values가 Null이어서는 안됩니다.");
        if (period <= 0) {
            throw new IllegalArgumentException("기간은 반드시 0보다 커야합니다.");
        }

        double[] result = new double[values.length];
        Arrays.fill(result, Double.NaN);

        // 데이터 개수가 기간보다 작을 경우 계산 불가능
        if (values.length < period) {
            return result;
        }

        // period개 만큼의 단순 평균을 계산 (기간이 3이면 계산 가능 첫 위치 인덱스는 2)
        // 최초 RMA (값의 급격한 변화를 부드럽게 만드는 이동 평균 방식) 계산
        double initialSum = 0.0;
        for (int i = 0; i < period; i++) {
            initialSum += values[i];
        }
        // 이전 RMA가 없으므로, 단순 평균으로 계산
        double previousAverage = initialSum / period;

        // 계산한 결과를 알맞은 인덱스 위치의 결과에 주입
        result[period - 1] = previousAverage;

        // 이후 값부터 Wilder 방식으로 계산 (현재 RMA 계산 방법 = 이전 RMA + (현재 TrueRange 값 - 이전 RMA) / period
        for (int i = period; i < values.length; i++) {
            double currentAverage = previousAverage + (values[i] - previousAverage) / period;
            result[i] = currentAverage;
            previousAverage = currentAverage;
        }

        return result;
    }
}
