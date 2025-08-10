package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record ChartsPersonalizeRes(

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

    public static ChartsPersonalizeRes of(ChartsPersonalizeReq request) {
        return ChartsPersonalizeRes.builder()
            .chartConditionCd(request.chartConditionCd())
            .chartConditionVal(request.chartConditionVal())
            .build();
    }

    public static List<ChartsPersonalizeRes> ofList(List<ChartsPersonalizeReq> requests) {
        return requests.stream().map(ChartsPersonalizeRes::of).toList();
    }

}
