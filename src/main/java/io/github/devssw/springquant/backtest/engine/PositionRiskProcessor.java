package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.BacktestExit;
import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;
import io.github.devssw.springquant.market.Candle;

// BacktestEngine에서 분리 - 보유 중인 포지션의 익절, 손절 등 위험관리 조건을 처리
final class PositionRiskProcessor {
    // ==================== 의존 객체 ====================
    private final LiquidationPriceCalculator liquidationPriceCalculator = new LiquidationPriceCalculator();
    private final ExecutionPriceCalculator executionPriceCalculator = new ExecutionPriceCalculator();

    // ==================== 위험관리 처리 진입점 ====================
    /**
     * 현재 캔들에서 발생한 위험관리 청산을 처리하고 실현 손익을 반환합니다.
     * 같은 캔들에서 손절과 익절이 모두 가능한 경우 손절을 먼저 처리합니다. (보수적인 테스트 결과를 확인하기 위함)
     */
    double process(OpenPosition position, Candle candle, double feeRate, double slippageRate, LiquidationConfig liquidation) {
        // 손절가/청산가에 닿았는지 먼저 확인 및 처리
        double realizedPnl = processAdverseExit(position, candle, feeRate, slippageRate, liquidation);

        // 포지션 남았는지 확인 1
        if (position.isClosed()) {
            return realizedPnl;
        }

        // 분할 익절 실행
        realizedPnl += processTakeProfits(position, candle, feeRate, slippageRate);

        // 포지션 남았는지 확인 2
        if (position.isClosed()) {
            return realizedPnl;
        }

        // 본절 승격 처리 (moveStopToBreakEvenIfNeeded를 통해 손절가가 수정되면, BacktestEngine.java에서 다음 루프에 승격 처리가 완료됨)
        moveStopToBreakEvenIfNeeded(position, candle);
        return realizedPnl;
    }

    // 현재 캔들이 손절가에 도달했다면 남은 수량을 전부 청산
    // ==================== 손절 처리 ====================
    private double processStopLoss(OpenPosition position, Candle candle, double feeRate, double slippageRate) {
        if (!isStopLossReached(position, candle)) {
            return 0.0;
        }
        double marketPrice = stopLossExecutionPrice(position, candle);

        double exitPrice = executionPriceCalculator.exitPrice(
                position.getSide(),
                marketPrice,
                slippageRate,
                ExitReason.STOP_LOSS
        );

        BacktestExit exit = position.closeQuantity(
                candle.openTimeMs(),
                exitPrice,
                position.getRemainingQuantity(),
                feeRate,
                ExitReason.STOP_LOSS
        );

        return exit.realizedPnl();
    }

    /**
     * 캔들 시가가 이미 손절가를 넘어선 갭 상황을 반영합니다.
     * 롱: 시가가 손절가보다 낮으면 더 낮은 시가로 청산
     * 숏: 시가가 손절가보다 높으면 더 높은 시가로 청산
     */
    // ==================== 손절 가격/도달 판단 ====================
    private double stopLossExecutionPrice(OpenPosition position, Candle candle) {
        double stopPrice = position.getCurrentStopPrice();

        return position.getSide() == PositionSide.LONG
                ? Math.min(candle.open(), stopPrice)
                : Math.max(candle.open(), stopPrice);
    }

    // 롱은 저가, 숏은 고가가 손절가에 도달했는지 확인
    private boolean isStopLossReached(OpenPosition position, Candle candle) {
        return switch (position.getSide()) {
            case LONG -> candle.low() <= position.getCurrentStopPrice();
            case SHORT -> candle.high() >= position.getCurrentStopPrice();
        };
    }

    // 현재 캔들에서 도달한 익절 단계를 순서대로 처리
    // ==================== 익절 처리 ====================
    private double processTakeProfits(OpenPosition position, Candle candle, double feeRate, double slippageRate) {
        double realizedPnl = 0.0;
        // 다음 익절 단계가 남아있는 동안 실행
        while (position.hasNextTakeProfit()) {
            // 현재 익절 설정 조회
            TakeProfitLevel level = position.nextTakeProfitLevel();
            // 설정의 목표 퍼센트를 조회하고, 실제 가격을 반환
            double takeProfitPrice = position.takeProfitPrice(level);
            // 이미 목표에 닿았는지 확인하고, 닿지 않은 상태라면 반복 종료
            if (!isTakeProfitReached(position, candle, takeProfitPrice)) {
                break;
            }
            double exitPrice = executionPriceCalculator.exitPrice(position.getSide(), takeProfitPrice, slippageRate, ExitReason.TAKE_PROFIT);

            // 설정 청산 수량을 조회하고 계산
            double quantity = position.takeProfitQuantity(level);
            // 포지션 청산 메서드
            BacktestExit exit = position.closeQuantity(candle.openTimeMs(), exitPrice, quantity, feeRate, ExitReason.TAKE_PROFIT);
            // 다음 익절 단계로 인덱스 변경
            position.completeTakeProfitLevel();
            // 실제 손익 계산
            realizedPnl += exit.realizedPnl();

            if (position.isClosed()) {
                break;
            }
        }
        return realizedPnl;
    }

    // 익절에 닿았는지 확인
    private boolean isTakeProfitReached(OpenPosition position, Candle candle, double takeProfitPrice) {
        return switch (position.getSide()) {
            case LONG -> candle.high() >= takeProfitPrice;
            case SHORT -> candle.low() <= takeProfitPrice;
        };
    }

