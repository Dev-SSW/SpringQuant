package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.model.EntryMode;
import io.github.devssw.springquant.backtest.model.RiskManagementConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record BacktestRequest (
        // 심볼, 간격, 시작/종료 시각 (반드시 0보다 큼)
        @NotBlank String symbol,
        @NotBlank String interval,
        @Positive long startTimeMs,
        @Positive long endTimeMs,
        // 시작 잔고, 수수료(0이상0, 레버리지(최소1), 잔고 사용 비율 (최소 0 - 최대 1)
        @Positive double initialBalance,
        @PositiveOrZero double feeRate,
        @Min(1) int leverage,
        @DecimalMin(value = "0.0", inclusive = false)
        @DecimalMax("1.0")
        double marginUsageRate,
        // BB 기간, 배수, 최소 폭
        @Min(1) int bollingerPeriod,
        @Positive double bollingerMultiplier,
        @PositiveOrZero double bollingerWidthMinPercent,
        // ST 길이, 팩터
        @Min(1) int superTrendLength,
        @Positive double superTrendFactor,
        // 진입 이유, ma 허용 (0이상), bb 허용, 추세 별 한 번 진입 허용/비허용
        @NotNull EntryMode entryMode,
        @PositiveOrZero double maTolerancePercent,
        @PositiveOrZero double bbTolerancePercent,
        boolean oneEntryPerTrend,

        @NotNull @Valid RiskManagementConfig riskManagement
) {

    public BacktestRequest {
            if (endTimeMs <= startTimeMs) {
                throw new IllegalArgumentException("endTimeMs는 startTimeMs보다 커야 합니다.");
            }
        }

        public BacktestConfig toConfig() {
            return new BacktestConfig(
                    initialBalance,
                    feeRate,
                    leverage,
                    marginUsageRate,
                    bollingerPeriod,
                    bollingerMultiplier,
                    bollingerWidthMinPercent,
                    superTrendLength,
                    superTrendFactor,
                    entryMode,
                    maTolerancePercent,
                    bbTolerancePercent,
                    oneEntryPerTrend,
                    riskManagement
            );
        }
}
