package com.sk.domain.shared.repository.master.tmm.projection;

import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import java.time.LocalDateTime;
import java.util.List;

public record EventListRow(
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
    LocalDateTime latestRecordFirstCreated,
    Long scenarioGroupId,
    ScenarioGroupStatus scenarioGroupStatus,
    Long requestUserId,
    boolean templateAvailable,
    List<EventIndexImpactRow> indexImpacts
) {
}
