package com.sk.domain.mi.conversion.strategy;

import com.sk.domain.mi.conversion.dto.ConversionResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 단위 변환 전략의 확장 인터페이스
 */
public interface ResultWrapperConversionStrategy<R extends ConversionResult> extends ConversionStrategy {

    /**
     * 특정 결과 타입을 반환하는 시계열 데이터의 변환을 처리
     */
    List<R> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<R> resultType);

    /**
     * 반환 값 설정
     */
    Class<R> resultType();

}
