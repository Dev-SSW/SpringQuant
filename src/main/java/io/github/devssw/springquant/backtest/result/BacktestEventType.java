package io.github.devssw.springquant.backtest.result;

// 백테스트 도중 발생한 사건을 기록하기 위한 이벤트
public enum BacktestEventType {
    // 포지션 진입
    ENTRY,
    // 부분 또는 전체 익절
    TAKE_PROFIT,
    // 손절가 승격 완료
    STOP_PROMOTION,
    // 시가가 승격가를 침범해서 승격 보류 (보류가 된 내역이 상황에 있었는지 확인하기 위함)
    STOP_PROMOTION_DEFERRED,
    // 손절
    STOP_LOSS,
    // SuperTrend 반전 청산
    SUPER_TREND_EXIT,
    // 강제청산
    LIQUIDATION,
    // 마지막 캔들에서 종료
    END_OF_DATA_EXIT
}
