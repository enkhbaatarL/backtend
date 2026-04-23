package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventFundamentalRes(

    @Schema(description = "항목 섹션")
    List<FundamentalMetricRes> metrics,

    @Schema(description = "지표 섹션")
    List<FundamentalIndicatorRes> indicators

) {
}
