package com.sk.domain.mi.conversion.strategy;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.FormulaService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 단위 변환 전략의 기본 인터페이스
 */
public interface ConversionStrategy {

    /**
     * 해당 전략이 지원하는 공식 서비스를 반환
     */
    FormulaService getFormulaService();

    /**
     * 단일 값의 변환을 처리
     */
    BigDecimal convert(BigDecimal value);

    /**
     * 해당 전략이 특정 공식 서비스를 지원하는지 확인
     */
    boolean supports(FormulaService formulaService);

    /**
     * 시계열 데이터의 변환을 처리
     */
    default List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "This strategy doesn't support time series data");
    }

}
