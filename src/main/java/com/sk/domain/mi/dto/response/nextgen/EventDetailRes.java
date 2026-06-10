package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
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

    @Schema(description = "상태 (new / updated)", example = "new")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

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

    @Schema(description = "시나리오화 추천 근거 (추천 아님일 때 null)", example = "글로벌 LNG 공급 차질 가능성이 시나리오화 가치 있음")
    String scenarioSuggestReason,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "Driver Tree Scoreboard — 인덱스(JKM/TTF/HH)별 그룹")
    List<EventDriverTreeIndexRes> driverTreeScoreboard,

    @Schema(description = "관련 이벤트 및 시나리오 (관련 테이블 미준비 상태로 현재 항상 null)", nullable = true)
    RelatedEventScenarioRes relatedEventScenario

) {

    public static EventDetailRes from(EventDetailRow row) {
        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        EventImpactAnalysisRes impactAnalysis = EventImpactAnalysisRes.from(row);

        return new EventDetailRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            row.createDt(),
            row.modifyDt(),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            EventAiInsightsRes.from(row),
            impactAnalysis,
            buildDriverTreeScoreboard(row.driverTags()),
            null
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
