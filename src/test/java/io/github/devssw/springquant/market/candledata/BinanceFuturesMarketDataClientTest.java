package io.github.devssw.springquant.market.candledata;

import io.github.devssw.springquant.market.Candle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 바이낸스 거래소와의 연결을 테스트합니다
public class BinanceFuturesMarketDataClientTest {
    private MockRestServiceServer server;
    private BinanceFuturesMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BinanceFuturesMarketDataClient(builder);
    }

    @Test
    void fetchesAndConvertsCandles() {
        // 응답 배열을 가정하여 정의
        String responseBody =
                """
                [[1000,"100.0","110.0","90.0","105.0","50.0",1999],[2000,"105.0","120.0","100.0","115.0","60.0",2999]]
                """;
        // 다음 주소로 요청
        server.expect(once(), requestTo("https://fapi.binance.com"
                                        + "/fapi/v1/klines"
                                        + "?symbol=BTCUSDT"
                                        + "&interval=4h"
                                        + "&startTime=1000"
                                        + "&endTime=2000"
                                        + "&limit=1000")
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Candle> result = client.fetchCandles("btcusdt", "4h", 1000L, 2000L);

        // 응답에 캔들을 두 개로 정의했으니, 결과는 두 개여야 함
        assertEquals(2, result.size());
        // 첫 결과를 저장
        Candle first = result.get(0);

        // 허용 오차를 포함하여 검증
        assertEquals(1000L, first.openTimeMs());
        assertEquals(100.0, first.open(), 1e-12);
        assertEquals(110.0, first.high(), 1e-12);
        assertEquals(90.0, first.low(), 1e-12);
        assertEquals(105.0, first.close(), 1e-12);

        Candle second = result.get(1);

        // 두 번째 결과 검증
        assertEquals(2000L, second.openTimeMs());
        assertEquals(105.0, second.open(), 1e-12);
        assertEquals(120.0, second.high(), 1e-12);
        assertEquals(100.0, second.low(), 1e-12);
        assertEquals(115.0, second.close(), 1e-12);

        server.verify();
    }

    @Test
    void 시작시간보다_종료시간이작을때_예외() {
        assertThrows(IllegalArgumentException.class, () -> client.fetchCandles("BTCUSDT", "4h", 2000L, 1000L));
    }

    @Test
    void 빈Symbol예외() {
        assertThrows(IllegalArgumentException.class, () -> client.fetchCandles(" ", "4h", 1000L, 2000L));
    }

    @Test
    void 빈Interval예외() {
        assertThrows(IllegalArgumentException.class, () -> client.fetchCandles("BTCUSDT", " ", 1000L, 2000L));
    }

    @Test
    void 종료되지않은캔들포함() {
        // 1분 뒤 종료시각이 끝나도록 설정
        long futureCloseTimeMs = System.currentTimeMillis() + 60000L;

        String responseBody =
            """
            [[1000,"100.0","110.0","90.0","105.0","50.0",1999],[2000,"105.0","120.0","100.0","115.0","60.0",%d]]
            """
                    .formatted(futureCloseTimeMs);

        server.expect(once(), requestTo("https://fapi.binance.com"
                                + "/fapi/v1/klines"
                                + "?symbol=BTCUSDT"
                                + "&interval=4h"
                                + "&startTime=1000"
                                + "&endTime=2000"
                                + "&limit=1000")
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Candle> result = client.fetchCandles("BTCUSDT", "4h", 1000L, 2000L);

        // 1분 뒤로 설정했으므로, 제외되어 결과는 2개가 아닌 1개여야 합니다.
        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).openTimeMs());

        server.verify();
    }
}
