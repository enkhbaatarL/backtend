package com.sk.domain.shared.enums.nextgen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// note: tmm_mi_impact_result_m / tmm_mi_impact_driver_m 의 impact_short/mid/long ENUM 값.
@Getter
@RequiredArgsConstructor
public enum ImpactGrade {

    UP_HIGH("상승_상", 3, MarketDirection.BULLISH, MarketImpactLevel.HIGH),
    UP_MID("상승_중", 2, MarketDirection.BULLISH, MarketImpactLevel.MEDIUM),
    UP_LOW("상승_하", 1, MarketDirection.BULLISH, MarketImpactLevel.LOW),
    NONE("영향없음", 0, MarketDirection.NEGLIGIBLE, null),
    DOWN_LOW("하락_하", -1, MarketDirection.BEARISH, MarketImpactLevel.LOW),
    DOWN_MID("하락_중", -2, MarketDirection.BEARISH, MarketImpactLevel.MEDIUM),
    DOWN_HIGH("하락_상", -3, MarketDirection.BEARISH, MarketImpactLevel.HIGH);

    // DB ENUM 값 (한글)
    private final String code;
    private final int score;
    private final MarketDirection direction;
    private final MarketImpactLevel level;

    public static ImpactGrade fromCode(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        for (ImpactGrade grade : values()) {
            if (grade.code.equals(dbValue)) {
                return grade;
            }
        }
        return null;
    }

    private static ImpactGrade fromScore(int score) {
        for (ImpactGrade grade : values()) {
            if (grade.score == score) {
                return grade;
            }
        }
        return null;
    }

    /**
     * 중장기 병합 규칙:
     *  - 둘 다 NULL -> null (중장기 없음)
     *  - 한쪽만 NULL -> 남은 값 그대로
     *  - 둘 다 존재 -> (중기 + 장기) / 2, 절대값 반올림 후 부호 유지
     *
     *  ref: https://app.notion.com/p/Agent-36f263ff004c808f972fc9d2a822fc2b?v=342263ff004c80b18ade000cd9fd9ff4&source=copy_link
     *
     * @param mid
     * @param longTerm
     * @return
     */
    public static ImpactGrade mergeMidLong(ImpactGrade mid, ImpactGrade longTerm) {
        if (mid == null && longTerm == null) return null;
        if (mid == null) return longTerm;
        if (longTerm == null) return mid;
        int sum = mid.score + longTerm.score;
        int magnitude = (int) Math.round(Math.abs(sum) / 2.0);
        return fromScore(Integer.signum(sum) * magnitude);
    }

    // 방향 무관 영향 강도 (상/중/하 -> 3/2/1, 영향없음 -> 0). 목록의 단기 vs 중장기 우열 비교에 사용.
    public int magnitude() {
        return Math.abs(score);
    }

}
