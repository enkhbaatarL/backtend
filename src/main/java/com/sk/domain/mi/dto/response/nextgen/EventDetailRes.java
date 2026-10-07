package com.sk.domain.mi.dto.response.nextgen;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.enums.nextgen.EventSourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EventDetailRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명")
    String eventName,

    @Schema(description = "상태 (new|updated)", example = "new")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @Schema(description = "출처 (platform|user)", example = "user")
    EventSourceType sourceType,

    @Schema(description = "출처 라벨 — user면 사용자 등록 배지 표기", example = "사용자 등록")
    String sourceTypeLabel,

    @Schema(description = "등록자명 (사용자 등록 이벤트만, 그 외 또는 미존재 사용자면 null)", example = "홍길동", nullable = true)
    String registrantName,

    @Schema(description = "생성일시 (KST). tmm_mi_mievent_m.oldest_record_first_created, 응답 시 KST 변환", example = "2026-05-12 15:08:34")
    LocalDateTime createdAt,

    @Schema(description = "갱신일시 (KST). tmm_mi_mievent_m.latest_record_first_created, 응답 시 KST 변환", example = "2026-05-12 15:08:34")
    LocalDateTime updatedAt,

    @Schema(description = "Event 관련 Record 중 최신 first_created 시각")
    LocalDateTime latestRecordFirstCreated,

    @Schema(description = "신호/소음 구분", example = "signal")
    EventSignalType signalType,

    @Schema(description = "신호/소음 라벨", example = "신호")
    String signalTypeLabel,

    @Schema(description = "시나리오화 추천 여부", example = "true")
    Boolean isScenarioSuggested,

    @Schema(description = "시나리오화 추천 근거", example = "글로벌 LNG 공급 차질 가능성이 시나리오화 가치 있음")
    String scenarioSuggestReason,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "Driver Tree Scoreboard — 인덱스(JKM/TTF/HH)별 그룹")
    List<EventDriverTreeIndexRes> driverTreeScoreboard,

    @Schema(description = "기관 전망 View 종합", nullable = true)
    String institutionViewSynthesis,

    @Schema(description = "연관 시나리오 그룹별 표 목록 (그룹 오래된 순, 없으면 빈 목록)")
    List<EventRelatedScenariosRes> relatedEventScenarios

) {

    public static EventDetailRes from(EventDetailRow row, String registrantName, List<EventRelatedScenariosRes> relatedEventScenarios) {

        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        EventImpactAnalysisRes impactAnalysis = EventImpactAnalysisRes.from(row);
        EventSourceType sourceType = EventSourceType.of(row.createLoginUserId(), row.modifyLoginUserId());

        return new EventDetailRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            sourceType,
            sourceType.getLabel(),
            registrantName,
            DateUtil.utcToKst(row.oldestRecordFirstCreated()),
            DateUtil.utcToKst(row.latestRecordFirstCreated()),
            DateUtil.utcToKst(row.latestRecordFirstCreated()),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            EventAiInsightsRes.from(row),
            impactAnalysis,
            buildDriverTreeScoreboard(row.driverTags()),
            row.institutionViewSynthesis(),
            relatedEventScenarios
        );
    }

    private static List<EventDriverTreeIndexRes> buildDriverTreeScoreboard(List<DriverTagScoreboardRow> driverTags) {
        Map<String, List<DriverTreeScoreboardRes>> grouped = new LinkedHashMap<>();
        for (DriverTagScoreboardRow tag : driverTags) {
            grouped.computeIfAbsent(tag.targetId(), k -> new ArrayList<>()).add(DriverTreeScoreboardRes.from(tag));
        }
        return grouped.entrySet().stream()
            .map(e -> new EventDriverTreeIndexRes(e.getKey(), e.getValue()))
            .toList();
    }

}
