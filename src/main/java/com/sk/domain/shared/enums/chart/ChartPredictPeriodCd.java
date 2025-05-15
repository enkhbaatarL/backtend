package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * 예측기간단위구분코드
 */
@Getter
@RequiredArgsConstructor
public enum ChartPredictPeriodCd {
    DAY("D", "일"),
    WEEK("W", "주"),
    MONTH("M", "월"),
    YEAR("Y", "년");

    private final String code;
    private final String description;
}
