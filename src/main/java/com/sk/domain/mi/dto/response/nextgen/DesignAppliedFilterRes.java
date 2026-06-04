package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.request.nextgen.DesignEventListReq;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record DesignAppliedFilterRes(

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

    @Schema(description = "검색 키워드")
    String keyword,

    @Schema(description = "완료 항목 숨기기")
    Boolean hideCompleted,

    @Schema(description = "시나리오 생성 비추천 항목 숨기기")
    Boolean hideNonRecommended

) {

    public static DesignAppliedFilterRes from(DesignEventListReq request) {
        return new DesignAppliedFilterRes(
            request.eventStatus(),
            request.eventTypeL1Id(),
            request.startDate(),
            request.endDate(),
            request.sort(),
            request.keyword(),
            request.hideCompleted(),
            request.hideNonRecommended()
        );
    }

}
