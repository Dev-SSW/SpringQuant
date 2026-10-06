package io.github.devssw.springquant.indicator.supertrend;

// ST의 기본이 되는 TrueRange 구성

import io.github.devssw.springquant.market.Candle;

import java.util.List;
import java.util.Objects;

/** 이해
 * 캔들의 고가와 저가로 중심 가격 계산
 * True Range로 각 캔들의 실제 변동 폭 계산
 * True Range를 부드럽게 평균 내서 ATR 계산
 * 중심 가격 위아래에 ATR 기반 기준선 생성
 * 현재 추세에 따라 위쪽 또는 아래쪽 선 선택
 * 종가가 선택된 선을 넘으면 추세 방향 전환
 * 선택된 선을 SuperTrend 값으로 출력
 */
public final class TrueRange {
    private TrueRange() {
    }

    public static double[] calculate(List<Candle> candles) {
        // 예외 처리 및 입력 캔들과 같은 사이즈의 결과 배열 생성
        Objects.requireNonNull(candles, "리스트에 담긴 캔들이 Null이어서는 안됩니다.");
        double[] result = new double[candles.size()];

        // 캔들이 비어있을 때는 계산할 값도 없음
        if (candles.isEmpty()) {
            return result;
        }

        // 첫번째 캔들 꺼내기 (위에서 빈 리스트 검사를 한 이유)
        Candle firstCandle = candles.get(0);
        // 첫번째 캔들은 이전 종가가 없으므로 미리 계산 (고가 - 저가)
        result[0] = firstCandle.high() - firstCandle.low();

        // 첫번째는 계산했으므로 1부터 시작
        for (int i = 1; i < candles.size(); i++) {
            // 현재 캔들과 이전 캔들 가져오기
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);
            // 캔들 자체의 가격 범위 (고가 - 저가) -> 가격의 얼마나 움직였는지
            double highLowRange = current.high() - current.low();
            // 현재 고가와 이전 종가의 차이 (절댓값)
            double highPreviousCloseRange = Math.abs(current.high() - previous.close());
            // 현재 저가와 이전 종가의 차이 (절댓값)
            double lowPreviousCloseRange = Math.abs(current.low() - previous.close());
            // 위의 세 개중 가장 큰 값을 선택하여 결과에 저장 (가격의 변동이 가장 큰 부분을 토대로 다음 계산을 하기 위함)
            result[i] = Math.max(highLowRange, Math.max(highPreviousCloseRange, lowPreviousCloseRange));
        }

        return result;
    }
}
