package io.github.devssw.springquant.backtest.model;

import java.util.Objects;

// 백테스트 도중 발생한 사건을 기록하기 위한 이벤트
public record BacktestEvent(
        long timeMs,                        // 이벤트가 발생한 캔들의 시작 시각
        BacktestEventType type,     // 진입·익절·손절 등의 이벤트 종류
        PositionSide side,                // LONG 또는 SHORT
        double price,                       // 진입가, 청산가 또는 승격된 손절가
        double quantity,                  // 체결된 수량
        String message                   // 화면 로그에 표시할 설명
) {
    public BacktestEvent {
        if (timeMs < 0L) {
            throw new IllegalArgumentException("이벤트 시각은 0 이상이어야 합니다.");
        }

        Objects.requireNonNull(type, "이벤트 타입이 null이면 안됩니다.");
        Objects.requireNonNull(side, "포지션 방향이 null이면 안됩니다.");

        if (!Double.isFinite(price) || price <= 0.0) {
            throw new IllegalArgumentException("이벤트 가격은 0보다 큰 유한한 값이어야 합니다.");
        }

        if (!Double.isFinite(quantity) || quantity < 0.0) {
            throw new IllegalArgumentException("이벤트 수량은 0 이상의 유한한 값이어야 합니다.");
        }

        Objects.requireNonNull(message, "이벤트 메시지가 null이면 안됩니다.");
        message = message.trim();

        if (message.isEmpty()) {
            throw new IllegalArgumentException("이벤트 메시지는 비어 있을 수 없습니다.");
        }
    }
}