package com.sk.domain.mi.dto.response.nextgen;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.enums.nextgen.EventSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record EventListItemRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명", example = "호르무즈 해협 긴장 재확대")
    String eventName,

    @Schema(description = "상태 (new|updated)", example = "new")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @Schema(description = "출처 (platform|user)", example = "user")
    EventSourceType sourceType,

    @Schema(description = "출처 라벨 — user면 사용자 등록 배지 표기", example = "사용자 등록")
    String sourceTypeLabel,

    @Schema(description = "이벤트 유형 Lv1", example = "supply")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "공급")
    String eventTypeLv1Label,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "최신 연관 레코드 최초 생성일시 (KST, tmm_mi_mievent_m.latest_record_first_created, 응답 시 KST 변환", example = "2026-05-12 15:08:34")
    LocalDateTime latestRecordFirstCreated,

    @Schema(description = "신호/소음 구분", example = "signal")
    EventSignalType signalType,

    @Schema(description = "신호/소음 라벨", example = "신호")
    String signalTypeLabel,

    @Schema(description = "시나리오화 추천 여부", example = "true")
    boolean isScenarioSuggested,

    @Schema(description = "시나리오화 추천 근거", example = "글로벌 공급 차질 가능성으로 시나리오화 검토 필요")
    String scenarioSuggestReason,

    @Schema(description = "연결된 활성 시나리오 그룹 수 (생성중 포함, 실패 방치 그룹 제외). 0이면 설계 아이콘, 1 이상이면 그룹 N 버튼 표기", example = "2")
    long scenarioGroupCount,

    @Schema(description = "인덱스별 영향도 (JKM|TTF|HH)")
    List<EventImpactIndexRes> indexImpacts

) {

    public static EventListItemRes from(EventListRow row) {
        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        EventSourceType sourceType = EventSourceType.of(row.createLoginUserId(), row.modifyLoginUserId());

        return new EventListItemRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            sourceType,
            sourceType.getLabel(),
            row.level1Code(),
            row.level1Label(),
            row.createDt(),
            row.modifyDt(),
            DateUtil.utcToKst(row.latestRecordFirstCreated()),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            row.scenarioGroupCount() == null ? 0L : row.scenarioGroupCount(),
            row.indexImpacts().stream().map(EventImpactIndexRes::fromRow).toList()
        );
    }

}
