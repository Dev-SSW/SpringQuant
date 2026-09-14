package io.github.devssw.springquant.market.candledata;

import io.github.devssw.springquant.market.Candle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// 바이낸스 거래소에서 실제 캔들을 가져옵니다
@Component
public class BinanceFuturesMarketDataClient implements MarketDataClient {
    private static final String BASE_URL = "https://fapi.binance.com";
    private final RestClient restClient;
    private static final int REQUEST_LIMIT = 1000;
    private static final ParameterizedTypeReference<List<List<Object>>> RESPONSE_TYPE = new ParameterizedTypeReference<>() { };

    // 바이낸스 HTTP API 호출 객체 생성
    public BinanceFuturesMarketDataClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    // 요청값 정상 확인
    private void validateRequest(String symbol, String interval, long startTimeMs, long endTimeMs) {
        Objects.requireNonNull(symbol, "Symbol이 NULL이어서는 안됩니다.");
        Objects.requireNonNull(interval, "Interval이 NULL이어서는 안됩니다.");

        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol이 비어있으면 안됩니다.");
        }

        if (interval.isBlank()) {
            throw new IllegalArgumentException("Interval이 비어있으면 안됩니다.");
        }

        if (startTimeMs < 0L) {
            throw new IllegalArgumentException("시작시간이 음수여서는 안됩니다.");
        }

        if (endTimeMs < startTimeMs) {
            throw new IllegalArgumentException("끝난시간은 시작시간보다 적어도 같거나 커야합니다.");
        }
    }

    private List<List<Object>> fetchPage(String symbol, String interval, long startTimeMs, long endTimeMs) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("startTime", startTimeMs)
                        .queryParam("endTime", endTimeMs)
                        .queryParam("limit", REQUEST_LIMIT) // 최대 1000개의 캔들을 받아옵니다.
                        .build()
                )
                .retrieve()
                .body(RESPONSE_TYPE); // 중첩 리스트 형태로 반환 (바깥쪽 리스트는 여러 개의 캔들을 의미, 안 쪽 리스트는 각 캔들의 정보)
    }

    private long toLong(Object value) {
        return Long.parseLong(value.toString());
    }

    private double toDouble(Object value) {
        return Double.parseDouble(value.toString());
    }

    private Candle toCandle(List<Object> row) {
        if (row == null || row.size() < 5) {
            throw new IllegalStateException("Invalid Binance kline response");
        }

        return new Candle(
                toLong(row.get(0)),
                toDouble(row.get(1)),
                toDouble(row.get(2)),
                toDouble(row.get(3)),
                toDouble(row.get(4))
        );
    }

    @Override
    public List<Candle> fetchCandles(String symbol, String interval, long startTimeMs, long endTimeMs) {
        // 요청 값이 정상인지 확인합니다. (BTC / 4H / 시작시간 / 끝난시간)
        validateRequest(symbol, interval, startTimeMs, endTimeMs);
        // 특정 국가나 언어의 규칙을 적용하지 않도록 지정하는 기준 => Locale.ROOT
        // 공백을 제거하고, 소문자를 대문자로 변경합니다.
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String normalizedInterval = interval.trim();

        // 결과 저장 객체
        List<Candle> candles = new ArrayList<>();

        long nextStartTimeMs = startTimeMs;
        while (nextStartTimeMs <= endTimeMs) {
            // 해당 바이낸스에서 한 페이지를 요청합니다.
            List<List<Object>> response = fetchPage(normalizedSymbol, normalizedInterval, nextStartTimeMs, endTimeMs);

            // 비어있거나 null이면 브레이크
            if (response == null || response.isEmpty()) {
                break;
            }

            // 응답 받은 캔들을 하나씩 읽습니다. (1000개를 받아왔다면 반복문도 1000번 실행됨)
            for (List<Object> row : response) {
                // 종료된 캔들인지 확인합니다.
                if (!isCompletedCandle(row)) {
                    continue;
                }
                // 캔들 값을 꺼냅니다 (거래량 인덱스 5를 제외한 0~4까지) (openTimeMs와 OHLC
                Candle candle = toCandle(row);
                // 혹시나 캔들 시작 시간이 종료 시각을 넘었다면 결과에 넣지 않습니다.
                if (candle.openTimeMs() > endTimeMs) {
                    continue;
                }
                // 정상 캔들을 리스트에 추가합니다.
                candles.add(candle);
            }
            // 응답 마지막 캔들의 시각을 확인합니다. (마지막 캔들의 인덱스(size() -1)의 시작 시간 (0)
            long lastOpenTimeMs = toLong(response.get(response.size() - 1).get(0));
            // 마지막 캔들 이후 시각 (+1)을 저장합니다.
            long followingStartTimeMs = lastOpenTimeMs + 1L;

            // 무한 반복을 막습니다 (시작 시간이 위의 변수보다 큰 경우면 중복될 수 있음)
            if (followingStartTimeMs <= nextStartTimeMs) {
                break;
            }
            // 다음 요청 시각 저장
            nextStartTimeMs = followingStartTimeMs;

            // 마지막 페이지인지 확인 (요청 제한보다 적게 응답을 받았다면, 아직 남은 데이터가 있다는 것)
            if (response.size() < REQUEST_LIMIT) {
                break;
            }
        }
        return List.copyOf(candles);
    }

    // 아직 시간이 종료되지 않은 캔들을 제외시켜야 합니다.
    private boolean isCompletedCandle(List<Object> row) {
        if (row == null || row.size() < 7) {
            throw new IllegalStateException("잘못된 캔들입니다.");
        }
        // 요청 한 후 받은 캔들 응답에서 인덱스 6은 종료 시각이 저장되어 있습니다.
        long closeTimeMs = toLong(row.get(6));
        long currentTimeMs = System.currentTimeMillis();
        // 캔들이 끝나있는지 확인합니다.
        return closeTimeMs < currentTimeMs;
    }
}
