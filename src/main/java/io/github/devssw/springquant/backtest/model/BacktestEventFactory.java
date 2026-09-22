package io.github.devssw.springquant.backtest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 손절가 승격과 승격 보류는 거래 내역만으로 알 수 없으므로, 해당 클래스에 조회하여 사용합니다
final class BacktestEventFactory {
    private BacktestEventFactory() { }

    // Trade 결과와 Exit 결과를 조회하여 합칩니다
    static List<BacktestEvent> create(List<BacktestTrade> trades) {
        Objects.requireNonNull(trades, "trades가 null이면 안됩니다.");
        List<BacktestEvent> events = new ArrayList<>();

        for (BacktestTrade trade : trades) {
            events.add(createEntryEvent(trade));
            for (BacktestExit exit : trade.exits()) {
                events.add(createExitEvent(trade, exit));
            }
        }
        return List.copyOf(events);
    }

    // 거래 내역 한 건
    private static BacktestEvent createEntryEvent(BacktestTrade trade) {
        return new BacktestEvent(
                trade.entryTimeMs(),
                BacktestEventType.ENTRY,
                trade.side(),
                trade.entryPrice(),
                trade.quantity(),
                trade.side() + " 포지션에 진입했습니다."
        );
    }

    // 거래 내역 한 건 안의 분할 익절 내역
    private static BacktestEvent createExitEvent(BacktestTrade trade, BacktestExit exit) {
        return new BacktestEvent(
                exit.exitTimeMs(),
                toEventType(exit.exitReason()),
                trade.side(),
                exit.exitPrice(),
                exit.quantity(),
                toMessage(exit.exitReason())
        );
    }

    // 청산 이유 지정
    private static BacktestEventType toEventType(ExitReason exitReason) {
        return switch (exitReason) {
            case TAKE_PROFIT -> BacktestEventType.TAKE_PROFIT;
            case STOP_LOSS -> BacktestEventType.STOP_LOSS;
            case SUPER_TREND -> BacktestEventType.SUPER_TREND_EXIT;
            case LIQUIDATION -> BacktestEventType.LIQUIDATION;
            case END_OF_DATA -> BacktestEventType.END_OF_DATA_EXIT;
        };
    }

    // 청산 메서지 지정
    private static String toMessage(ExitReason exitReason) {
        return switch (exitReason) {
            case TAKE_PROFIT -> "익절이 체결되었습니다.";
            case STOP_LOSS -> "손절이 체결되었습니다.";
            case SUPER_TREND -> "SuperTrend 전환으로 포지션을 종료했습니다.";
            case LIQUIDATION -> "포지션이 강제청산되었습니다.";
            case END_OF_DATA -> "백테스트 데이터 종료로 포지션을 종료했습니다.";
        };
    }
}
