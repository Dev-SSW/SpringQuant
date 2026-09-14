package io.github.devssw.springquant.backtest;

import java.util.Objects;

// 백테스트에서 사용 할 설정 값을 저장합니다
public record BacktestConfig(
        double initialBalance,
        double feeRate,
        int leverage,
        double marginUsageRate,

        int bollingerPeriod,
        double bollingerMultiplier,
        // 밴드 폭 최소 설정
        double bollingerWidthMinPercent,

        int superTrendLength,
        double superTrendFactor,

        EntryMode entryMode,
        // 중심선 접근 허용 오차
        double maTolerancePercent,
        // 볼린저밴드 충돌 허용 오차
        double bbTolerancePercent,
        // 진입 허용 True/False
        boolean oneEntryPerTrend,

        // 위험관리설정 추가
        RiskManagementConfig riskManagement
) {
    // initialBalance : 테스트를 위해 추가한 가상의 시작 금액
    // 나머지는 차례대로 수수료, 배율, 잔고의 몇 %를 사용할지
    public BacktestConfig {
        if (!Double.isFinite(initialBalance) || initialBalance <= 0.0) {
            throw new IllegalArgumentException("시작 잔고는 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(feeRate) || feeRate < 0.0 || feeRate >= 1.0) {
            throw new IllegalArgumentException("수수료는 적어도 0과 1 사이여야 합니다.");
        }

        if (leverage <= 0) {
            throw new IllegalArgumentException("배율은 적어도 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(marginUsageRate) || marginUsageRate <= 0.0 || marginUsageRate > 1.0) {
            throw new IllegalArgumentException("퍼센트 비율은 적어도 0과 1 사이여야 합니다.");
        }

        if (bollingerPeriod <= 0) {
            throw new IllegalArgumentException("볼린저밴드 기간은 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(bollingerMultiplier) || bollingerMultiplier <= 0.0) {
            throw new IllegalArgumentException("bollingerMultiplier는 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(bollingerWidthMinPercent) || bollingerWidthMinPercent < 0.0) {
            throw new IllegalArgumentException("bollingerWidthMinPercent는 음수여서는 안됩니다.");
        }

        if (superTrendLength <= 0) {
            throw new IllegalArgumentException("superTrendLength는 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(superTrendFactor) || superTrendFactor <= 0.0) {
            throw new IllegalArgumentException("superTrendFactor는 0 이상이어야 합니다.");
        }

        Objects.requireNonNull(entryMode, "entryMode가 null이어서는 안됩니다.");

        if (!Double.isFinite(maTolerancePercent) || maTolerancePercent < 0.0) {
            throw new IllegalArgumentException("maTolerancePercent가 음수여서는 안됩니다.");
        }

        if (!Double.isFinite(bbTolerancePercent) || bbTolerancePercent < 0.0) {
            throw new IllegalArgumentException("bbTolerancePercent가 음수여서는 안됩니다.");
        }

        Objects.requireNonNull(riskManagement, "riskManagement가 null이어서는 안됩니다.");
    }
}
