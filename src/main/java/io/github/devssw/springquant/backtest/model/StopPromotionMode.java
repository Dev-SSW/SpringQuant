package io.github.devssw.springquant.backtest.model;

// 익절 단계에 따라 SL 수정 기능 (여러 가지 모드로 테스트 가능)
public enum StopPromotionMode {
    // 손절가를 승격하지 않음
    NONE,
    // 1차 익절 후 진입가로 승격
    BE_AFTER_TP1,
    // 2차 익절 후 진입가로 승격
    BE_AFTER_TP2,
    // 2차 익절 후 1차 익절가로 승격
    TP1_AFTER_TP2,
    // 1차 익절 후 진입가, 2차 익절 후 1차 익절가로 승격
    BE_TP1_AFTER_TP2,
    // 1차 익절 후 진입가와 1차 익절가의 중간으로 승격
    LOCK_HALF_TP1_AFTER_TP1
}
