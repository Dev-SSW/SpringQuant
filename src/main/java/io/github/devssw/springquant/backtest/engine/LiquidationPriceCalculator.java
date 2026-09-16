package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;

import java.util.Objects;

// 강제 청산에 대한 기능을 정의합니다
// 유지증거금, 펀딩비, 거래소 별 청산 수수료 등은 아직 고려하지 않았습니다 (이후 추가를 위해 만든 클래스)
final class LiquidationPriceCalculator {
    double calculate(PositionSide side, double entryPrice, int leverage) {
        Objects.requireNonNull(side, "side가 null이어서는 안 됩니다.");

        if (!Double.isFinite(entryPrice) || entryPrice <= 0.0) {
            throw new IllegalArgumentException("entryPrice는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (leverage <= 0) {
            throw new IllegalArgumentException("leverage는 0보다 커야 합니다.");
        }

        // 증거금은 레버리지에 따라 달라집니다 (Ex - 10일 경우, 전체 금액에서 10%만 증거금으로 사용됨)
        double marginRate = 1.0 / leverage;
        return switch (side) {
            case LONG -> entryPrice * (1.0 - marginRate);
            case SHORT -> entryPrice * (1.0 + marginRate);
        };
    }
}
