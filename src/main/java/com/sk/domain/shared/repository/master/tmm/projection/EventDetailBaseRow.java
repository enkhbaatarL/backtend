package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

public record EventDetailBaseRow(
    Long miEventId,
    String oneLineSummary,
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
    String keyBulletsJson,
    String institutionViewSynthesis
) {

    @QueryProjection
    public EventDetailBaseRow {
    }

}
