package io.github.devssw.springquant.backtest.result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// 전체 거래의 요약 결과를 반환합니다 (테스트 종료 후 BacktestTrade의 결과들을 한 곳에 모아 계산합니다)
public record BacktestResult(
        // 전체 거래를 합산하 후 요약 결과를 저장 할 변수들 입니다
        // 초기 자금, 최종 자금, 증가/감소 퍼센트
        double initialBalance,
        double finalBalance,
        double totalReturnPercent,
        // 총 거래수, 수수료 차감 이후의 승리/패배 횟수, 승률, 전체 수수료 차감 전 손익, 전체 수수료 합계, 모든 수수료 차감 후 최종 손익
        int totalTrades,
        int winningTrades,
        int losingTrades,
        double winRatePercent,
        double totalGrossPnl,
        double totalFee,
        double totalNetPnl,
        // 백테스트에서 완료한 개별 거래 전체 목록
        List<BacktestTrade> trades,
        List<BacktestEvent> events
) {
    public BacktestResult {
        if (!Double.isFinite(initialBalance) || initialBalance <= 0.0) {
            throw new IllegalArgumentException("시작 자금은 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(finalBalance) || finalBalance < 0.0) {
            throw new IllegalArgumentException("최종 자산이 음수여서는 안됩니다.");
        }

        Objects.requireNonNull(trades, "거래 전체 목록이 null이어서는 안됩니다.");
        trades = List.copyOf(trades);

        Objects.requireNonNull(events, "이벤트 목록이 null이면 안됩니다.");
        events = List.copyOf(events);
    }

    // 시작 잔고와 완료된 거래 목록으로 전체 결과를 계산합니다.
    public static BacktestResult from(double initialBalance, List<BacktestTrade> trades) {
        return from(initialBalance, trades, List.of());
    }

    // 시작 잔고와 완료된 거래 목록으로 전체 결과를 계산합니다.
    public static BacktestResult from(double initialBalance, List<BacktestTrade> trades, List<BacktestEvent> runtimeEvents) {
        Objects.requireNonNull(trades, "거래 전체 목록이 null이어서는 안됩니다.");
        Objects.requireNonNull(runtimeEvents, "실행 중 이벤트 목록이 null이면 안됩니다.");

        // 수정되지 않도록 임시 변수로 가져옴
        List<BacktestTrade> copiedTrades = List.copyOf(trades);
        int winningTrades = 0;
        double totalGrossPnl = 0.0;
        double totalFee = 0.0;
        double totalNetPnl = 0.0;

        // 각 거래를 조회합니다
        for (BacktestTrade trade : copiedTrades) {
            // 승리 횟수를 저장합니다
            if (trade.isWin()) {
                winningTrades++;
            }
            // 각 거래별 수수료 차감 전 손익/전체 수수료/전체 최종 손익을 각 변수에 더해서 저장하여 전체 거래의 변수를 채웁니다
            totalGrossPnl += trade.grossPnl();
            totalFee += trade.totalFee();
            totalNetPnl += trade.netPnl();
        }

        // 전체 거래수, 패배 횟수, 최종 잔고, 최종 손익 퍼센트, 승률을 계산합니다
        int totalTrades = copiedTrades.size();
        int losingTrades = totalTrades - winningTrades;
        double finalBalance = initialBalance + totalNetPnl;
        double totalReturnPercent = totalNetPnl / initialBalance * 100.0;
        double winRatePercent = totalTrades == 0 ? 0.0 : (double) winningTrades / totalTrades * 100.0;

        List<BacktestEvent> events = new ArrayList<>(runtimeEvents);
        events.addAll(BacktestEventFactory.create(copiedTrades));
        events.sort(Comparator.comparingLong(BacktestEvent::timeMs));

        return new BacktestResult(
                initialBalance,
                finalBalance,
                totalReturnPercent,
                totalTrades,
                winningTrades,
                losingTrades,
                winRatePercent,
                totalGrossPnl,
                totalFee,
                totalNetPnl,
                copiedTrades,
                events
        );
    }
}
