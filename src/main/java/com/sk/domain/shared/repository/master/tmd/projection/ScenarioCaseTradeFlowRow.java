package com.sk.domain.shared.repository.master.tmd.projection;

public record ScenarioCaseTradeFlowRow(
    String fromHeaderKey,
    String toHeaderKey,
    String fromRegionCode,
    String fromNationName,
    String toRegionCode,
    String toNationName,
    Double flowValue
) {
}
