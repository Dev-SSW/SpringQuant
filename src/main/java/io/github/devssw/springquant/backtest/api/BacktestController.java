package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.backtest.service.BacktestService;
import io.github.devssw.springquant.common.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backtests")
public class BacktestController {
    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @Operation(summary = "백테스트 실행", description = "지정한 기간의 캔들 데이터를 조회하고 입력된 전략 및 위험관리 설정으로 백테스트를 실행합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "백테스트 실행 성공",
                    content = @Content(schema = @Schema(implementation = BacktestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 또는 JSON 형식 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping
    public BacktestResponse run(@Valid @RequestBody BacktestRequest request) {
        return backtestService.run(
                request.symbol(),
                request.interval(),
                request.startTimeMs(),
                request.endTimeMs(),
                request.toConfig()
        );
    }
}