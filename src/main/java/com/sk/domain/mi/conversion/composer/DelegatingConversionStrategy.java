package com.sk.domain.mi.conversion.composer;

import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.math.BigDecimal;
import java.util.List;

public class DelegatingConversionStrategy implements ConversionStrategy {

    private final List<ConversionStrategy> delegateStrategies;
    private final FormulaService targetService;

    public DelegatingConversionStrategy(List<ConversionStrategy> strategies, FormulaService targetService) {
        this.delegateStrategies = strategies;
        this.targetService = targetService;
    }

    @Override
    public FormulaService getFormulaService() {
        return targetService;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        // 기본 구현은 구체적인 요구사항에 따라 상속 클래스에서 재정의 필요
        throw new UnsupportedOperationException("This delegate strategy doesn't implement basic conversion");
    }

    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        // 기본 구현은 구체적인 요구사항에 따라 상속 클래스에서 재정의 필요
        throw new UnsupportedOperationException("This delegate strategy doesn't implement time series conversion");
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == targetService;
    }

}
