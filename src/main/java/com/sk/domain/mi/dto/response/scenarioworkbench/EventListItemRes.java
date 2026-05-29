package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.EventSignalType;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record EventListItemRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명", example = "호르무즈 해협 긴장 재확대")
    String eventName,

    @Schema(description = "상태 (new / updated)", example = "new")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String eventTypeLv1Label,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "신호/소음 구분", example = "signal")
    EventSignalType signalType,

    @Schema(description = "신호/소음 라벨", example = "신호")
    String signalTypeLabel,

    @Schema(description = "시나리오화 추천 여부", example = "true")
    Boolean isScenarioSuggested,

    @Schema(description = "시나리오화 추천 근거", example = "글로벌 공급 차질 가능성으로 시나리오화 검토 필요")
    String scenarioSuggestReason,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<EventImpactIndexRes> indexImpacts

) {

    public static EventListItemRes from(EventListRow row) {
        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;

        return new EventListItemRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            row.level1Code(),
            row.level1Label(),
            row.createDt(),
            row.modifyDt(),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            row.indexImpacts().stream().map(EventImpactIndexRes::fromRow).toList()
        );
    }

}
