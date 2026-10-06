package io.github.devssw.springquant.backtest.model;

public enum ExitReason {
    // 포지션이 종료된 이유를 반환합니다. (ST 전환, 손절, 익절, 청산, 테스트 데이터가 끝남)
    SUPER_TREND,
    STOP_LOSS,
    TAKE_PROFIT,
    LIQUIDATION,
    END_OF_DATA
}
