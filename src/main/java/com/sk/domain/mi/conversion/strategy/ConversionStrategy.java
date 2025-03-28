package com.sk.domain.mi.conversion.strategy;

import com.sk.domain.mi.conversion_on_demand.FormulaService;

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
     * 다중 값의 변환을 처리 (기본 구현은 단일 값 메서드 호출)
     */
    default BigDecimal convert(BigDecimal... values) {
        if (values.length == 1) {
            return convert(values[0]);
        }
        throw new UnsupportedOperationException("This strategy doesn't support multiple parameters");
    }

    /**
     * 시계열 데이터의 변환을 처리
     */
    default List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        throw new UnsupportedOperationException("This strategy doesn't support time series data");
    }

    /**
     * 특정 결과 타입을 반환하는 시계열 데이터의 변환을 처리
     */
    default <T> List<T> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<T> resultType) {
        throw new UnsupportedOperationException("This strategy doesn't support complex time series data");
    }

    /**
     * 해당 전략이 특정 공식 서비스를 지원하는지 확인
     */
    boolean supports(FormulaService formulaService);

}
