package io.github.devssw.springquant.backtest.engine;

// 유지증거금 기반 계산을 위한 설정 (실제로 포지션은 강제 청산가에 닿기 이전에 유지증거금에 따라 청산이 발생합니다)
public record LiquidationConfig(
        // 유지증거금률 (0.001이면 1%), 유지증거금계산에서 보정/차감되는 금액
        double maintenanceMarginRate,
        double maintenanceAmount,
        // 청산가가 손절가보다 먼저 도달하는 진입을 막을지 여부, 손절가와 청산가 사이에 추가로 요구할 여유 거리
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