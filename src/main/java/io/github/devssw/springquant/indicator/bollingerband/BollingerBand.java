package io.github.devssw.springquant.indicator.bollingerband;

/**
 * @param basis        중심선인 단순이동평균
 * @param upper        볼린저밴드 상단선
 * @param lower        볼린저밴드 하단선
 * @param widthPercent 중심선 대비 밴드 폭의 백분율
*/
public record BollingerBand(
        double basis,
        double upper,
        double lower,
        double widthPercent
) {
    public static BollingerBand unavailable() {
        // NaN 처리
        return new BollingerBand(
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
    }
    
    public boolean isAvailable() {
        // 유한한 숫자인지 확인하여 boolean 값 제공
        return Double.isFinite(basis)
                && Double.isFinite(upper)
                && Double.isFinite(lower)
                && Double.isFinite(widthPercent);
    }
}
