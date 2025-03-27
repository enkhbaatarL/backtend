package com.sk.domain.mi.conversion_on_demand.strategy;

import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.math.BigDecimal;
import java.util.List;

public interface ConversionStrategy {

    FormulaService getFormulaService();

    BigDecimal convert(BigDecimal value);

    default BigDecimal convert(BigDecimal... values) {
        if (values.length == 1) {
            return convert(values[0]);
        }
        throw new UnsupportedOperationException("This strategy doesn't support multiple parameters");
    }

    default List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        throw new UnsupportedOperationException("This strategy doesn't support complex time series data");
    }

    default <T> List<T> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<T> resultType) {
        throw new UnsupportedOperationException("This strategy doesn't support complex time series data");
    }

    boolean supports(FormulaService formulaService);

}
