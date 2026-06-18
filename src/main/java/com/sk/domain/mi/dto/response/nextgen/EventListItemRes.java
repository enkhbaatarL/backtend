package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
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

    @Schema(description = "이벤트 유형 Lv1", example = "supply")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "공급")
    String eventTypeLv1Label,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "최신 연관 레코드 최초 생성일시 (최신순 정렬 기준)")
    LocalDateTime latestRecordFirstCreated,

    @Schema(description = "신호/소음 구분", example = "signal")
    EventSignalType signalType,

    @Schema(description = "신호/소음 라벨", example = "신호")
    String signalTypeLabel,

    @Schema(description = "시나리오화 추천 여부", example = "true")
    Boolean isScenarioSuggested,

    @Schema(description = "시나리오화 추천 근거", example = "글로벌 공급 차질 가능성으로 시나리오화 검토 필요")
    String scenarioSuggestReason,

    @Schema(description = "시나리오 디자인 상태 — designable(설계가능) / creating(설계중) / created(설계완료)", example = "designable")
    ScenarioDesignDisplayStatus scenarioDesignStatus,

    @Schema(description = "시나리오 디자인 상태 라벨", example = "설계가능")
    String scenarioDesignStatusLabel,

    @Schema(description = "활성 시나리오 그룹 ID (없으면 null, created일 때 이동 대상)", example = "1", nullable = true)
    Long scenarioGroupId,

    @Schema(description = "설계 점유 여부 (설계중이면 true)", example = "false")
    boolean occupied,

    @Schema(description = "점유 사용자 ID (점유 중이 아니면 null)", example = "10001", nullable = true)
    Long occupantUserId,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<EventImpactIndexRes> indexImpacts

) {

    public static EventListItemRes from(EventListRow row) {
        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        ScenarioDesignDisplayStatus designStatus = ScenarioDesignDisplayStatus.of(row.scenarioGroupStatus());
        // 점유 = 활성 그룹이 creating(설계중)인 상태. 점유자는 그룹 요청자(request_usr_id).
        boolean occupied = row.scenarioGroupStatus() == ScenarioGroupStatus.CREATING;

        return new EventListItemRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            row.level1Code(),
            row.level1Label(),
            row.createDt(),
            row.modifyDt(),
            row.latestRecordFirstCreated(),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            designStatus,
            designStatus.getLabel(),
            row.scenarioGroupId(),
            occupied,
            occupied ? row.requestUserId() : null,
            row.indexImpacts().stream().map(EventImpactIndexRes::fromRow).toList()
        );
    }

}
