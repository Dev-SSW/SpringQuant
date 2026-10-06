package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;

import java.util.Objects;

// 슬리피지를 구현합니다 (항상 원하는 가격에 체결 될 수 없으므로 )
final class ExecutionPriceCalculator {
    // 진입 슬리피지
    double entryPrice(PositionSide side, double marketPrice, double slippageRate) {
        validate(side, marketPrice, slippageRate);

        return switch (side) {
            case LONG -> marketPrice * (1.0 + slippageRate);
            case SHORT -> marketPrice * (1.0 - slippageRate);
        };
    }

    // 청산 슬리피지
    double exitPrice(PositionSide side, double marketPrice, double slippageRate, ExitReason exitReason) {
        validate(side, marketPrice, slippageRate);
        Objects.requireNonNull(exitReason, "exitReason이 null이면 안 됩니다.");

        // 강제 청산의 경우 슬리피지를 적용하지 않습니다
        if (exitReason == ExitReason.LIQUIDATION) {
            return marketPrice;
        }

        return switch (side) {
            case LONG -> marketPrice * (1.0 - slippageRate);
            case SHORT -> marketPrice * (1.0 + slippageRate);
        };
    }

    private void validate(PositionSide side, double marketPrice, double slippageRate) {
        Objects.requireNonNull(side, "side가 null이면 안 됩니다.");

        if (!Double.isFinite(marketPrice) || marketPrice <= 0.0) {
            throw new IllegalArgumentException("marketPrice는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(slippageRate) || slippageRate < 0.0 || slippageRate >= 1.0) {
            throw new IllegalArgumentException("slippageRate는 0 이상 1 미만이어야 합니다.");
        }
    }
}