package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.EventSignalType;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record EventDetailRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명")
    String eventName,

    @Schema(description = "상태 (NEW / UPDATED)", example = "NEW")
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

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "Fundamental Deep-dive (테이블 미준비 상태이므로 현재는 null)")
    EventFundamentalRes fundamentalDeepdive,

    @Schema(description = "Driver Tree Scoreboard")
    List<DriverTreeScoreboardRes> driverTreeScoreboard,

    @Schema(description = "관련 이벤트 및 시나리오")
    RelatedEventScenarioRes relatedEventScenario

) {

    public static EventDetailRes from(EventDetailRow row) {
        ContentStatus status = row.createDt().equals(row.modifyDt()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.signalYn()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        EventImpactAnalysisRes impactAnalysis = EventImpactAnalysisRes.from(row);

        RelatedEventRes selfEvent = new RelatedEventRes(
            row.miEventId(),
            row.eventTitle(),
            row.createDt(),
            row.modifyDt(),
            impactAnalysis.eventTypeLv1(),
            impactAnalysis.eventTypeLv1Label(),
            impactAnalysis.indexImpacts()
        );

        return new EventDetailRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            row.createDt(),
            row.modifyDt(),
            signalType,
            signalType.getLabel(),
            impactAnalysis,
            EventAiInsightsRes.from(row),
            null,
            row.driverTags().stream().map(DriverTreeScoreboardRes::from).toList(),
            new RelatedEventScenarioRes(selfEvent, List.of())
        );
    }

}
