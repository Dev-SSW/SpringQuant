package io.github.devssw.springquant.backtest.model;

import java.util.List;
import java.util.Objects;

public record RiskManagementConfig(
        // 손절 범위, 단계별 익절 범위, 손절가 위치를 바꿀지 말지, 몇 퍼센트 움직였을때 손절가 위치를 바꿀지
        double stopLossPercent,
        List<TakeProfitLevel> takeProfitLevels,
        boolean moveStopToBreakEven,
        double breakEvenTriggerPercent
) {
    // 허용 오차
    private static final double RATIO_TOLERANCE = 1e-9;

    // 손절 범위 값 검증
    private static void validateStopLossPercent(double stopLossPercent) {
        if (!Double.isFinite(stopLossPercent) || stopLossPercent <= 0.0) {
            throw new IllegalArgumentException("stopLossPercent는 0보다 큰 유한한 값이어야 합니다.");
        }
    }

    // 단계별 익절 범위에 대한 값 검증
    private static void validateTakeProfitLevels(List<TakeProfitLevel> takeProfitLevels) {
        if (takeProfitLevels.isEmpty()) {
            throw new IllegalArgumentException("익절 단계는 최소 하나 이상이어야 합니다.");
        }

        double previousTargetPercent = 0.0;
        double totalCloseRatio = 0.0;

        for (TakeProfitLevel level : takeProfitLevels) {
            if (level.targetPercent() <= previousTargetPercent) {
                throw new IllegalArgumentException("익절 목표는 오름차순이어야 합니다.");
            }

            previousTargetPercent = level.targetPercent();
            totalCloseRatio += level.closeRatio();
        }

        if (Math.abs(totalCloseRatio - 1.0) > RATIO_TOLERANCE) {
            throw new IllegalArgumentException("익절 청산 비율의 합은 1.0이어야 합니다.");
        }
    }

    // 손절가 이동 True/False와 손절 승격은 몇 %에 닿았을 때 할 것인지에 대한 값에 대한 검증
    private static void validateBreakEven(boolean moveStopToBreakEven, double breakEvenTriggerPercent) {
        if (!Double.isFinite(breakEvenTriggerPercent) || breakEvenTriggerPercent < 0.0) {
            throw new IllegalArgumentException("breakEvenTriggerPercent는 0 이상의 유한한 값이어야 합니다.");
        }

        if (moveStopToBreakEven && breakEvenTriggerPercent == 0.0) {
            throw new IllegalArgumentException("본전 손절을 사용하면 전환 기준은 0보다 커야 합니다.");
        }
    }

    public RiskManagementConfig {
        validateStopLossPercent(stopLossPercent);
        Objects.requireNonNull(takeProfitLevels, "takeProfitLevels가 null이어서는 안됩니다.");
        takeProfitLevels = List.copyOf(takeProfitLevels);
        validateTakeProfitLevels(takeProfitLevels);
        validateBreakEven(moveStopToBreakEven, breakEvenTriggerPercent);
    }
}