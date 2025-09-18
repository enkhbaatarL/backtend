package com.sk.domain.shared.enums.newsletter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    DAILY("DAILY", "일간"),
    WEEKLY("WEEKLY", "주간"),
    MONTHLY("MONTHLY", "월간"),
    QUARTERLY("QUARTERLY", "분기"),
    ON_DEMAND("ON-DEMAND", "수시");

    private final String code;
    private final String description;
}