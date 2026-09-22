package io.github.devssw.springquant.backtest.result;

import java.util.List;
import java.util.Objects;

// MDD를 계산합니다 (최대 낙폭)
public final class DrawdownCalculator {
    private DrawdownCalculator() { }

    public static double calculateMaxDrawdownPercent(List<EquityPoint> equityCurve) {
        Objects.requireNonNull(equityCurve, "자산 곡선이 null이면 안됩니다.");

        if (equityCurve.isEmpty()) {
            return 0.0;
        }

        /** 초기화 */
        // 최고 잔고를 기록
        double peakBalance = equityCurve.get(0).balance();
        // 가장 큰 하락률 기록
        double maxDrawdownPercent = 0.0;
        // 이전 시각
        long previousTimeMs = -1L;

        for (EquityPoint point : equityCurve) {
            Objects.requireNonNull(point, "자산 곡선에 null이 포함될 수 없습니다.");

            if (point.timeMs() < previousTimeMs) {
                throw new IllegalArgumentException("자산 곡선은 시간순으로 정렬되어야 합니다.");
            }

            // 현재 시각 저장
            previousTimeMs = point.timeMs();

            // 새로운 최고 잔고 확인
            if (point.balance() > peakBalance) {
                peakBalance = point.balance();
            }

            // 0으로 나누지 않기 위한 조건문
            if (peakBalance <= 0.0) {
                continue;
            }

            // 현재 낙폭 = (현재까지 최고 잔고 - 현재 잔고) / (현재까지 최고 잔고 * 100)
            double drawdownPercent = (peakBalance - point.balance()) / peakBalance * 100.0;
            // 최고 낙폭 vs 현재 낙폭
            maxDrawdownPercent = Math.max(maxDrawdownPercent, drawdownPercent);
        }

        return maxDrawdownPercent;
    }
}