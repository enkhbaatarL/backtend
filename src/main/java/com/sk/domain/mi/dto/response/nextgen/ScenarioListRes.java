package com.sk.domain.mi.dto.response.nextgen;

import com.sk.common.model.PaginationRes;
import io.swagger.v3.oas.annotations.media.Schema;

public record ScenarioListRes(

    @Schema(description = "적용된 필터")
    ScenarioAppliedFilterRes appliedFilter,

    @Schema(description = "시나리오 목록 페이지")
    PaginationRes<ScenarioListItemRes> scenarios

) {
}
