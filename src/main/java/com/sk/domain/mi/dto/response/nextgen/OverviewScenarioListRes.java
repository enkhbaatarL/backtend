package com.sk.domain.mi.dto.response.nextgen;

import com.sk.common.model.PaginationRes;
import io.swagger.v3.oas.annotations.media.Schema;

public record OverviewScenarioListRes(

    @Schema(description = "시나리오 목록 (관련 이벤트 포함)")
    PaginationRes<OverviewScenarioItemRes> scenarios

) {}
