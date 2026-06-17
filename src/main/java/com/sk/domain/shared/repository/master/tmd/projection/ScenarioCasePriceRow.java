package com.sk.domain.shared.repository.master.tmd.projection;

public record ScenarioCasePriceRow(
    String periodUnitSeparateCode,
    String periodKey,
    String priceRegionName,
    Double baseCasePrice,
    Double userAdjustedPrice
) {
}
