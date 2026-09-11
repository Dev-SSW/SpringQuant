package io.github.devssw.springquant.backtest;

public enum PositionSide {
    LONG, SHORT;

    // 현재의 반대 방향을 반환합니다
    public PositionSide opposite() {
        return this == LONG ? SHORT : LONG;
    }
}
