package io.github.devssw.springquant.backtest.model;

import java.util.List;
import java.util.Objects;

// 완료한 거래 한 건의 기록을 반환합니다.
public record BacktestTrade (
        // 거래 내역 한 건
        // 포지션 시작 시간, 방향, 진입가, 수량, 진입 수수료
        long entryTimeMs,
        PositionSide side,
        double entryPrice,
        double quantity,
        double entryFee,
        List<BacktestExit> exits,
        double balanceAfter
) {
    private static final double QUANTITY_TOLERANCE = 1e-9;

    // 청산 시각의 유효성 확인 / 총 청산 수량이 최초 진입 수량과 일치하는지 검증
    private static void validateExits(long entryTimeMs, double quantity, List<BacktestExit> exits) {
        long previousExitTimeMs = entryTimeMs;
        double closedQuantity = 0.0;

        for (BacktestExit exit : exits) {
            if (exit.exitTimeMs() < entryTimeMs) {
                throw new IllegalArgumentException("청산 시각은 진입 시각보다 빠를 수 없습니다.");
            }

            if (exit.exitTimeMs() < previousExitTimeMs) {
                throw new IllegalArgumentException("청산 내역은 시간순으로 정렬되어야 합니다.");
            }

            previousExitTimeMs = exit.exitTimeMs();
            closedQuantity += exit.quantity();
        }

        double tolerance = Math.max(1.0, quantity) * QUANTITY_TOLERANCE;

        if (Math.abs(closedQuantity - quantity) > tolerance) {
            throw new IllegalArgumentException("전체 청산 수량은 최초 진입 수량과 같아야 합니다.");
        }
    }

    public BacktestTrade {
            if (entryTimeMs < 0L) {
                throw new IllegalArgumentException("entryTimeMs는 0 이상이어야 합니다.");
            }

            Objects.requireNonNull(side, "side가 null이어서는 안됩니다.");

            if (!Double.isFinite(entryPrice) || entryPrice <= 0.0) {
                throw new IllegalArgumentException("entryPrice는 0보다 큰 유한한 값이어야 합니다.");
            }

            if (!Double.isFinite(quantity) || quantity <= 0.0) {
                throw new IllegalArgumentException("quantity는 0보다 큰 유한한 값이어야 합니다.");
            }

            if (!Double.isFinite(entryFee) || entryFee < 0.0) {
                throw new IllegalArgumentException("entryFee는 0 이상의 유한한 값이어야 합니다.");
            }

            Objects.requireNonNull(exits, "exits가 null이어서는 안됩니다.");
            exits = List.copyOf(exits);

            if (exits.isEmpty()) {
                throw new IllegalArgumentException("청산 내역은 최소 하나 이상이어야 합니다.");
            }

            validateExits(entryTimeMs, quantity, exits);

            if (!Double.isFinite(balanceAfter)) {
                throw new IllegalArgumentException("balanceAfter는 유한한 값이어야 합니다.");
            }
    }

    // 가격 변화로 발생한 수수료 차감 전 손익입니다.
    public double grossPnl() {
        return exits.stream()
                .mapToDouble(BacktestExit::grossPnl)
                .sum();
    }

    // 청산 수수료 합산
    public double exitFee() {
        return exits.stream()
                .mapToDouble(BacktestExit::exitFee)
                .sum();
    }

    // 진입과 청산 과정에서 발생한 전체 수수료입니다.
    public double totalFee() {
        return entryFee + exitFee();
    }

    // 수수료까지 차감한 최종 손익입니다.
    public double netPnl() {
        return grossPnl() - totalFee();
    }

    // 청산 시각
    public long exitTimeMs() {
        return lastExit().exitTimeMs();
    }

    // 포지션을 보유한 시간입니다.
    public long holdingTimeMs() {
        return exitTimeMs() - entryTimeMs;
    }

    // 수수료 차감 후 이익이 발생했는지 확인합니다. (승률 계산을 위하여)
    public boolean isWin() {
        return netPnl() > 0.0;
    }

    public double exitPrice() {
        double weightedPriceSum = exits.stream()
                .mapToDouble(exit -> exit.exitPrice() * exit.quantity())
                .sum();

        return weightedPriceSum / quantity;
    }

    // 청산 이유
    public ExitReason exitReason() {
        return lastExit().exitReason();
    }

    // 마지막 청산
    private BacktestExit lastExit() {
        return exits.get(exits.size() - 1);
    }
}