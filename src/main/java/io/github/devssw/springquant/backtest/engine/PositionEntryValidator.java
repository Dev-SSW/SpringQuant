package io.github.devssw.springquant.backtest.engine;

import java.util.Objects;

// 청산가가 손절가보다 먼저 나오는 진입을 진입을 하지 못하도록 거부하는 기능
final class PositionEntryValidator {
    private final LiquidationPriceCalculator liquidationPriceCalculator = new LiquidationPriceCalculator();

    boolean canEnter(OpenPosition position, LiquidationConfig config) {
        Objects.requireNonNull(position, "position이 null이면 안 됩니다.");
        Objects.requireNonNull(config, "config가 null이면 안 됩니다.");

        // 청산가가 손절가보다 먼저 도달할 때, 진입을 막을지 말지에 대한 여부 체크 (false면 진입을 허용)
        if (!config.rejectEntryIfLiquidationBeforeStop()) {
            return true;
        }

        // 청산가 계산 (유지 증거금을 포함하여 계산)
        double liquidationPrice = liquidationPriceCalculator.calculate(
                position.getSide(),
                position.getEntryPrice(),
                position.getRemainingQuantity(),
                position.getRemainingMargin(),
                config
        );

        // 손절가와 청산가 사이에 여유 거리 계산
        // (숏 기준 - 진입가 : 100, 청산가 : 111, 손절가 : 110) -> 진입 허용
        // stopBufferPercent가 2라면, 여유거리를 포함해서 청산가는 112.2 이상이 되어야 합니다 -> 진입 거부
        double bufferRate = config.stopBufferPercent() / 100.0;

        // 롱 기준 - 청산가 < 손절가 * (여유거리)
        return switch (position.getSide()) {
            case LONG -> liquidationPrice < position.getCurrentStopPrice() * (1.0 - bufferRate);
            case SHORT -> liquidationPrice > position.getCurrentStopPrice() * (1.0 + bufferRate);
        };
    }
}
