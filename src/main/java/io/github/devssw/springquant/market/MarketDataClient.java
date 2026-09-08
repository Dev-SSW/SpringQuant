package io.github.devssw.springquant.market;

import java.util.List;

// 확장성을 위해 interface로 구현 (여러 거래소의 데이터를 가져올 수 있으므로)
public interface MarketDataClient {
    // 거래 종목 / 캔들 시간대 / 조회 시작 시간 / 조회 종료 시각
    List<Candle> fetchCandles(String symbol, String interval, long startTimeMs, long endTimeMs);
}
