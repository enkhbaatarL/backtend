package com.sk.domain.shared.repository.master.tmd.projection;

public record ScenarioCaseTradeFlowHeaderRow(
    String axisCode,
    String headerKey,
    String headerLabelName,
    String headerKindCode,
    String regionCode,
    String nationName,
    Integer headerOrder
) {
}
