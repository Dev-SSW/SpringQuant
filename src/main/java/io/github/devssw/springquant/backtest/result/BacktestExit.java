package io.github.devssw.springquant.backtest.result;

import io.github.devssw.springquant.backtest.model.ExitReason;

import java.util.Objects;

// 분할 익절에 따라 포지션의 일부, 또는 전부를 청산했을 때 결과입니다 (거래 한 건)
// 기존의 BacktestTrade는 포지션 전체를 한 번에 청산하는 구조였습니다.
public record BacktestExit(
        // 청산 -> 시각, 가격, 수량, 수수료 차감 전 손익, 수수료, 사유
        long exitTimeMs,
        double exitPrice,
        double quantity,
        double grossPnl,
        double exitFee,
        ExitReason exitReason
) {
    public BacktestExit {
        if (exitTimeMs < 0L) {
            throw new IllegalArgumentException("exitTimeMs는 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(exitPrice) || exitPrice <= 0.0) {
            throw new IllegalArgumentException("exitPrice는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException("quantity는 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(grossPnl)) {
            throw new IllegalArgumentException("grossPnl은 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(exitFee) || exitFee < 0.0) {
            throw new IllegalArgumentException("exitFee는 0 이상의 유한한 값이어야 합니다.");
        }

        Objects.requireNonNull(exitReason, "exitReason는 null이어선 안됩니다");
    }

    // 청산 시 수수료 차감
    public double realizedPnl() {
        return grossPnl - exitFee;
    }
}
