package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.common.model.PaginationRes;
import io.swagger.v3.oas.annotations.media.Schema;

public record OverviewEventListRes(

    @Schema(description = "이벤트 목록 (하위 시나리오 포함)")
    PaginationRes<OverviewEventItemRes> events

) {}
