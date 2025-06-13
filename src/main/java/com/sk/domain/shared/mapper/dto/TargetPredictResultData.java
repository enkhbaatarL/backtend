package com.sk.domain.shared.mapper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// tmm_bztgtprdcrsltdatam Table
@Getter
@Setter
@NoArgsConstructor
public class TargetPredictResultData {
    /**
     * 예측수행일자
     */
    private String predictFulfillDate;
    /**
     * 예측결과일자
     */
    private String predictResultDate;
    /**
     * 예측결과값
     */
    private BigDecimal predictResultValue;
}
 