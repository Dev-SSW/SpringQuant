package io.github.devssw.springquant.indicator;

public record SuperTrend(
        // ATR, 최종 ST선 (상단과 하단 중 하나 선택), 상단선, 하단선, 현재 추세 (상/하), 추세 전환 신호, 구간 번호
        double atr,
        double value,
        double upperBand,
        double lowerBand,
        Direction direction,
        Signal signal,
        int trendId
) {
    // 현재 추세
    public enum Direction {
        UNAVAILABLE,
        UP,
        DOWN
    }

    // 추세 전환 신호
    public enum Signal {
        NONE,
        LONG,
        SHORT
    }

    // ATR 기간을 채우지 못한 초기 구간의 결과를 만듦
    public static SuperTrend unavailable() {
        return new SuperTrend(
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Direction.UNAVAILABLE,
                Signal.NONE,
                -1
        );
    }

    // 사용가능한 결과인지 확인 (유한한 숫자)
    public boolean isAvailable() {
        return Double.isFinite(atr)
                && Double.isFinite(value)
                && direction != Direction.UNAVAILABLE;
    }
}
