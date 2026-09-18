package io.github.devssw.springquant.backtest.engine;

// 유지증거금 기반 계산을 위한 설정 (실제로 포지션은 강제 청산가에 닿기 이전에 유지증거금에 따라 청산이 발생합니다)
public record LiquidationConfig(
        double maintenanceMarginRate,
        double maintenanceAmount,
        boolean rejectEntryIfLiquidationBeforeStop,
        double stopBufferPercent
) {
    public LiquidationConfig {
        if (!Double.isFinite(maintenanceMarginRate) || maintenanceMarginRate < 0.0 || maintenanceMarginRate >= 1.0) {
            throw new IllegalArgumentException("maintenanceMarginRate는 0 이상 1 미만이어야 합니다.");
        }

        if (!Double.isFinite(maintenanceAmount) || maintenanceAmount < 0.0) {
            throw new IllegalArgumentException("maintenanceAmount는 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(stopBufferPercent) || stopBufferPercent < 0.0 || stopBufferPercent >= 100.0) {
            throw new IllegalArgumentException("stopBufferPercent는 0 이상 100 미만이어야 합니다.");
        }
    }
}