package io.github.devssw.springquant.indicator.bollingerband;

import io.github.devssw.springquant.market.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 볼린저밴드 계산
public final class BollingerBandCalculator {
    private BollingerBandCalculator() {
    }
    /**
     * @param candles   시간순으로 정렬된 캔들 목록
     * @param period    이동평균과 표준편차 계산 기간
     * @param multiplier 표준편차 배수
     * @return 각 캔들에 대응하는 볼린저밴드 목록
     */

    public static List<BollingerBand> calculate(List<Candle> candles, int period, double multiplier) {
        Objects.requireNonNull(candles, "리스트에 담긴 캔들이 Null이어서는 안됩니다.");
        if (period <= 0) {
            throw new IllegalArgumentException("기간은 반드시 0보다 커야합니다.");
        }

        if (!Double.isFinite(multiplier) || multiplier <= 0.0) {
            throw new IllegalArgumentException("표준편차 배수는 유한한 숫자여야하고, 0보다 커야합니다.");
        }

        // 평균 이동
        double[] movingAverages = SimpleMovingAverage.calculate(candles, period);
        // 표준 편차
        double[] standardDeviations = RollingStandardDeviation.calculate(candles, period);
        List<BollingerBand> result = new ArrayList<>(candles.size());

        for (int i = 0; i < candles.size(); i++) {
            // i번째의 이동평균값과 표준편차값을 담은 변수 각각 생성
            double basis = movingAverages[i];
            double standardDeviation = standardDeviations[i];

            // 각 변수는 유한한 수여야 함.
            if (!Double.isFinite(basis) || !Double.isFinite(standardDeviation)) {
                // 유한한 수가 아닌 경우 NaN 처리를 하는 함수 호출 후 결과 리스트에 추가
                // 3이라면 첫번째와 두번째 위치의 인덱스는 모든 값이 NaN 처리가 됨
                result.add(BollingerBand.unavailable());
                continue;
            }

            // 편차 범위 : 표준편차 * 표준편차 배수
            double deviation = standardDeviation * multiplier;
            // 상단값 : 이동평균값 + 편차 범위
            double upper = basis + deviation;
            // 하단값 : 이동평균값 - 편차 범위
            double lower = basis - deviation;
            // 밴드 폭 : (상단값 - 하단값) / 이동평균값 * 100
            double widthPercent = (upper - lower) / basis * 100.0;

            // 해당 위치의 볼린저밴드 값 결과 리스트에 추가
            result.add(new BollingerBand(basis, upper, lower, widthPercent));
        }
        // 결과 리스트의 복사본 제공
        return List.copyOf(result);
    }
}
