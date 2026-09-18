package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;

import java.util.Objects;

// 강제 청산에 대한 기능을 정의합니다
// 유지증거금, 펀딩비, 거래소 별 청산 수수료 등은 아직 고려하지 않았습니다 (이후 추가를 위해 만든 클래스)
final class LiquidationPriceCalculator {
    // 유지증거금 기반 청산 계산을 위한 메서드
    double calculate(PositionSide side, double entryPrice, double quantity, double isolatedMargin, LiquidationConfig config) {
        Objects.requireNonNull(side, "side가 null이어서는 안 됩니다.");
        Objects.requireNonNull(config, "config가 null이어서는 안 됩니다.");

        if (!Double.isFinite(entryPrice) || entryPrice <= 0.0) {
            throw new IllegalArgumentException("entryPrice는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException("quantity는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(isolatedMargin) || isolatedMargin <= 0.0) {
            throw new IllegalArgumentException("isolatedMargin은 0보다 큰 유한한 값이어야 합니다.");
        }

        // 거래소에 따라 수량이 음수 표시일 수 있으니 절대값으로
        double absoluteQuantity = Math.abs(quantity);
        // 유지 증거금률 조회
        double maintenanceMarginRate = config.maintenanceMarginRate();

        // 유지 증거금률을 포함한 청산가 공식의 분모 계산
        double denominator = switch (side) {
            case LONG -> absoluteQuantity * (1.0 - maintenanceMarginRate);
            case SHORT -> absoluteQuantity * (1.0 + maintenanceMarginRate);
        };

        // (남은 수량 * 진입가 - 격리 증거금 - 유지 증거금 계산에 들어가는 보정 금액) / 청산가를 구하기 위한 분모
        double liquidationPrice = switch (side) {
            case LONG -> (absoluteQuantity * entryPrice - isolatedMargin - config.maintenanceAmount()) / denominator;
            case SHORT -> (absoluteQuantity * entryPrice + isolatedMargin + config.maintenanceAmount()) / denominator;
        };

        return Math.max(0.0, liquidationPrice);
    }
}
