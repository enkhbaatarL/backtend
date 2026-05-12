package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record EventAppliedFilterRes(

    @Schema(description = "이벤트 상태")
    ContentStatus eventStatus,

    @Schema(description = "이벤트 유형 Lv1")
    List<EventCategoryLv1> eventCategoryLv1,

    @Schema(description = "조회 시작일")
    LocalDate startDate,

    @Schema(description = "조회 종료일")
    LocalDate endDate,

    @Schema(description = "정렬 기준")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword

) {

    public static EventAppliedFilterRes from(EventListReq request) {
        return new EventAppliedFilterRes(
            request.eventStatus(),
            request.eventCategoryLv1(),
            request.startDate(),
            request.endDate(),
            request.sort(),
            request.keyword()
        );
    }

}
