package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// note: tmm_mi_impact_result_m / tmm_mi_impact_driver_m 의 impact_short/mid/long ENUM 값.
//  {방향_강도}를 하나의 값으로 인코딩하며, -3 ~ +3 점수로 매핑해 중장기 병합(평균)에 사용한다.
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


}
