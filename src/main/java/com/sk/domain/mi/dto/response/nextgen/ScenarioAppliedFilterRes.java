package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ScenarioListReq;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ScenarioAppliedFilterRes(

    @Schema(description = "시나리오 상태")
    ContentStatus scenarioStatus,

    @Schema(description = "조회 시작일")
    LocalDate startDate,

    @Schema(description = "조회 종료일")
    LocalDate endDate,

    @Schema(description = "정렬 기준")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword

) {

    public static ScenarioAppliedFilterRes from(ScenarioListReq request) {
        return new ScenarioAppliedFilterRes(
            request.scenarioStatus(),
            request.startDate(),
            request.endDate(),
            request.sort(),
            request.keyword()
        );
    }

}
