package com.sk.domain.shared.mapper.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CME 벤치마크 가격의 한 점
 */
@Getter
@Setter
@NoArgsConstructor
public class CmeBenchmarkPriceRow {

    /** 차트 X축 월 (value_month). 포맷 YYYYMM */
    private String valueMonth;

    /** 차트 Y축 값 (index_value). */
    private BigDecimal indexValue;
}
