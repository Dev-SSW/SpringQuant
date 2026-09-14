
package io.github.devssw.springquant.backtest;

import lombok.Getter;

// 현재 보유 중인 포지션을 표현하는 레코드입니다 (백테스트 내부에서만 사용하기 위해, public을 제외함)
// 열려있는 포지션의 상태를 저장해두기 위해 필요한 파일입니다
// 2번 캔들에서 시작하고, 5번 캔들에서 끝나는 거래가 있다면, 3번-4번 캔들에서의 포지션 상태 기록이 필요합니다
// 다음에 만들 백테스트 엔진은 시간 순서대로 진입과 청산을 반복하기 때문에 포지션 상태 기록이 필요합니다
@Getter
final class OpenPosition {
    private static final double QUANTITY_TOLERANCE = 1e-12;

    private final PositionSide side;
    private final long entryTimeMs;
    private final double entryPrice;
    private final double initialQuantity;
    private final double entryFee;
    private final RiskManagementConfig riskManagement;

    private double remainingQuantity;
    private double currentStopPrice;
    private int nextTakeProfitIndex;
    private boolean breakEvenApplied;

    OpenPosition(
            long entryTimeMs,
            PositionSide side,
            double entryPrice,
            double quantity,
            double entryFee,
            RiskManagementConfig riskManagement
    ) {
        this.side = side;
        this.entryTimeMs = entryTimeMs;
        this.entryPrice = entryPrice;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
        this.entryFee = entryFee;
        this.riskManagement = riskManagement;
        this.currentStopPrice = calculateInitialStopPrice();
        this.nextTakeProfitIndex = 0;
        this.breakEvenApplied = false;
    }

    private double calculateInitialStopPrice() {
        double stopLossRate = riskManagement.stopLossPercent() / 100.0;

        return switch (side) {
            case LONG -> entryPrice * (1.0 - stopLossRate);
            case SHORT -> entryPrice * (1.0 + stopLossRate);
        };
    }

    boolean hasNextTakeProfit() {
        return nextTakeProfitIndex < riskManagement.takeProfitLevels().size();
    }

    TakeProfitLevel nextTakeProfitLevel() {
        if (!hasNextTakeProfit()) {
            throw new IllegalStateException("남은 익절 단계가 없습니다.");
        }

        return riskManagement.takeProfitLevels().get(nextTakeProfitIndex);
    }

    double takeProfitPrice(TakeProfitLevel level) {
        double targetRate = level.targetPercent() / 100.0;

        return switch (side) {
            case LONG -> entryPrice * (1.0 + targetRate);
            case SHORT -> entryPrice * (1.0 - targetRate);
        };
    }

    double takeProfitQuantity(TakeProfitLevel level) {
        double requestedQuantity = initialQuantity * level.closeRatio();
        return Math.min(requestedQuantity, remainingQuantity);
    }

    void completeTakeProfitLevel(double closedQuantity) {
        if (!Double.isFinite(closedQuantity)
                || closedQuantity <= 0.0
                || closedQuantity > remainingQuantity + QUANTITY_TOLERANCE) {
            throw new IllegalArgumentException("유효하지 않은 청산 수량입니다.");
        }

        remainingQuantity -= closedQuantity;

        if (remainingQuantity < QUANTITY_TOLERANCE) {
            remainingQuantity = 0.0;
        }

        nextTakeProfitIndex++;
    }

    boolean shouldMoveStopToBreakEven(double candleHigh, double candleLow) {
        if (!riskManagement.moveStopToBreakEven() || breakEvenApplied) {
            return false;
        }

        double triggerRate = riskManagement.breakEvenTriggerPercent() / 100.0;

        double triggerPrice = switch (side) {
            case LONG -> entryPrice * (1.0 + triggerRate);
            case SHORT -> entryPrice * (1.0 - triggerRate);
        };

        return switch (side) {
            case LONG -> candleHigh >= triggerPrice;
            case SHORT -> candleLow <= triggerPrice;
        };
    }

    void moveStopToBreakEven() {
        currentStopPrice = entryPrice;
        breakEvenApplied = true;
    }

    boolean isClosed() {
        return remainingQuantity <= QUANTITY_TOLERANCE;
    }
}