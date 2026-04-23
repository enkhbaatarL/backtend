package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record EventListItemRes(

    @Schema(description = "이벤트 ID", example = "ev_8f3a91c2b7d0")
    String eventId,

    @Schema(description = "이벤트 제목", example = "호르무즈 해협 긴장 재확대")
    String title,

    @Schema(description = "이벤트 상태")
    EventStatus eventStatus,

    @Schema(description = "이벤트 유형 Lv1")
    EventCategoryLv1 eventCategoryLv1,

    @Schema(description = "이벤트 발생/인지 시각")
    LocalDateTime occurredAt,

    @Schema(description = "마지막 업데이트 시각")
    LocalDateTime updatedAt,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<ImpactIndexSummaryRes> indexImpacts

) {
}
