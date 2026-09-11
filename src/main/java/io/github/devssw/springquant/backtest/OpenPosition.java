package io.github.devssw.springquant.backtest;

import java.util.Objects;

// 현재 보유 중인 포지션을 표현하는 레코드입니다 (백테스트 내부에서만 사용하기 위해, public을 제외함)
// 열려있는 포지션의 상태를 저장해두기 위해 필요한 파일입니다
// 2번 캔들에서 시작하고, 5번 캔들에서 끝나는 거래가 있다면, 3번-4번 캔들에서의 포지션 상태 기록이 필요합니다
// 다음에 만들 백테스트 엔진은 시간 순서대로 진입과 청산을 반복하기 때문에 포지션 상태 기록이 필요합니다
record OpenPosition(
        // 진입 시간, 진입 방향, 진입 가격, 수량, 진입 수수료
        long entryTimeMs,
        PositionSide side,
        double entryPrice,
        double quantity,
        double entryFee
) {
    OpenPosition {
        Objects.requireNonNull(side, "진입 방향이 null이어서는 안됩니다.");

        if (entryTimeMs < 0L) {
            throw new IllegalArgumentException("진입 시간이 음수여서는 안됩니다.");
        }

        if (!Double.isFinite(entryPrice) || entryPrice <= 0.0) {
            throw new IllegalArgumentException("진입 가격은 유한해야하고, 0보다 커야합니다.");
        }

        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException("수량은 적어도 0보다 커야합니다.");
        }

        if (!Double.isFinite(entryFee) || entryFee < 0.0) {
            throw new IllegalArgumentException("진입 수수료가 음수여서는 안됩니다.");
        }
    }

    // 진입 시점의 포지션 명목 금액입니다.
    double entryNotional() {
        return entryPrice * quantity;
    }
}
