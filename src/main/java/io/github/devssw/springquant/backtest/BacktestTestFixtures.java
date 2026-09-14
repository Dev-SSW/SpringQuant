package io.github.devssw.springquant.backtest;

import java.util.List;

final class BacktestTestFixtures {
    private BacktestTestFixtures() {
    }

    static RiskManagementConfig riskManagementConfig() {
        return new RiskManagementConfig(
                2.0,
                List.of(
                        new TakeProfitLevel(1.0, 0.5),
                        new TakeProfitLevel(2.0, 0.3),
                        new TakeProfitLevel(3.0, 0.2)
                ),
                true,
                2.0
        );
    }
}