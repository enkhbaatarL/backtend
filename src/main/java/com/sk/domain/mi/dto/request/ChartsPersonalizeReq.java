package com.sk.domain.mi.dto.request;

import com.sk.domain.shared.enums.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChartsPersonalizeReq(

    @Schema(
        description = "차트 조건 타입(RANGE, INTERVAL, INDICATOR, CHART, UNIT, COMP_DATA_SOURCE)",
        example = "RANGE"
    )
    ChartSettingConditionCd chartSettingConditionCd,

    @Schema(
        description = "차트 조건 값(RANGE, INTERVAL, INDICATOR, CHART, UNIT, COMP_DATA_SOURCE)",
        example = "1WEEK"
    )
    String chartConditionValue

) {
}
