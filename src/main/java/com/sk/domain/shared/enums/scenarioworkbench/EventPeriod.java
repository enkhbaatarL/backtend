package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventPeriod {
    RECENT_7_DAYS("RECENT_7_DAYS", "최근 7일"),
    ONE_MONTH("ONE_MONTH", "1개월"),
    THREE_MONTHS("THREE_MONTHS", "3개월"),
    SIX_MONTHS("SIX_MONTHS", "6개월"),
    TWELVE_MONTHS("TWELVE_MONTHS", "12개월"),
    ALL("ALL", "전체 기간"),
    CUSTOM("CUSTOM", "사용자 지정");

    private final String code;
    private final String description;
}
