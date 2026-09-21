package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.backtest.model.StopPromotionMode;

import java.util.Objects;

// 손절가 승격을 예약하는 기능
final class StopPromotionProcessor {
    private final StopPromotionCalculator calculator = new StopPromotionCalculator();

    /*
    * 익절 체결 후 손절가 승격을 예약합니다.
    * 손절가는 현재 캔들에서 바로 변경하지 않습니다.
    */
    void scheduleAfterTakeProfit(OpenPosition position, int completedTakeProfitIndex) {
        Objects.requireNonNull(position, "position이 null이면 안됩니다.");
        StopPromotionMode mode = position.getRiskManagement().stopPromotionMode();
        Double candidate = calculator.calculateCandidate(position, completedTakeProfitIndex, mode);

        if (candidate != null) {
            position.scheduleStopPromotion(completedTakeProfitIndex);
        }
    }

    // 이전 캔들에서 예약된 손절가 승격을 현재 캔들 시가에서 시도합니다.
    void tryApplyPendingAtOpen(OpenPosition position, double openPrice) {
        Objects.requireNonNull(position, "position이 null이면 안됩니다.");

        if (!Double.isFinite(openPrice) || openPrice <= 0.0) {
            throw new IllegalArgumentException("시가는 0보다 큰 유한한 값이어야 합니다.");
        }

        // 승격 대기 예약이 있는지 확인
        if (!position.hasPendingStopPromotion()) {
            return;
        }

        // 익절 단계 번호를 가져오고, 익절 모드를 조회한 후, 후보 손절가를 다시 계산
        int completedTakeProfitIndex = position.getPendingStopPromotionTakeProfitIndex();
        StopPromotionMode mode = position.getRiskManagement().stopPromotionMode();
        Double candidate = calculator.calculateCandidate(position, completedTakeProfitIndex, mode);

        // 현재 손절가가 이미 후보 가격과 같거나 더 유리하다면
        // 더 이상 승격할 필요가 없습니다.
        if (candidate == null) {
            position.clearPendingStopPromotion();
            return;
        }

        // 시가가 이미 승격할 손절가를 넘어선 경우
        // 기존 손절가를 유지하고 다음 캔들에서 다시 시도합니다.
        if (isCandidateCrossed(
                position.getSide(),
                candidate,
                openPrice
        )) {
            return;
        }

        position.applyPromotedStop(candidate);
        position.clearPendingStopPromotion();
    }

    // 시가가 이미 승격할 손절가를 넘어섰는지 확인합니다
    private boolean isCandidateCrossed(PositionSide side, double candidate, double openPrice) {
        return switch (side) {
            case LONG -> openPrice <= candidate;
            case SHORT -> openPrice >= candidate;
        };
    }
}
