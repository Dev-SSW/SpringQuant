package io.github.devssw.springquant.backtest.result;

// 특정 시각의 실현 잔고
public record EquityPoint(long timeMs, double balance) {
    public EquityPoint {
        if (timeMs < 0L) {
            throw new IllegalArgumentException("자산 기록 시각은 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(balance) || balance < 0.0) {
            throw new IllegalArgumentException("잔고는 0 이상의 유한한 값이어야 합니다.");
        }
    }
}
