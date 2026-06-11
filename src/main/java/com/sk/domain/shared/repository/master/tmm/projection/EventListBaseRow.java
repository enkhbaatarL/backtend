package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

public record EventListBaseRow(
    Long miEventId,
    String eventTitle,
    String level1Code,
    String level1Label,
    Boolean isSignal,
    Boolean isNew,
    Boolean isScenarioSuggested,
    String scenarioSuggestReason,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    LocalDateTime latestRecordFirstCreated
) {

    @QueryProjection
    public EventListBaseRow {
    }

}
