package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSourceType;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record EventListReq(

    @Schema(description = "이벤트 상태 (`all` / `new` / `updated`)", implementation = ContentStatus.class, defaultValue = "all")
    ContentStatus eventStatus,

    @ArraySchema(schema = @Schema(description = "이벤트 유형 Lv1 ID", example = "1"))
    List<Long> eventTypeL1Id,

    @Schema(description = "조회 시작일", example = "2025-09-01")
    LocalDate startDate,

    @Schema(description = "조회 종료일", example = "2026-02-28")
    LocalDate endDate,

    @Schema(description = "정렬 기준 (`latest` / `oldest`)", implementation = SortOrder.class, defaultValue = "latest")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword,

    @Schema(description = "추천 항목만 보기 (시나리오화 추천 = scenario_suggest_yn=Y)", example = "false", defaultValue = "false")
    Boolean suggestedOnly,

    @Schema(description = "출처 (`platform` / `user`). 생략 시 전체", implementation = EventSourceType.class, nullable = true)
    EventSourceType sourceType,

    @Schema(description = "내가 만든 이벤트만 보기 (true면 출처는 `user`로 고정)", example = "false", defaultValue = "false")
    Boolean mine,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "20", defaultValue = "20")
    Integer size

) {

    public EventListReq {
        eventStatus = eventStatus == null ? ContentStatus.ALL : eventStatus;
        eventTypeL1Id = defaultList(eventTypeL1Id);
        sort = sort == null ? SortOrder.LATEST : sort;
        keyword = keyword == null ? "" : keyword;
        suggestedOnly = suggestedOnly != null && suggestedOnly;
        mine = mine != null && mine;
        sourceType = mine ? EventSourceType.USER : sourceType;
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

    private static <T> List<T> defaultList(List<T> value) {
        return value == null ? Collections.emptyList() : value;
    }

}
