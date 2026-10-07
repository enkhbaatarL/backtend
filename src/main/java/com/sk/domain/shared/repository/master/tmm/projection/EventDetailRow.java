package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;
import java.util.List;

// note: 이벤트 상세 화면 — 영향도 분석 + AI Insights 영역에 필요한 모든 데이터를 Repository에서 조립
public record EventDetailRow(
    Long miEventId,
    String eventTitle,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    LocalDateTime latestRecordFirstCreated,
    LocalDateTime oldestRecordFirstCreated,
    String createLoginUserId,
    String modifyLoginUserId,
    String level1Code,
    String level1Label,
    String level2Code,
    String level2Label,
    Boolean isSignal,
    Boolean isNew,
    Boolean isScenarioSuggested,
    String scenarioSuggestReason,
    String detailAnalysis,
    String institutionViewSynthesis,
    String impactSummary,
    List<EventIndexImpactRow> indexImpacts,
    List<String> keyBullets,
    List<String> sources,
    List<DriverTagScoreboardRow> driverTags,
    List<ScenarioGroupBaseRow> relatedScenarioGroups,
    List<ScenarioBaseRow> relatedScenarios
) {

    /**
     * 이벤트 상세 base(단일 조회) + 개별 조회한 부가 데이터들을 하나의 상세 Row로 조립한다.
     * 이벤트명은 one_line_summary 표기를 사용한다. 연관 그룹은 멀티 이벤트 전환으로 N건이다.
     */
    public static EventDetailRow of(EventDetailBaseRow base, String impactSummary, List<EventIndexImpactRow> indexImpacts,
                                    List<String> keyBullets, List<String> sources, List<DriverTagScoreboardRow> driverTags,
                                    List<ScenarioGroupBaseRow> relatedScenarioGroups, List<ScenarioBaseRow> relatedScenarios
    ) {
        return new EventDetailRow(
            base.miEventId(),
            base.oneLineSummary(),
            base.createDt(),
            base.modifyDt(),
            base.latestRecordFirstCreated(),
            base.oldestRecordFirstCreated(),
            base.createLoginUserId(),
            base.modifyLoginUserId(),
            base.level1Code(),
            base.level1Label(),
            base.level2Code(),
            base.level2Label(),
            base.isSignal(),
            base.isNew(),
            base.isScenarioSuggested(),
            base.scenarioSuggestReason(),
            base.detailAnalysis(),
            base.institutionViewSynthesis(),
            impactSummary,
            indexImpacts,
            keyBullets,
            sources,
            driverTags,
            relatedScenarioGroups,
            relatedScenarios
        );
    }

}
