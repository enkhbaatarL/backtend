package com.sk.domain.mi.dto.request.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.EventPeriod;
import com.sk.domain.shared.enums.scenarioworkbench.EventSort;
import com.sk.domain.shared.enums.scenarioworkbench.EventStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record EventListReq(

    @ArraySchema(schema = @Schema(description = "이벤트 상태", implementation = EventStatus.class))
    List<EventStatus> eventStatus,

    @ArraySchema(schema = @Schema(description = "이벤트 유형 Lv1", implementation = EventCategoryLv1.class))
    List<EventCategoryLv1> eventCategoryLv1,

    @Schema(description = "조회 기간", implementation = EventPeriod.class, defaultValue = "SIX_MONTHS")
    EventPeriod period,

    @Schema(description = "커스텀 조회 시작일 (period=CUSTOM 시 사용)", example = "2025-09-01")
    LocalDate startDate,

    @Schema(description = "커스텀 조회 종료일 (period=CUSTOM 시 사용)", example = "2026-02-28")
    LocalDate endDate,

    @Schema(description = "정렬 기준", implementation = EventSort.class, defaultValue = "LATEST")
    EventSort sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "20", defaultValue = "20")
    Integer size
) {

    // 기본값 보정
    public EventListReq {
        eventStatus = defaultList(eventStatus);
        eventCategoryLv1 = defaultList(eventCategoryLv1);
        period = period == null ? EventPeriod.SIX_MONTHS : period;
        sort = sort == null ? EventSort.LATEST : sort;
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

    private static <T> List<T> defaultList(List<T> value) {
        return value == null ? Collections.emptyList() : value;
    }

}
