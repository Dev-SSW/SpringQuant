package io.github.devssw.springquant.backtest.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 완료된 거래를 다시 읽고, 시간별 잔고를 생성
public final class EquityCurveCalculator {
    private static final double BALANCE_TOLERANCE = 1e-9;
    private EquityCurveCalculator() { }
    
    public static List<EquityPoint> calculate(long startTimeMs, double initialBalance, List<BacktestTrade> trades) {
        if (startTimeMs < 0L) {
            throw new IllegalArgumentException("백테스트 시작 시각은 0 이상이어야 합니다.");
        }

        if (!Double.isFinite(initialBalance) || initialBalance <= 0.0) {
            throw new IllegalArgumentException("시작 잔고는 0보다 큰 유한한 값이어야 합니다.");
        }

        Objects.requireNonNull(trades, "거래 목록이 null이면 안됩니다.");
        List<EquityPoint> points = new ArrayList<>();
        double balance = initialBalance;
        // 시작 잔고를 기록
        addOrReplace(points, startTimeMs, balance);

        // 거래를 하나씩 읽습니다
        for (BacktestTrade trade : trades) {
            if (trade.entryTimeMs() < startTimeMs) {
                throw new IllegalArgumentException("거래 진입 시각은 백테스트 시작 시각보다 빠를 수 없습니다.");
            }

            // 진입할 때 지급한 수수료를 반영합니다.
            balance -= trade.entryFee();
            addOrReplace(points, trade.entryTimeMs(), balance);

            // 부분 청산과 최종 청산의 실현 손익을 차례대로 반영합니다.
            for (BacktestExit exit : trade.exits()) {
                balance += exit.realizedPnl();
                addOrReplace(points, exit.exitTimeMs(), balance);
            }

            validateBalanceAfter(balance, trade.balanceAfter());
        }

        return List.copyOf(points);
    }

    // 같은 시각에 여러 잔고 변화가 발생하면 해당 시각의 마지막 잔고만 저장합니다.
    private static void addOrReplace(List<EquityPoint> points, long timeMs, double balance) {
        EquityPoint point = new EquityPoint(timeMs, balance);

        if (points.isEmpty()) {
            points.add(point);
            return;
        }

        EquityPoint lastPoint = points.get(points.size() - 1);

        if (timeMs < lastPoint.timeMs()) {
            throw new IllegalArgumentException("자산 기록은 시간순으로 생성되어야 합니다.");
        }

        if (timeMs == lastPoint.timeMs()) {
            points.set(points.size() - 1, point);
            return;
        }

        points.add(point);
    }

    // 자산 곡선에서 계산한 잔고가 거래에 기록된 최종 잔고와 같은지 확인합니다.
    private static void validateBalanceAfter(double calculatedBalance, double recordedBalance) {
        double tolerance = Math.max(1.0, Math.abs(recordedBalance)) * BALANCE_TOLERANCE;

        if (Math.abs(calculatedBalance - recordedBalance) > tolerance) {
            throw new IllegalStateException("계산한 잔고와 거래에 기록된 잔고가 일치하지 않습니다.");
        }
    }
}
