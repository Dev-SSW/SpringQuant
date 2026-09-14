package io.github.devssw.springquant.backtest.model;

import java.util.Objects;

// 완료한 거래 한 건의 기록을 반환합니다.
public record BacktestTrade (
        // 거래 내역 한 건
        // 포지션을 시작한 캔들의 시간 / 종료한 캔들의 시간
        // 포지션 방향, 진입가, 청산가, 수량, 진입/청산 수수료, 최종 손익, 포지션 종료 이유
        long entryTimeMs,
        long exitTimeMs,
        PositionSide side,
        double entryPrice,
        double exitPrice,
        double quantity,
        double entryFee,
        double exitFee,
        double balanceAfter,
        ExitReason exitReason
) {
    public BacktestTrade {
        Objects.requireNonNull(side, "포지션 방향이 null이어서는 안됩니다.");
        Objects.requireNonNull(exitReason, "포지션 종료 이유가 null이어서는 안됩니다.");

        if (entryTimeMs < 0L) {
            throw new IllegalArgumentException("진입 시간이 음수여서는 안됩니다.");
        }

        if (exitTimeMs < entryTimeMs) {
            throw new IllegalArgumentException("종료 시간이 진입 시간보다 빠르면 안됩니다.");
        }

        if (!Double.isFinite(entryPrice) || entryPrice <= 0.0) {
            throw new IllegalArgumentException("진입가는 유한하고, 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(exitPrice) || exitPrice <= 0.0) {
            throw new IllegalArgumentException("청산가는 유한하고, 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException("수량은 유한하고, 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(entryFee) || entryFee < 0.0) {
            throw new IllegalArgumentException("진입 수수료가 음수여서는 안됩니다.");
        }

        if (!Double.isFinite(exitFee) || exitFee < 0.0) {
            throw new IllegalArgumentException("청산 수수료가 음수여서는 안됩니다.");
        }

        if (!Double.isFinite(balanceAfter) || balanceAfter < 0.0) {
            throw new IllegalArgumentException("최종 손익이 음수여서는 안됩니다.");
        }
    }

    // 가격 변화로 발생한 수수료 차감 전 손익입니다.
    public double grossPnl() {
        // 롱이면 롱 손익 계산, 숏이면 숏 손익 계산을 진행합니다.
        double priceDifference = side == PositionSide.LONG ? exitPrice - entryPrice : entryPrice - exitPrice;
        // 수수료 차감 전 손익을 계산합니다 ( * 수량 )
        return priceDifference * quantity;
    }

    // 진입과 청산 과정에서 발생한 전체 수수료입니다.
    public double totalFee() {
        return entryFee + exitFee;
    }

    // 수수료까지 차감한 최종 손익입니다.
    public double netPnl() {
        return grossPnl() - totalFee();
    }

    // 포지션을 보유한 시간입니다.
    public long holdingTimeMs() {
        return exitTimeMs - entryTimeMs;
    }

    // 수수료 차감 후 이익이 발생했는지 확인합니다. (승률 계산을 위하여)
    public boolean isWin() {
        return netPnl() > 0.0;
    }
}