package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ScenarioGroupAppliedFilterRes(

    @Schema(description = "조회 시작일")
    LocalDate startDate,

    @Schema(description = "조회 종료일")
    LocalDate endDate,

    @Schema(description = "정렬 기준")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "카타르")
    String keyword

) {

    public static ScenarioGroupAppliedFilterRes from(ScenarioGroupListReq request) {
        return new ScenarioGroupAppliedFilterRes(
            request.startDate(),
            request.endDate(),
            request.sort(),
            request.keyword()
        );
    }

}
