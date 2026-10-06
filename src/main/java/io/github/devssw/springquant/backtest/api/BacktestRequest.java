package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.engine.LiquidationConfig;
import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.model.EntryMode;
import io.github.devssw.springquant.backtest.model.RiskManagementConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Schema(description = "백테스트 실행 요청")
public record BacktestRequest(
        @Schema(description = "거래 종목", example = "BTCUSDT")
        @NotBlank(message = "symbol은 필수입니다.")
        String symbol,

        @Schema(description = "캔들 시간 간격", example = "4h")
        @NotBlank(message = "interval은 필수입니다.")
        String interval,

        @Schema(description = "조회 시작 시각(Unix millisecond)", example = "1700000000000")
        @Positive(message = "startTimeMs는 0보다 커야 합니다.")
        long startTimeMs,

        @Schema(description = "조회 종료 시각(Unix millisecond)", example = "1701000000000")
        @Positive(message = "endTimeMs는 0보다 커야 합니다.")
        long endTimeMs,

        @Schema(description = "백테스트 시작 자금", example = "10000")
        @Positive(message = "initialBalance는 0보다 커야 합니다.")
        double initialBalance,

        @Schema(description = "편도 거래 수수료율", example = "0.0005")
        @PositiveOrZero(message = "feeRate는 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", inclusive = false, message = "feeRate는 1보다 작아야 합니다.")
        double feeRate,

        @Schema(description = "주문 체결 슬리피지율", example = "0.0003")
        @PositiveOrZero(message = "slippageRate는 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", inclusive = false, message = "slippageRate는 1보다 작아야 합니다.")
        double slippageRate,

        @Schema(description = "포지션 레버리지", example = "2")
        @Min(value = 1, message = "leverage는 1 이상이어야 합니다.")
        int leverage,

        @Schema(description = "잔고 중 증거금으로 사용할 비율", example = "0.1")
        @DecimalMin(value = "0.0", inclusive = false, message = "marginUsageRate는 0보다 커야 합니다.")
        @DecimalMax(value = "1.0", message = "marginUsageRate는 1 이하여야 합니다.")
        double marginUsageRate,

        @Schema(description = "볼린저밴드 계산 기간", example = "12")
        @Min(value = 1, message = "bollingerPeriod는 1 이상이어야 합니다.")
        int bollingerPeriod,

        @Schema(description = "볼린저밴드 표준편차 배수", example = "1.5")
        @Positive(message = "bollingerMultiplier는 0보다 커야 합니다.")
        double bollingerMultiplier,

        @Schema(description = "진입을 허용할 최소 볼린저밴드 폭(%)", example = "0.1")
        @PositiveOrZero(message = "bollingerWidthMinPercent는 0 이상이어야 합니다.")
        double bollingerWidthMinPercent,

        @Schema(description = "SuperTrend 계산 기간", example = "8")
        @Min(value = 1, message = "superTrendLength는 1 이상이어야 합니다.")
        int superTrendLength,

        @Schema(description = "SuperTrend ATR 배수", example = "2.5")
        @Positive(message = "superTrendFactor는 0보다 커야 합니다.")
        double superTrendFactor,

        @Schema(description = "포지션 진입 조건", example = "MA_BOUNCE")
        @NotNull(message = "entryMode는 필수입니다.")
        EntryMode entryMode,

        @Schema(description = "이동평균선 접촉 허용 범위(%)", example = "0.15")
        @PositiveOrZero(message = "maTolerancePercent는 0 이상이어야 합니다.")
        double maTolerancePercent,

        @Schema(description = "볼린저밴드 접촉 허용 범위(%)", example = "0.15")
        @PositiveOrZero(message = "bbTolerancePercent는 0 이상이어야 합니다.")
        double bbTolerancePercent,

        @Schema(description = "같은 SuperTrend 추세에서 한 번만 진입할지 여부", example = "true")
        boolean oneEntryPerTrend,

        @Schema(description = "손절, 분할 익절, 손절가 승격 설정")
        @NotNull(message = "riskManagement는 필수입니다.")
        @Valid
        RiskManagementConfig riskManagement,

        @Schema(description = "강제청산 및 진입 안전성 설정")
        @NotNull(message = "liquidation은 필수입니다.")
        @Valid
        LiquidationConfig liquidation
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
                    slippageRate,
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
                    riskManagement,
                    liquidation
            );
        }
}
