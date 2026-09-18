package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.*;
import io.github.devssw.springquant.indicator.bollingerband.BollingerBand;
import io.github.devssw.springquant.indicator.bollingerband.BollingerBandCalculator;
import io.github.devssw.springquant.indicator.supertrend.SuperTrend;
import io.github.devssw.springquant.indicator.supertrend.SuperTrendCalculator;
import io.github.devssw.springquant.market.Candle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public final class BacktestEngine {
    private final EntrySignalEvaluator entrySignalEvaluator = new EntrySignalEvaluator();
    private final PositionRiskProcessor positionRiskProcessor = new PositionRiskProcessor();
    private final ExecutionPriceCalculator executionPriceCalculator = new ExecutionPriceCalculator();
    // 가상 포지션 생성
    private OpenPosition enterPosition(Candle candle, PositionSide side, double balance, BacktestConfig config) {
        // 진입 가격은 슬리피지를 포함하여 계산
        double entryPrice = executionPriceCalculator.entryPrice(side, candle.open(), config.slippageRate());
        double margin = balance * config.marginUsageRate();
        double notional = margin * config.leverage();
        double quantity = notional / entryPrice;
        double entryFee = notional * config.feeRate();

        return new OpenPosition(
                candle.openTimeMs(),
                side,
                entryPrice,
                quantity,
                margin,
                entryFee,
                config.riskManagement()
        );
    }

    // 모든 수량이 청산된 포지션을 완료 거래로 변환
    private BacktestTrade completeTrade(OpenPosition position, double balanceAfter) {
        if (!position.isClosed()) {
            throw new IllegalStateException("포지션 수량이 남아 있어 거래를 완료할 수 없습니다.");
        }

        return new BacktestTrade(
                position.getEntryTimeMs(),
                position.getSide(),
                position.getEntryPrice(),
                position.getInitialQuantity(),
                position.getEntryFee(),
                position.getExits(),
                balanceAfter
        );
    }

    // 포지션 청산 및 손익 계산
    private BacktestTrade closePosition(
            OpenPosition position,
            long exitTimeMs,
            double marketPrice,
            double currentBalance,
            double feeRate,
            double slippageRate,
            ExitReason exitReason
    ) {
        double exitPrice = executionPriceCalculator.exitPrice(position.getSide(), marketPrice, slippageRate, exitReason);

        BacktestExit exit = position.closeQuantity(
                exitTimeMs,
                exitPrice,
                position.getRemainingQuantity(),
                feeRate,
                exitReason
        );

        double balanceAfter = currentBalance + exit.realizedPnl();
        return completeTrade(position, balanceAfter);
    }

    // 백테스트 전체 실행
    public BacktestResult run(List<Candle> candles, BacktestConfig config) {
        Objects.requireNonNull(candles, "candles이 null이면 안됩니다.");
        Objects.requireNonNull(config, "config가 null이면 안됩니다.");

        if (candles.size() < 2) {
            return BacktestResult.from(config.initialBalance(), List.of());
        }

        // 볼린저밴드와 슈퍼트렌드 계산
        List<BollingerBand> bands = BollingerBandCalculator.calculate(candles, config.bollingerPeriod(), config.bollingerMultiplier());
        List<SuperTrend> superTrends = SuperTrendCalculator.calculate(candles, config.superTrendLength(), config.superTrendFactor());

        // 완료된 거래 목록, 가상 잔고, 가상 포지션, 마지막으로 진입했던 ST 추세 번호
        List<BacktestTrade> trades = new ArrayList<>();
        double balance = config.initialBalance();
        OpenPosition openPosition = null;
        long lastEnteredTrendId = -1L;

        // 캔들을 시간 순서대로 조회합니다
        for (int i = 1; i < candles.size(); i++) {
            // 현재 캔들입니다 (i)
            Candle executionCandle = candles.get(i);
            // 신호를 확인 할 캔들은 이전에 완료된 캔들입니다 (i-1)
            Candle signalCandle = candles.get(i - 1);
            BollingerBand band = bands.get(i - 1);
            SuperTrend superTrend = superTrends.get(i - 1);

            // 사용 가능 여부 확인 (캔들이 1개라면 코드를 건너띄고, 다음 캔들로 이동함)
            if (!superTrend.isAvailable()) {
                continue;
            }

            // 새 캔들의 시가가 이미 손절가 또는 강제청산가를 넘었다면
            // SuperTrend 반전 청산보다 먼저 처리합니다.
            if (openPosition != null) {
                double realizedPnl = positionRiskProcessor.processOpenSafetyExit(openPosition, executionCandle, config.feeRate(), config.slippageRate(), config.liquidation());

                balance += realizedPnl;

                if (openPosition.isClosed()) {
                    BacktestTrade completedTrade = completeTrade(openPosition, balance);
                    trades.add(completedTrade);
                    openPosition = null;
                    // 시가 안전 청산이 발생한 캔들에서는 새로운 포지션에 다시 진입하지 않습니다.
                    continue;
                }
            }

            // 현재 보유 중인 포지션과 반대되는 SuperTrend 전환 신호가 발생하면 청산합니다.
            if (openPosition != null && entrySignalEvaluator.shouldExitBySuperTrend(openPosition, superTrend)) {
                // 내부적으로 포지션을 종료하고 손익을 계산합니다 (closePosition())
                BacktestTrade completedTrade = closePosition(
                        openPosition,
                        executionCandle.openTimeMs(),
                        executionCandle.open(),
                        balance,
                        config.feeRate(),
                        config.slippageRate(),
                        ExitReason.SUPER_TREND
                );
                // 청산 후 완료된 거래의 손익을 계산하고, 반환합니다.
                trades.add(completedTrade);
                balance = completedTrade.balanceAfter();
                openPosition = null;
            }

            // 새로운 진입 방향을 판단합니다 (만약 진입 조건이 충족되지 않는다면 null이 반환되어 다음 캔들로 넘어갑니다)
            // Direction : 시장의 추세 상태 / Signal : 추세가 방금 바뀌었음을 나타냄 / PositionSide : 백테스트가 실제로 보유중인 방향
            // Direction이 Long으로 바뀌면, Signal은 처음에 Long을 반환합니다. 그리고 Direction이 다시 바뀌기 전까지는 None 상태를 유지합니다
            // 아직 Direction이 유지중이라면, 조건을 충족하는 순간 (determineEntrySide) PositionSide가 진입을 시도합니다
            // 보유중인 상황에서 Direction의 추세가 바뀌면, Signal은  Short를 반환하고, PositionSide는 청산됩니다
            PositionSide entrySide = entrySignalEvaluator.determineEntrySide(signalCandle, band, superTrend, config);
            // oneEntryPerTrend()는 추세 당 한 번의 진입만을 허용할지 말지에 대한 설정입니다. True이면 추세 당 한 번만 허용합니다.
            // trendid는 추세가 바뀌면 증가하는 추세 번호입니다. 해당 추세 번호가 마지막 추세 번호와 같다면, 같은 추세에서 이미 한 번 진입했다는 의미입니다.
            boolean alreadyEnteredInTrend = config.oneEntryPerTrend() && lastEnteredTrendId == superTrend.trendId();

            // 가상 포지션을 생성합니다
            if (openPosition == null && entrySide != null && !alreadyEnteredInTrend && balance > 0.0) {
                openPosition = enterPosition(
                        executionCandle,
                        entrySide,
                        balance,
                        config
                );
                // 진입 수수료 차감
                balance -= openPosition.getEntryFee();
                // 추세 번호를 주입합니다.
                lastEnteredTrendId = superTrend.trendId();
            }

            // 현재 실행 캔들에서 단계별 익절 가격에 도달했는지 확인
            if (openPosition != null) {
                double realizedPnl = positionRiskProcessor.process(
                        openPosition,
                        executionCandle,
                        config.feeRate(),
                        config.slippageRate(),
                        config.liquidation()
                );
                balance += realizedPnl;
                // 마지막 익절 단계까지 실행되어 수량이 모두 청산된 경우
                if (openPosition.isClosed()) {
                    BacktestTrade completedTrade = completeTrade(openPosition, balance);
                    trades.add(completedTrade);
                    openPosition = null;
                }
            }
        }

        // 데이터가 끝났는데 포지션이 남아 있다면 마지막 캔들의 종가로 강제 청산합니다. (테스트의 마지막 캔들에서도 포지션이 남아 있는 경우에 대한 예외)
        if (openPosition != null) {
            Candle lastCandle = candles.get(candles.size() - 1);
            BacktestTrade completedTrade = closePosition(
                    openPosition,
                    lastCandle.openTimeMs(),
                    lastCandle.close(),
                    balance,
                    config.feeRate(),
                    config.slippageRate(),
                    ExitReason.END_OF_DATA
            );
            trades.add(completedTrade);
        }
        return BacktestResult.from(config.initialBalance(), trades);
    }
}