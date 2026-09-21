package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.StopPromotionMode;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;

import java.util.Objects;

// 익절 단계에 따라 SL 수정
final class StopPromotionCalculator {
    /*
    *  position                             현재 포지션
    *  completedTakeProfitIndex 체결된 익절 단계 인덱스
    *  mode                                 손절가 승격 방식
    */
    Double calculateCandidate(OpenPosition position, int completedTakeProfitIndex, StopPromotionMode mode) {
        Objects.requireNonNull(position, "position이 null이면 안됩니다.");
        Objects.requireNonNull(mode, "mode가 null이면 안됩니다.");

        if (completedTakeProfitIndex < 0) {
            throw new IllegalArgumentException("익절 단계 인덱스는 0 이상이어야 합니다.");
        }

        Double candidate = switch (mode) {
            case NONE -> null;
            case BE_AFTER_TP1 -> completedTakeProfitIndex >= 0 ? position.getEntryPrice() : null;
            case BE_AFTER_TP2 -> completedTakeProfitIndex >= 1 ? position.getEntryPrice() : null;
            case TP1_AFTER_TP2 -> completedTakeProfitIndex >= 1 ? firstTakeProfitPrice(position) : null;
            case BE_TP1_AFTER_TP2 -> {
                if (completedTakeProfitIndex == 0) {
                    yield position.getEntryPrice();
                }
                yield firstTakeProfitPrice(position);
            }
            case LOCK_HALF_TP1_AFTER_TP1 -> completedTakeProfitIndex >= 0 ? halfwayToFirstTakeProfit(position) : null;
        };

        // 새로운 SL이 기존 SL보다 유리한 위치로 설정됐는지 확인
        if (candidate == null || !improvesCurrentStop(position, candidate)) {
            return null;
        }

        return candidate;
    }

    // 1차 익절 가격을 반환합니다.
    private double firstTakeProfitPrice(OpenPosition position) {
        TakeProfitLevel firstLevel = position.getRiskManagement().takeProfitLevels().get(0);
        return position.takeProfitPrice(firstLevel);
    }

    // 진입가와 1차 익절가 사이의 중간 가격을 반환합니다.
    private double halfwayToFirstTakeProfit(OpenPosition position) {
        double entryPrice = position.getEntryPrice();
        double firstTakeProfitPrice = firstTakeProfitPrice(position);
        return (entryPrice + firstTakeProfitPrice) / 2.0;
    }

    // 새로운 손절가가 기존 손절가보다 포지션에 유리한지 확인합니다.
    private boolean improvesCurrentStop(OpenPosition position, double candidate) {
        double currentStopPrice = position.getCurrentStopPrice();

        return switch (position.getSide()) {
            case LONG -> candidate > currentStopPrice;
            case SHORT -> candidate < currentStopPrice;
        };
    }
}
