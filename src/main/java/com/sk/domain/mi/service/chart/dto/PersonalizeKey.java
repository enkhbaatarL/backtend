package com.sk.domain.mi.service.chart.dto;

import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;

public record PersonalizeKey(
    ChartSettingConditionCd cd,
    String val
) {
}
