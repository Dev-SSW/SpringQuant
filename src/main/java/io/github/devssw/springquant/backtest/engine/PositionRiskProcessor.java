package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.BacktestExit;
import io.github.devssw.springquant.backtest.model.ExitReason;
import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;
import io.github.devssw.springquant.market.Candle;

// BacktestEngine에서 분리 - 보유 중인 포지션의 익절, 손절 등 위험관리 조건을 처리
final class PositionRiskProcessor {
    /**
     * 현재 캔들에서 발생한 위험관리 청산을 처리하고 실현 손익을 반환합니다.
     * 같은 캔들에서 손절과 익절이 모두 가능한 경우 손절을 먼저 처리합니다. (보수적인 테스트 결과를 확인하기 위함)
     */
    double process(OpenPosition position, Candle candle, double feeRate) {
        // 손절가에 닿았는지 먼저 확인
        double stopLossPnl = processStopLoss(position, candle, feeRate);

        if (position.isClosed()) {
            return stopLossPnl;
        }

        // 분할 익절 실행
        double takeProfitPnl = processTakeProfits(position, candle, feeRate);
        return stopLossPnl + takeProfitPnl;
    }

    // 현재 캔들이 손절가에 도달했다면 남은 수량을 전부 청산
    private double processStopLoss(OpenPosition position, Candle candle, double feeRate) {
        if (!isStopLossReached(position, candle)) {
            return 0.0;
        }

        double exitPrice = stopLossExecutionPrice(position, candle);

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
    private double processTakeProfits(OpenPosition position, Candle candle, double feeRate) {
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
            // 설정 청산 수량을 조회하고 계산
            double quantity = position.takeProfitQuantity(level);
            // 포지션 청산 메서드
            BacktestExit exit = position.closeQuantity(candle.openTimeMs(), takeProfitPrice, quantity, feeRate, ExitReason.TAKE_PROFIT);
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
}
