package io.github.devssw.springquant.backtest;

public enum EntryMode {
    // 추세 전환 시 진입, MA 반등 시 진입, BB 충돌/돌파 진입
    // 현재는 MA_BOUNCE 만을 사용합니다
    ST_FLIP,
    MA_BOUNCE,
    BB_PIERCE
}
