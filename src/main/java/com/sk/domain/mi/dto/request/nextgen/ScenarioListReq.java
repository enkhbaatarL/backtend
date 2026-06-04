package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record ScenarioListReq(

    @Schema(description = "시나리오 상태", implementation = ContentStatus.class, defaultValue = "ALL")
    ContentStatus scenarioStatus,

    @Schema(description = "조회 시작일", example = "2025-09-01")
    LocalDate startDate,

    @Schema(description = "조회 종료일", example = "2026-02-28")
    LocalDate endDate,

    @Schema(description = "정렬 기준", implementation = SortOrder.class, defaultValue = "LATEST")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "20", defaultValue = "20")
    Integer size

) {

    public ScenarioListReq {
        scenarioStatus = scenarioStatus == null ? ContentStatus.ALL : scenarioStatus;
        sort = sort == null ? SortOrder.LATEST : sort;
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

}
