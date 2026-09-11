package io.github.devssw.springquant.backtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BacktestControllerTest {

    @Mock private BacktestService backtestService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BacktestController controller = new BacktestController(backtestService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void 백테스트실행응답() throws Exception {
        BacktestConfig expectedConfig = new BacktestConfig(10000.0, 0.0004, 1, 1.0, 10, 1.0, 0.0, 10, 1.0, EntryMode.MA_BOUNCE, 0.0, 0.0, true);
        BacktestResult expectedResult = BacktestResult.from(10000.0, List.of());

        when(backtestService.run("BTCUSDT", "4h", 1000L, 10000L, expectedConfig)).thenReturn(expectedResult);

        String requestBody =
                """
                {
                  "symbol": "BTCUSDT",
                  "interval": "4h",
                  "startTimeMs": 1000,
                  "endTimeMs": 10000,
                  "initialBalance": 10000.0,
                  "feeRate": 0.0004,
                  "leverage": 1,
                  "marginUsageRate": 1.00,
                  "bollingerPeriod": 10,
                  "bollingerMultiplier": 1.0,
                  "bollingerWidthMinPercent": 0.0,
                  "superTrendLength": 10,
                  "superTrendFactor": 1.0,
                  "entryMode": "MA_BOUNCE",
                  "maTolerancePercent": 0.0,
                  "bbTolerancePercent": 0.0,
                  "oneEntryPerTrend": true
                }
                """;

        mockMvc.perform(
                post("/api/backtests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialBalance").value(10000.0))
                .andExpect(jsonPath("$.finalBalance").value(10000.0));

        verify(backtestService).run("BTCUSDT", "4h", 1000L, 10000L, expectedConfig);
    }

    @Test
    void 심볼이없을때예외() throws Exception {
        String requestBody =
                """
                {
                  "symbol": "",
                  "interval": "4h",
                  "startTimeMs": 1000,
                  "endTimeMs": 10000,
                  "initialBalance": 10000.0,
                  "feeRate": 0.0004,
                  "leverage": 1,
                  "marginUsageRate": 1.00,
                  "bollingerPeriod": 10,
                  "bollingerMultiplier": 1.0,
                  "bollingerWidthMinPercent": 0.0,
                  "superTrendLength": 10,
                  "superTrendFactor": 1.0,
                  "entryMode": "MA_BOUNCE",
                  "maTolerancePercent": 0.0,
                  "bbTolerancePercent": 0.0,
                  "oneEntryPerTrend": true
                }
                """;

        mockMvc.perform(
                post("/api/backtests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest());
        verifyNoInteractions(backtestService);
    }
}
