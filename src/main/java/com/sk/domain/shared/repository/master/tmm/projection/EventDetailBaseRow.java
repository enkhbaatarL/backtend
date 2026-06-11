package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

public record EventDetailBaseRow(
    Long miEventId,
    String eventTitle,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    LocalDateTime latestRecordFirstCreated,
    String level1Code,
    String level1Label,
    String level2Code,
    String level2Label,
    Boolean isSignal,
    Boolean isNew,
    Boolean isScenarioSuggested,
    String scenarioSuggestReason,
    String detailAnalysis,
    String keyBulletsJson
) {

    @QueryProjection
    public EventDetailBaseRow {
    }

}
