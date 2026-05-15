package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;
import java.util.List;

public record EventListRow(
    Long miEventId,
    String eventTitle,
    String level1Code,
    String level1Label,
    Boolean signalYn,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    List<EventIndexImpactRow> indexImpacts
) {
}
