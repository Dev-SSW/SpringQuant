package io.github.devssw.springquant.market;

public record Candle (
        /** 캔들 시작 시간, 시가, 고가, 저가, 종가 */
        long openTimeMs,
        double open,
        double high,
        double low,
        double close
) {
    public Candle {
        // 타임 스탬프가 음수인 경우 거부
        if (openTimeMs < 0) {
            throw new IllegalArgumentException("openTimeMs가 0 또는 양수여야 합니다.");
        }

        validatePrice("open", open);
        validatePrice("high", high);
        validatePrice("low", low);
        validatePrice("close", close);

        // 고가가 시가와 종가보다 작은지
        if (high < Math.max(open, close)) {
            throw new IllegalArgumentException(
                    "고가는 시가와 종가보다 크거나 같아야 합니다."
            );
        }

        // 저가가 시가와 종가보다 큰지
        if (low > Math.min(open, close)) {
            throw new IllegalArgumentException(
                    "저가는 시가와 종가보다 작거나 같아야 합니다."
            );
        }
    }

    private static void validatePrice(String name, double price) {
        // 가격이 유한한 숫자인지 검사 (Open, High, Low, Close)
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException(
                    name + " 유한한 숫자여야 합니다."
            );
        }

        // 가격이 NaN이 아닌지 명시적으로 검사 (Open, High, Low, Close)
        if (price <= 0.0) {
            throw new IllegalArgumentException(
                    name + " 0보다 큰 숫자여야 합니다."
            );
        }
    }
}
