package io.github.devssw.springquant.backtest;

import io.github.devssw.springquant.backtest.engine.LiquidationConfig;
import io.github.devssw.springquant.backtest.model.RiskManagementConfig;
import io.github.devssw.springquant.backtest.model.TakeProfitLevel;

import java.util.List;

public final class BacktestTestFixtures {
    private BacktestTestFixtures() {
    }

    public static RiskManagementConfig riskManagementConfig() {
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

    public static LiquidationConfig liquidationConfig() {
        return new LiquidationConfig(0.01, 0.0, true, 0.0);
    }
}