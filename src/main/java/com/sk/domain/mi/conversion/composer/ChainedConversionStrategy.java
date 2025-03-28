package com.sk.domain.mi.conversion.composer;

import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 여러 전략을 체인으로 연결하는 복합 전략 클래스
 */
public class ChainedConversionStrategy implements ConversionStrategy {

    private final ConversionStrategy first;
    private final ConversionStrategy second;
    private final FormulaService targetService;

    public ChainedConversionStrategy(ConversionStrategy first, ConversionStrategy second, FormulaService targetService) {
        this.first = first;
        this.second = second;
        this.targetService = targetService;
    }

    @Override
    public FormulaService getFormulaService() {
        return targetService; // 조합 전략의 경우 별도의 타겟 서비스 지정
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        BigDecimal intermediate = first.convert(value);
        return second.convert(intermediate);
    }

    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        List<BigDecimal> intermediate = first.convertTimeSeries(timeSeriesData, windowSize);
        return second.convertTimeSeries(intermediate, windowSize);
    }

    @Override
    public <T> List<T> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<T> resultType) {
        List<BigDecimal> intermediate = first.convertTimeSeries(timeSeriesData, windowSize);
        return second.convertTimeSeries(intermediate, windowSize, resultType);
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == targetService;
    }

}
