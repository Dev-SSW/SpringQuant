package io.github.devssw.springquant.backtest.engine;

import io.github.devssw.springquant.backtest.model.BacktestConfig;
import io.github.devssw.springquant.backtest.model.PositionSide;
import io.github.devssw.springquant.indicator.bollingerband.BollingerBand;
import io.github.devssw.springquant.indicator.supertrend.SuperTrend;
import io.github.devssw.springquant.market.Candle;

// BacktestEngine에서 분리 - 지표와 캔들 정보를 이용해 진입 방향과 SuperTrend 청산 신호를 판단하는 클래스입니다
final class EntrySignalEvaluator {
    // 익절에 도달했는지 여부를 확인합니다
    boolean shouldExitBySuperTrend(OpenPosition position, SuperTrend superTrend) {
        return switch (position.getSide()) {
            case LONG -> superTrend.signal() == SuperTrend.Signal.SHORT;
            case SHORT -> superTrend.signal() == SuperTrend.Signal.LONG;
        };
    }

    // 진입 조건 판단 (진입 방향 판단)
    PositionSide determineEntrySide(Candle candle, BollingerBand band, SuperTrend superTrend, BacktestConfig config) {
        return switch (config.entryMode()) {
            // ST 전환 신호에 진입 모드일 경우
            case ST_FLIP -> positionSideFromSignal(superTrend.signal());

            // MA 반등 신호에 진입 모드일 경우
            case MA_BOUNCE -> {
                // 밴드 폭 최소 조건 만족 확인
                if (!hasSufficientBandWidth(band, config)) {
                    yield null;
                }
                // 위의 조건 만족 시 중심선 반등 조건 확인
                yield determineMaBounceSide(candle, band, superTrend, config.maTolerancePercent());
            }

            // BB 충돌/돌파 신호에 진입 모드일 경우
            case BB_PIERCE -> {
                // 밴드 폭 최소 조건 만족 확인
                if (!hasSufficientBandWidth(band, config)) {
                    yield null;
                }
                // 위의 조건 만족 시 상단/하단 충돌 조건 확인
                yield determineBbPierceSide(candle, band, superTrend, config.bbTolerancePercent());
            }
        };
    }

    // SuperTrend 신호를 포지션 방향으로 변환
    private PositionSide positionSideFromSignal(SuperTrend.Signal signal) {
        return switch (signal) {
            case LONG -> PositionSide.LONG;
            case SHORT -> PositionSide.SHORT;
            case NONE -> null;
        };
    }

    // 설정한 볼린저밴드 최소 폭을 충족하는지 확인
    private boolean hasSufficientBandWidth(BollingerBand band, BacktestConfig config) {
        return band.isAvailable() && band.widthPercent() >= config.bollingerWidthMinPercent();
    }

    // 볼린저밴드 중심선 반등 조건 판단
    private PositionSide determineMaBounceSide(Candle candle, BollingerBand band, SuperTrend superTrend, double tolerancePercent) {
        double tolerance = tolerancePercent / 100.0;
        // ST 방향이 상승일 때 (ST 선은 아래에 있음) 허용 오차를 통해 반등을 확인
        if (superTrend.direction() == SuperTrend.Direction.UP) {
            boolean touchedBasis = candle.low() <= band.basis() * (1.0 + tolerance);
            boolean recoveredAboveBasis = candle.close() >= band.basis();
            if (touchedBasis && recoveredAboveBasis) {
                return PositionSide.LONG;
            }
        }
        // ST 방향이 아래 일 때
        if (superTrend.direction() == SuperTrend.Direction.DOWN) {
            boolean touchedBasis = candle.high() >= band.basis() * (1.0 - tolerance);
            boolean recoveredBelowBasis = candle.close() <= band.basis();
            if (touchedBasis && recoveredBelowBasis) {
                return PositionSide.SHORT;
            }
        }
        return null;
    }

    // 밴드 상단/하단 접근 조건 확인
    private PositionSide determineBbPierceSide(Candle candle, BollingerBand band, SuperTrend superTrend, double tolerancePercent) {
        double tolerance = tolerancePercent / 100.0;
        // ST 방향이 상승일 때 (ST 선은 아래에 있음) 허용 오차를 통해 충돌을 확인
        if (superTrend.direction() == SuperTrend.Direction.UP) {
            boolean touchedLowerBand = candle.low() <= band.lower() * (1.0 + tolerance);
            if (touchedLowerBand) {
                return PositionSide.LONG;
            }
        }

        if (superTrend.direction() == SuperTrend.Direction.DOWN) {
            boolean touchedUpperBand = candle.high() >= band.upper() * (1.0 - tolerance);
            if (touchedUpperBand) {
                return PositionSide.SHORT;
            }
        }
        return null;
    }
}