    // 본절 승격 처리
    // ==================== 본전 손절 처리 ====================
    private void moveStopToBreakEvenIfNeeded(OpenPosition position, Candle candle) {
        if (position.shouldMoveStopToBreakEven(candle.high(), candle.low())) {
            position.moveStopToBreakEven();
        }
    }

    // =========== 강제 청산 =========== //
    // 손절과 강제 청산 순서 판단 메서드 (무엇이 먼저인지)
    // ==================== 손절/강제청산 우선순위 처리 ====================
    private double processAdverseExit(OpenPosition position, Candle candle, double feeRate, double slippageRate, LiquidationConfig liquidation) {
        // 새로운 캔들의 시가가 이미 강제청산가나 손절가를 넘어섰는지 확인합니다. (강제 청산 우선)
        double realizedPnl = processOpenSafetyExit(position, candle, feeRate, slippageRate, liquidation);

        // 포지션 남았는지 확인
        if (position.isClosed()) {
            return realizedPnl;
        }

        // 청산가 계산
        double liquidationPrice = calculateLiquidationPrice(position, liquidation);

        // 청산가에는 아직 닿지 않은 상황에서, 손절가와 강제 청산가 중 어느 가격이 더 가까운지 확인
        if (isStopBeforeLiquidation(position, liquidationPrice)) {
            // 손절가가 가까운 상황이라면 손절 도달 여부를 확인하고, 도달했다면 손절
            return processStopLoss(position, candle, feeRate, slippageRate);
        }

        // 청산가가 더 가까운 상황이라면 청산가 도달 여부를 확인하고, 도달했다면 청산 (liquidate())
        return processLiquidation(position, candle, liquidationPrice, feeRate);
    }

    // 시가가 이미 강제 청산가를 넘었는지 판단
    // ==================== 강제청산 도달 판단 ====================
    private boolean isLiquidatedAtOpen(OpenPosition position, Candle candle, double liquidationPrice) {
        return switch (position.getSide()) {
            case LONG -> candle.open() <= liquidationPrice;
            case SHORT -> candle.open() >= liquidationPrice;
        };
    }

    // 손절가가 청산가보다 가까운 위치에 있는지 판단
    private boolean isStopBeforeLiquidation(OpenPosition position, double liquidationPrice) {
        return switch (position.getSide()) {
            case LONG -> position.getCurrentStopPrice() >= liquidationPrice;
            case SHORT -> position.getCurrentStopPrice() <= liquidationPrice;
        };
    }

    // 강제 청산을 처리하는 메서드
    // ==================== 강제청산 실행 ====================
    private double processLiquidation(OpenPosition position, Candle candle, double liquidationPrice, double feeRate) {
        // 청산가에 도달했는지 먼저 확인
        if (!isLiquidationReached(position, candle, liquidationPrice)) {
            return 0.0;
        }
        // 도달했으면 강제 청산을 처리
        return liquidate(
                position,
                candle,
                liquidationPrice,
                feeRate
        );
    }

    // 청산가에 도달했는지 확인하는 메서드
    private boolean isLiquidationReached(OpenPosition position, Candle candle, double liquidationPrice) {
        return switch (position.getSide()) {
            case LONG -> candle.low() <= liquidationPrice;
            case SHORT -> candle.high() >= liquidationPrice;
        };
    }

    // 청산가로 강제 청산 처리
    private double liquidate(OpenPosition position, Candle candle, double liquidationPrice, double feeRate) {
        BacktestExit exit = position.closeQuantity(
                candle.openTimeMs(),
                liquidationPrice,
                position.getRemainingQuantity(),
                feeRate,
                ExitReason.LIQUIDATION
        );
        return exit.realizedPnl();
    }

    // =========== =========== //

    /**
     * 새로운 캔들의 시가가 이미 강제청산가나 손절가를 넘어섰는지 확인합니다.
     * 강제청산과 손절이 동시에 발생할 수 있다면 강제청산을 우선합니다.
     */
    // ==================== 시가 안전 청산 ====================
    double processOpenSafetyExit(OpenPosition position, Candle candle, double feeRate, double slippageRate, LiquidationConfig liquidation) {
        double liquidationPrice = calculateLiquidationPrice(position, liquidation);
        ExitReason exitReason;

        // 시가가 이미 청산가를 넘어섰는지 확인 (강제 청산 우선)
        if (isLiquidatedAtOpen(position, candle, liquidationPrice)) {
            exitReason = ExitReason.LIQUIDATION;
        } else if (isStoppedAtOpen(position, candle)) {
            // 시가가 손절가를 넘었는지확인
            exitReason = ExitReason.STOP_LOSS;
        } else {
            return 0.0;
        }

        double exitPrice = executionPriceCalculator.exitPrice(
                position.getSide(),
                candle.open(),
                slippageRate,
                exitReason
        );

        BacktestExit exit = position.closeQuantity(
                candle.openTimeMs(),
                exitPrice,
                position.getRemainingQuantity(),
                feeRate,
                exitReason
        );

        return exit.realizedPnl();
    }

    // ==================== 시가 안전 청산 판단 헬퍼 ====================
    private boolean isStoppedAtOpen(OpenPosition position, Candle candle) {
        return switch (position.getSide()) {
            case LONG -> candle.open() <= position.getCurrentStopPrice();
            case SHORT -> candle.open() >= position.getCurrentStopPrice();
        };
    }

    // 강제 청산가 계산 메서드
    // ==================== 청산가 계산 ====================
    private double calculateLiquidationPrice(OpenPosition position, LiquidationConfig liquidation) {
        return liquidationPriceCalculator.calculate(
                position.getSide(),
                position.getEntryPrice(),
                position.getRemainingQuantity(),
                position.getRemainingMargin(),
                liquidation
        );
    }
}
