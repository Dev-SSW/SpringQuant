package io.github.devssw.springquant.backtest.api;

import io.github.devssw.springquant.backtest.result.BacktestResult;
import io.github.devssw.springquant.backtest.service.BacktestService;
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

    @PostMapping
    public BacktestResult run(@Valid @RequestBody BacktestRequest request) {
        return backtestService.run(request.symbol(), request.interval(), request.startTimeMs(), request.endTimeMs(), request.toConfig());
    }
}