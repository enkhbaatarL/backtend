package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.ScenarioGenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record DesignEventItemRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명", example = "호르무즈 해협 긴장 재확대")
    String eventName,

    @Schema(description = "상태 (NEW / UPDATED)", example = "NEW")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    EventCategoryLv1 eventCategoryLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String eventCategoryLv1Label,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<EventImpactIndexRes> indexImpacts,

    @Schema(description = "시나리오 생성 상태 (READY / PROCESSING / COMPLETED)", example = "READY")
    ScenarioGenStatus scenarioGenStatus,

    @Schema(description = "시나리오 생성 상태 라벨", example = "생성준비")
    String scenarioGenStatusLabel,

    @Schema(description = "시나리오 생성 요청 ID (PROCESSING/COMPLETED 상태일 때만 존재)", example = "1", nullable = true)
    Long requestId

) {

}
