package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;

public record DriverTagScoreboardRow(
    String lv1Id,
    String lv1Label,
    String lv2Id,
    String lv2Label,
    String lv3Id,
    String lv3Label,
    String magnitudeLevel,
    String direction,
    String driverReason
) {

    @QueryProjection
    public DriverTagScoreboardRow {
    }

}
