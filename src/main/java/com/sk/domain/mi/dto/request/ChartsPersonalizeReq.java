package com.sk.domain.mi.dto.request;

import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChartsPersonalizeReq(

    @Schema(
        description = "개인화 차트 조건 타입(RANGE, INTERVAL, INDICATOR, CHART, UNIT_CD, COMP_DATA_SOURCE)",
        example = "RANGE"
    )
    ChartSettingConditionCd chartConditionCd,

    @Schema(
        description = "개인화 차트 조건 값",
        example = "1W"
    )
    String chartConditionVal

) {

    public ChartsPersonalizeReq validThen() {
        if (this.chartConditionCd == ChartSettingConditionCd.UNIT || this.chartConditionCd == ChartSettingConditionCd.COMP_DATA_SOURCE) {
            return this;
        } else if (this.chartConditionCd.getAllowedValues().contains(this.chartConditionVal)) {
            return this;
        } else {
            return null;
        }
    }

}
