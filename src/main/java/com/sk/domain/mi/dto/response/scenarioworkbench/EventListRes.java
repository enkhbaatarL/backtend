package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.common.model.PaginationRes;
import io.swagger.v3.oas.annotations.media.Schema;

public record EventListRes(

    @Schema(description = "이벤트 목록 요약 알림")
    EventSummaryNoticeRes summaryNotice,

    @Schema(description = "적용된 필터")
    EventAppliedFilterRes appliedFilter,

    @Schema(description = "이벤트 목록 페이지")
    PaginationRes<EventListItemRes> events

) {
}
