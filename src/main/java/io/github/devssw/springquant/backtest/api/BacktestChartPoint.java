package io.github.devssw.springquant.backtest.api;

/**
 * 하나의 캔들 시점에 화면 차트로 표시할 데이터입니다.
 *  timeMs                           캔들 시작 시각
 *  open                              시가
 *  high                               고가
 *  low                                저가
 *  close                             종가
 *  bollingerBasis               볼린저밴드 중심선
 *  bollingerUpper              볼린저밴드 상단선
 *  bollingerLower              볼린저밴드 하단선
 *  superTrendValue           SuperTrend 선의 가격w
 *  superTrendDirection     SuperTrend 방향
 */
public record BacktestChartPoint(
        long timeMs,
        double open,
        double high,
        double low,
        double close,
        Double bollingerBasis,
        Double bollingerUpper,
        Double bollingerLower,
        Double superTrendValue,
        String superTrendDirection
) {
}
