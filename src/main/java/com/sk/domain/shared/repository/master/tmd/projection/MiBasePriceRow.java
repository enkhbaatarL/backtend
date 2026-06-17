package com.sk.domain.shared.repository.master.tmd.projection;

import java.math.BigDecimal;

/**
 * MI Base 기준선의 한 점 (시나리오에 종속되지 않는 exec_type_cd=MI_BASE 케이스의 가격 결과).
 */
public record MiBasePriceRow(
    String periodKey,
    BigDecimal baseCasePrice,
    BigDecimal userAdjustedPrice
) {
}
