
package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.*;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// 현재 보유 중인 포지션을 표현하는 레코드입니다 (백테스트 내부에서만 사용하기 위해, public을 제외함)
// 열려있는 포지션의 상태를 저장해두기 위해 필요한 파일입니다
// 2번 캔들에서 시작하고, 5번 캔들에서 끝나는 거래가 있다면, 3번-4번 캔들에서의 포지션 상태 기록이 필요합니다
// 다음에 만들 백테스트 엔진은 시간 순서대로 진입과 청산을 반복하기 때문에 포지션 상태 기록이 필요합니다
@Getter
final class OpenPosition {
    private static final double QUANTITY_TOLERANCE = 1e-12;

    // 진입 방향, 진입 시각, 진입가, 진입 수량, 진입 수수료, 위험 관리 설정
    private final PositionSide side;
    private final long entryTimeMs;
    private final double entryPrice;
    private final double initialQuantity;
    private final double entryFee;
    private final RiskManagementConfig riskManagement;

    @Getter(AccessLevel.NONE)
    private final List<BacktestExit> exits = new ArrayList<>();

    // 포지션 유지 시 바뀌는 필드
    // 남아있는 포지션 수량, 현재 손절가, 다음 익절 단계 (1->2->3), 본전 손절 적용이 되었는지
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

    // 최초 손절가를 계산
    private double calculateInitialStopPrice() {
        double stopLossRate = riskManagement.stopLossPercent() / 100.0;

        return switch (side) {
            case LONG -> entryPrice * (1.0 - stopLossRate);
            case SHORT -> entryPrice * (1.0 + stopLossRate);
        };
    }

    // 익절 단계가 남았는지 확인
    boolean hasNextTakeProfit() {
        return nextTakeProfitIndex < riskManagement.takeProfitLevels().size();
    }

    // 현재 검사해야 할 익절 설정 조회
    TakeProfitLevel nextTakeProfitLevel() {
        if (!hasNextTakeProfit()) {
            throw new IllegalStateException("남은 익절 단계가 없습니다.");
        }

        return riskManagement.takeProfitLevels().get(nextTakeProfitIndex);
    }

    // 익절 단계 별 목표 퍼센트를 실제 가격으로 변환
    double takeProfitPrice(TakeProfitLevel level) {
        double targetRate = level.targetPercent() / 100.0;

        return switch (side) {
            case LONG -> entryPrice * (1.0 + targetRate);
            case SHORT -> entryPrice * (1.0 - targetRate);
        };
    }

    // 익절 단계 별 청산 수량을 계산 (초기 수량을 기준으로 계산)
    double takeProfitQuantity(TakeProfitLevel level) {
        double requestedQuantity = initialQuantity * level.closeRatio();
        return Math.min(requestedQuantity, remainingQuantity);
    }

    // 수량 값 검증
    private void validateCloseQuantity(double quantity) {
        if (!Double.isFinite(quantity)
                || quantity <= 0.0
                || quantity > remainingQuantity + QUANTITY_TOLERANCE) {
            throw new IllegalArgumentException("유효하지 않은 청산 수량입니다.");
        }
    }

    // 포지션의 일부/전체를 실제로 청산하는 메서드
    BacktestExit closeQuantity(
            long exitTimeMs,
            double exitPrice,
            double quantity,
            double feeRate,
            ExitReason exitReason
    ) {
        // 수량 검증
        validateCloseQuantity(quantity);

        // 손익과 수수료 계산
        double grossPnl = calculateGrossPnl(exitPrice, quantity);
        double exitFee = exitPrice * quantity * feeRate;

        // 청산 결과 제작
        BacktestExit exit = new BacktestExit(
                exitTimeMs,
                exitPrice,
                quantity,
                grossPnl,
                exitFee,
                exitReason
        );

        // 청산 결과를 저장
        recordExit(exit);
        return exit;
    }

    // 청산 수수료를 제외한 실현 손익 계산
    private double calculateGrossPnl(double exitPrice, double quantity) {
        double priceDifference = side == PositionSide.LONG
                ? exitPrice - entryPrice
                : entryPrice - exitPrice;

        return priceDifference * quantity;
    }

    // 생성된 청산 결과를 저장하고, 남은 수량을 감소시킴
    private void recordExit(BacktestExit exit) {
        if (exit.exitTimeMs() < entryTimeMs) {
            throw new IllegalArgumentException("청산 시각은 진입 시각보다 빠를 수 없습니다.");
        }

        if (!exits.isEmpty() && exit.exitTimeMs() < exits.get(exits.size() - 1).exitTimeMs()) {
            throw new IllegalArgumentException("청산 내역은 시간순으로 기록되어야 합니다.");
        }

        exits.add(exit);
        remainingQuantity -= exit.quantity();

        if (remainingQuantity < QUANTITY_TOLERANCE) {
            remainingQuantity = 0.0;
        }
    }

    // 현재 익절 단계가 처리 됐다는 것을 기록함
    void completeTakeProfitLevel() {
        if (!hasNextTakeProfit()) {
            throw new IllegalStateException("남은 익절 단계가 없습니다.");
        }

        nextTakeProfitIndex++;
    }

    // 본절 손절가 승격 전환 가격에 도달했는지 확인하는 메서드
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

    // 본전 손절을 실제로 적용
    void moveStopToBreakEven() {
        currentStopPrice = entryPrice;
        breakEvenApplied = true;
    }

    // 남은 수량이 0이 됐는지 확인
    boolean isClosed() {
        return remainingQuantity <= QUANTITY_TOLERANCE;
    }

    // 현재까지 저장된 청산 내역을 반환
    List<BacktestExit> getExits() {
        return List.copyOf(exits);
    }
}