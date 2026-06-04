package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record EventAppliedFilterRes(

    @Schema(description = "이벤트 상태")
    ContentStatus eventStatus,

    @Schema(description = "이벤트 유형 Lv1 ID")
    List<Long> eventTypeL1Id,

    @Schema(description = "조회 시작일")
    LocalDate startDate,

    @Schema(description = "조회 종료일")
    LocalDate endDate,

    @Schema(description = "정렬 기준")
    SortOrder sort,

    @Schema(description = "검색 키워드", example = "호르무즈")
    String keyword,

    @Schema(description = "추천 항목만 보기 적용 여부", example = "false")
    Boolean suggestedOnly

) {

    public static EventAppliedFilterRes from(EventListReq request) {
        return new EventAppliedFilterRes(
            request.eventStatus(),
            request.eventTypeL1Id(),
            request.startDate(),
            request.endDate(),
            request.sort(),
            request.keyword(),
            request.suggestedOnly()
        );
    }

}
