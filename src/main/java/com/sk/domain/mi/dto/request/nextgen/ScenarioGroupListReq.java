package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/**
 * Scenario Group List 화면(그룹/개별 모드)의 공통 요청 필터.
 * 새 디자인 기준: 날짜 범위 + 정렬 + 검색 + 페이징 (상태 필터 없음).
 */
public record ScenarioGroupListReq(

    @Schema(description = "조회 시작일", example = "2026-04-20")
    LocalDate startDate,

    @Schema(description = "조회 종료일", example = "2026-05-19")
    LocalDate endDate,

    @Schema(description = "정렬 기준", implementation = SortOrder.class, defaultValue = "LATEST")
    SortOrder sort,

    @Schema(description = "검색 키워드 (그룹 모드: 그룹명 / 개별 모드: 시나리오명)", example = "카타르")
    String keyword,

    @Schema(description = "내가 만든 그룹만 조회 (그룹 모드: 그룹 소유자 / 개별 모드: 부모 그룹 소유자 기준)", example = "true", defaultValue = "false")
    Boolean mineOnly,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "20", defaultValue = "20")
    Integer size

) {

    public ScenarioGroupListReq {
        sort = sort == null ? SortOrder.LATEST : sort;
        keyword = keyword == null ? "" : keyword;
        mineOnly = mineOnly != null && mineOnly;
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

}
