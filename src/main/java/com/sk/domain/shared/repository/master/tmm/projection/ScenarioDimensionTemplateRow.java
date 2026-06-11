package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;

public record ScenarioDimensionTemplateRow(
    Long eventTypeL2Id,
    String templateJson
) {

    @QueryProjection
    public ScenarioDimensionTemplateRow {
    }

}
