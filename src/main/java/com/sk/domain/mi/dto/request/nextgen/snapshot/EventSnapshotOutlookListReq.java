package com.sk.domain.mi.dto.request.nextgen.snapshot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/** [MCP Snapshot] 이벤트 Snapshot 기관 전망 사본 목록 요청 (page=1·size=10 기본). */
public record EventSnapshotOutlookListReq(

    @Schema(description = "기관명 (institution_name 부분 일치)", example = "IEA")
    String institutionName,

    @Schema(description = "전망 발행일 시작 (포함). view_date 기준", example = "2026-07-01")
    LocalDate startDate,

    @Schema(description = "전망 발행일 종료 (포함). view_date 기준", example = "2026-09-30")
    LocalDate endDate,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "10", defaultValue = "10")
    Integer size

) {

    public EventSnapshotOutlookListReq {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }

}
