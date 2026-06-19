package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
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

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
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

    @Schema(description = "시나리오 디자인 상태 (designable|unavailable|creating|created)", example = "designable")
    ScenarioDesignDisplayStatus scenarioDesignStatus,

    @Schema(description = "시나리오 디자인 상태 라벨 (설계가능|설계불가|설계중|설계완료)", example = "설계가능")
    String scenarioDesignStatusLabel,

    @Schema(description = "설계 점유 여부 (설계중이면 true)", example = "false")
    boolean occupied,

    @Schema(description = "점유 사용자 ID (점유 중이 아니면 null)", example = "10001", nullable = true)
    Long occupantUserId,

    @Schema(description = "점유 사용자명 (점유 중이 아니면 null)", example = "홍길동", nullable = true)
    String occupantUserName,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "Driver Tree Scoreboard — 인덱스(JKM/TTF/HH)별 그룹")
    List<EventDriverTreeIndexRes> driverTreeScoreboard,

    @Schema(description = "연관 시나리오", nullable = true)
    EventRelatedScenariosRes relatedEventScenario

) {

    public static EventDetailRes from(EventDetailRow row, EventRelatedScenariosRes relatedEventScenario, String occupantUserName) {
        ContentStatus status = Boolean.TRUE.equals(row.isNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;
        EventSignalType signalType = Boolean.TRUE.equals(row.isSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        EventImpactAnalysisRes impactAnalysis = EventImpactAnalysisRes.from(row);

        ScenarioGroupBaseRow group = row.relatedScenarioGroup();
        ScenarioDesignDisplayStatus designStatus = ScenarioDesignDisplayStatus.of(group == null ? null : group.status(), row.templateAvailable());
        // 점유 = 활성 시나리오 그룹이 creating(설계중)인 상태. 점유자는 그룹 요청자(request_usr_id)
        boolean occupied = group != null && group.status() == ScenarioGroupStatus.CREATING;

        return new EventDetailRes(
            row.miEventId(),
            row.eventTitle(),
            status,
            status.getLabel(),
            row.createDt(),
            row.modifyDt(),
            row.latestRecordFirstCreated(),
            signalType,
            signalType.getLabel(),
            Boolean.TRUE.equals(row.isScenarioSuggested()),
            row.scenarioSuggestReason(),
            designStatus,
            designStatus.getLabel(),
            occupied,
            occupied ? group.requestUserId() : null,
            occupied ? occupantUserName : null,
            EventAiInsightsRes.from(row),
            impactAnalysis,
            buildDriverTreeScoreboard(row.driverTags()),
            relatedEventScenario
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
