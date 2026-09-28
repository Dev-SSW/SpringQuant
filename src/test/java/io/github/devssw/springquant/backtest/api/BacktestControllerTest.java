package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.BacktestTestFixtures;
import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.backtest.model.EntryMode;
import io.github.devssw.springquant.backtest.service.BacktestService;
import io.github.devssw.springquant.common.api.GlobalExceptionHandler;
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
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 백테스트실행응답() throws Exception {
        BacktestConfig expectedConfig = new BacktestConfig(10000.0, 0.0004, 0.0, 1, 1.0, 10, 1.0, 0.0, 10, 1.0, EntryMode.MA_BOUNCE, 0.0, 0.0, true, BacktestTestFixtures.riskManagementConfig(), BacktestTestFixtures.liquidationConfig());
        BacktestResult expectedResult = BacktestResult.from(10000.0, List.of());
        BacktestChartPoint chartPoint = new BacktestChartPoint(1000L, 100.0, 110.0, 90.0, 105.0, null, null, null, null, null);
        BacktestResponse expectedResponse = new BacktestResponse("BTCUSDT", "4h", 1000L, 10000L, List.of(chartPoint), expectedResult);

        when(backtestService.run("BTCUSDT", "4h", 1000L, 10000L, expectedConfig)).thenReturn(expectedResponse);

        String requestBody =
                """
                {
                  "symbol": "BTCUSDT",
                  "interval": "4h",
                  "startTimeMs": 1000,
                  "endTimeMs": 10000,
                  "initialBalance": 10000.0,
                  "feeRate": 0.0004,
                  "slippageRate": 0.0,
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
                  "oneEntryPerTrend": true,
                          "riskManagement": {
                            "stopLossPercent": 2.0,
                            "takeProfitLevels": [{"targetPercent": 1.0,"closeRatio": 0.5},{"targetPercent": 2.0,"closeRatio": 0.3},{"targetPercent": 3.0,"closeRatio": 0.2}],
                            "stopPromotionMode": "BE_AFTER_TP2"
                          },
                          "liquidation": {
                            "maintenanceMarginRate": 0.01,
                            "maintenanceAmount": 0.0,
                             "rejectEntryIfLiquidationBeforeStop": true,
                             "stopBufferPercent": 0.0
                          }
                }
                """;

        mockMvc.perform(
                post("/api/backtests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.interval").value("4h"))
                .andExpect(jsonPath("$.startTimeMs").value(1000))
                .andExpect(jsonPath("$.endTimeMs").value(10000))
                .andExpect(jsonPath("$.chartPoints").isArray())
                .andExpect(jsonPath("$.chartPoints.length()").value(1))
                .andExpect(jsonPath("$.chartPoints[0].timeMs").value(1000))
                .andExpect(jsonPath("$.chartPoints[0].open").value(100.0))
                .andExpect(jsonPath("$.chartPoints[0].high").value(110.0))
                .andExpect(jsonPath("$.chartPoints[0].low").value(90.0))
                .andExpect(jsonPath("$.chartPoints[0].close").value(105.0))
                .andExpect(jsonPath("$.result.initialBalance").value(10000.0))
                .andExpect(jsonPath("$.result.finalBalance").value(10000.0))
                .andExpect(jsonPath("$.result.maxDrawdownPercent").value(0.0));

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
                  "slippageRate": 0.0,
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
                  "oneEntryPerTrend": true,
                          "riskManagement": {
                            "stopLossPercent": 2.0,
                            "takeProfitLevels": [{"targetPercent": 1.0,"closeRatio": 0.5},{"targetPercent": 2.0,"closeRatio": 0.3},{"targetPercent": 3.0,"closeRatio": 0.2}],
                            "stopPromotionMode": "BE_AFTER_TP2"
                          },
                          "liquidation": {
                            "maintenanceMarginRate": 0.01,
                            "maintenanceAmount": 0.0,
                             "rejectEntryIfLiquidationBeforeStop": true,
                             "stopBufferPercent": 0.0
                          }
                }
                """;

        mockMvc.perform(
                        post("/api/backtests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors.symbol").value("symbol은 필수입니다."));

        verifyNoInteractions(backtestService);
    }

    @Test
    void 잘못된JSON요청() throws Exception {
        String requestBody =
                """
                {
                  "symbol": "BTCUSDT",
                  "interval": "4h",
                }
                """;

        mockMvc.perform(
                        post("/api/backtests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청 본문의 형식 또는 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path").value("/api/backtests"));

        verifyNoInteractions(backtestService);
    }

    @Test
    void 수수료율이1이상이면요청을거부() throws Exception {
        String requestBody =
                """
                {
                  "symbol": "BTCUSDT",
                  "interval": "4h",
                  "startTimeMs": 1000,
                  "endTimeMs": 10000,
                  "initialBalance": 10000.0,
                  "feeRate": 1.0,
                  "slippageRate": 0.0,
                  "leverage": 1,
                  "marginUsageRate": 0.1,
                  "bollingerPeriod": 12,
                  "bollingerMultiplier": 1.5,
                  "bollingerWidthMinPercent": 0.0,
                  "superTrendLength": 8,
                  "superTrendFactor": 2.5,
                  "entryMode": "MA_BOUNCE",
                  "maTolerancePercent": 0.0,
                  "bbTolerancePercent": 0.0,
                  "oneEntryPerTrend": true,
                  "riskManagement": {
                    "stopLossPercent": 3.0,
                    "takeProfitLevels": [
                      {
                        "targetPercent": 2.0,
                        "closeRatio": 1.0
                      }
                    ],
                    "stopPromotionMode": "NONE"
                  },
                  "liquidation": {
                    "maintenanceMarginRate": 0.01,
                    "maintenanceAmount": 0.0,
                    "rejectEntryIfLiquidationBeforeStop": true,
                    "stopBufferPercent": 0.0
                  }
                }
                """;

        mockMvc.perform(post("/api/backtests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.feeRate").value("feeRate는 1보다 작아야 합니다."));

        verifyNoInteractions(backtestService);
    }
}
