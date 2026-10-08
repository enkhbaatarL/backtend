package com.sk.domain.mi.dto.request.nextgen.snapshot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/** [MCP Snapshot] 이벤트 Snapshot 근거 Record 목록 요청 (날짜는 KST, page=1·size=10 기본). */
public record EventSnapshotRecordListReq(

    @Schema(description = "Record 유형 (record_type 일치)", allowableValues = {"NEWS", "REPORT", "EMAIL"}, example = "NEWS")
    String recordType,

    @Schema(description = "당시 Record 출처명 (ref_nm 부분 일치)", example = "Reuters")
    String refNm,

    @Schema(description = "Record 발생일 시작 (KST, 포함). record_first_created 기준", example = "2026-07-01")
    LocalDate startDate,

    @Schema(description = "Record 발생일 종료 (KST, 포함). record_first_created 기준", example = "2026-07-31")
    LocalDate endDate,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "10", defaultValue = "10")
    Integer size

) {

    public EventSnapshotRecordListReq {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }

}
