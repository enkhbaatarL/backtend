package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;
import java.util.List;

public record EventListRow(
    Long miEventId,
    String eventTitleKo,
    String level1Code,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    List<EventIndexImpactRow> indexImpacts
) {
}
