package io.github.devssw.springquant.indicator;

import io.github.devssw.springquant.market.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SuperTrendCalculator {
    private SuperTrendCalculator() {
    }

    // 최초로 계산 가능한 위치 탐색
    private static int findFirstAvailableIndex(double[] atr) {
        // NaN이 아닌 첫번째 계산 가능한 위치 찾기
        for (int i = 0; i < atr.length; i++) {
            if (Double.isFinite(atr[i])) {
                return i;
            }
        }
        return -1;
    }

    // 최초 SuperTrend 초기화 (이전 ST가 없기 때문에, 처음엔 초기화 과정이 필요)
    private static void initializeFirstSuperTrend(List<Candle> candles, double[] atr, double multiplier, List<SuperTrend> result, int startIndex) {
        Candle candle = candles.get(startIndex);
        // 중심 가격 계산
        double middlePrice = (candle.high() + candle.low()) / 2.0;
        // 최초 상단선과 하단선 (표준 편차 배수를 곱한)
        double upperBand = middlePrice + multiplier * atr[startIndex];
        double lowerBand = middlePrice - multiplier * atr[startIndex];

        SuperTrend.Direction direction;
        // 최초 방향 결정 (중심 가격 이상이면 상승 추세로 시작)
        if (candle.close() >= middlePrice) {
            direction = SuperTrend.Direction.UP;
        } else {
            direction = SuperTrend.Direction.DOWN;
        }

        double superTrendValue;
        // 최초 Value 결정 (상승 추세 시 하단선, 하락 추세 시 상단선 선택)
        if (direction == SuperTrend.Direction.UP) {
            superTrendValue = lowerBand;
        } else {
            superTrendValue = upperBand;
        }

        // 최초 추세 전환 신호는 전환으로 볼 수 없기에 NONE으로 설정
        result.set(startIndex, new SuperTrend(atr[startIndex], superTrendValue, upperBand, lowerBand, direction, SuperTrend.Signal.NONE, 0));
    }

    // 최종 상단선
    private static double calculateFinalUpperBand(double basicUpperBand, double previousUpperBand, double previousClose) {
        // 1차 조건 : 이전 상단선이 없다면 현재 상단선을 사용 (초기값이나 예외값)
        // 2차 조건 : 현재 기본 상단선이 이전 상단선보다 낮아졌다면 현재 상단선 사용
        // 3차 조건 : 이전 종가가 이전 상단선을 이미 돌파했다면, 새로운 밴드 구간이 시작된 것으로 보고 현재 상단선 사용
        if (!Double.isFinite(previousUpperBand) || basicUpperBand < previousUpperBand || previousClose > previousUpperBand) {
            return basicUpperBand;
        }
        // 어떤 조건도 만족하지 않을 경우, 이전 상단선을 유지함 (종가와 상단선이 같은 경우 등의 예외 상황)
        return previousUpperBand;
    }
    // 최종 하단선
    private static double calculateFinalLowerBand(double basicLowerBand, double previousLowerBand, double previousClose) {
        if (!Double.isFinite(previousLowerBand) || basicLowerBand > previousLowerBand || previousClose < previousLowerBand) {
            return basicLowerBand;
        }
        return previousLowerBand;
    }

    public static List<SuperTrend> calculate(List<Candle> candles, int period, double multiplier) {
        Objects.requireNonNull(candles, "리스트에 담긴 캔들이 Null이어서는 안됩니다.");
        if (period <= 0) {
            throw new IllegalArgumentException("기간은 반드시 0보다 커야합니다.");
        }

        if (!Double.isFinite(multiplier) || multiplier <= 0.0) {
            throw new IllegalArgumentException("표준편차 배수는 유한한 숫자여야하고, 0보다 커야합니다.");
        }

        // ATR 계산 ( TrueRange.calculate() → WilderMovingAverage.calculate() → AverageTrueRange.calculate() )
        double[] atr = AverageTrueRange.calculate(candles, period);
        List<SuperTrend> result = new ArrayList<>(candles.size());

        // NaN 채우기
        for (int i = 0; i < candles.size(); i++) {
            result.add(SuperTrend.unavailable());
        }

        // 최초로 계산 가능한 위치 탐색
        int startIndex = findFirstAvailableIndex(atr);

        // 계산 가능한 ATR이 없는 경우 findFirstAvailableIndex()에 의해 -1이 출력되므로, 모든 결과가 unavailable인 리스트 반환
        if (startIndex < 0) {
            return List.copyOf(result);
        }

        // 최초 SuperTrend 초기화
        initializeFirstSuperTrend(candles, atr, multiplier, result, startIndex);

        // 최초 이후 인덱스부터는 반복 계산
        for (int i = startIndex + 1; i < candles.size(); i++) {
            // 현재 고가, 저가, 종가
            Candle currentCandle = candles.get(i);
            // 이전 종가
            Candle previousCandle = candles.get(i - 1);
            // 이전 상단선, 하단선, 방향, trendId
            SuperTrend previous = result.get(i - 1);

            // 기본 상단선/하단선 계산 (중간값 + 배수 * 현재 ATR 값)
            double middlePrice = (currentCandle.high() + currentCandle.low()) / 2.0;
            double basicUpperBand = middlePrice + multiplier * atr[i];
            double basicLowerBand = middlePrice - multiplier * atr[i];
            // 최종 상단선/하단선 계산 ( 이전 선 값을 사용할지, 방금 계산한 선 값을 사용할지 선택 )
            double finalUpperBand = calculateFinalUpperBand(basicUpperBand, previous.upperBand(), previousCandle.close());
            double finalLowerBand = calculateFinalLowerBand(basicLowerBand, previous.lowerBand(), previousCandle.close());

            // 일단 이전 방향을 그대로 복사 (방향 유지, 신호 없음)
            SuperTrend.Direction direction = previous.direction();
            SuperTrend.Signal signal = SuperTrend.Signal.NONE;

            // 이전 방향이 상승인 경우
            if (previous.direction() == SuperTrend.Direction.UP) {
                // 현재 종가 < 최종 하단선인 경우 상승 추세가 붕괴 -> DOWN -> SHORT 신호
                if (currentCandle.close() < finalLowerBand) {
                    direction = SuperTrend.Direction.DOWN;
                    signal = SuperTrend.Signal.SHORT;
                }
            } else {
                // 이전 방향이 하락인 경우
                // 현재 종가 > 최종 상단선인 경우 하락 추세가 붕괴 -> UP -> LONG 신호
                if (currentCandle.close() > finalUpperBand) {
                    direction = SuperTrend.Direction.UP;
                    signal = SuperTrend.Signal.LONG;
                }
            }

            // 최종 ST Value 선택 (선 하나만 선택)
            double superTrendValue;
            // 방향이 UP이면 하단선만, DOWN이면 상단선만 선택하여 표시
            if (direction == SuperTrend.Direction.UP) {
                superTrendValue = finalLowerBand;
            } else {
                superTrendValue = finalUpperBand;
            }

            // TrendID 갱신
            int trendId = previous.trendId();
            // 방향이 바뀌면 ID 번호 하나 증가
            if (direction != previous.direction()) {
                trendId++;
            }

            result.set(i, new SuperTrend(atr[i], superTrendValue, finalUpperBand, finalLowerBand, direction, signal, trendId));
        }
        // 불변 리스트로 반환
        return List.copyOf(result);
    }
}
