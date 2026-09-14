package io.github.devssw.springquant.backtest;

public record TakeProfitLevel(double targetPercent, double closeRatio) {
    // targetPercent - 익절 범위는 몇 퍼센트로 설정할지
    // closeRatio - 포지션 중 몇 퍼센트만 청산할지
    public TakeProfitLevel {
        if (!Double.isFinite(targetPercent) || targetPercent <= 0.0) {
            throw new IllegalArgumentException("targetPercent는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(closeRatio) || closeRatio <= 0.0 || closeRatio > 1.0) {
            throw new IllegalArgumentException("closeRatio는 0보다 크고 1 이하여야 합니다.");
        }
    }
}
