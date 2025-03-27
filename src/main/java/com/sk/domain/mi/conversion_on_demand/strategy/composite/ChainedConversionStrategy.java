package com.sk.domain.mi.conversion_on_demand.strategy.composite;

import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.mi.conversion_on_demand.strategy.AbstractComposableConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.strategy.ComposableConversionStrategy;

import java.math.BigDecimal;
import java.util.List;

public class ChainedConversionStrategy extends AbstractComposableConversionStrategy {

    private final ComposableConversionStrategy first;
    private final ComposableConversionStrategy second;

    public ChainedConversionStrategy(ComposableConversionStrategy first, ComposableConversionStrategy second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public FormulaService getFormulaService() {
        return null; // 조합 전략
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
        return first.supports(formulaService) || second.supports(formulaService);
    }

}
