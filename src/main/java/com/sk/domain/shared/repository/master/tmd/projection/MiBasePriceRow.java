package com.sk.domain.shared.repository.master.tmd.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;

/**
 * MI Base 기준선의 한 점 (시나리오에 종속되지 않는 exec_type_cd=MI_BASE 케이스의 가격 결과).
 * miBaseTitle은 가격과 같은 행에서 읽은 MI Base 케이스 타이틀(범례 표기용).
 */
public record MiBasePriceRow(
    String periodKey,
    BigDecimal baseCasePrice,
    BigDecimal userAdjustedPrice,
    String miBaseTitle
) {

    @QueryProjection
    public MiBasePriceRow {
    }

}
