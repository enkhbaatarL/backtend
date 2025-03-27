package com.sk.domain.mi.conversion_on_demand.strategy;

import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.math.BigDecimal;

public interface ConversionStrategy {

    FormulaService getFormulaService();

    BigDecimal convert(BigDecimal value);

    default BigDecimal convert(BigDecimal... values) {
        if (values.length == 1) {
            return convert(values[0]);
        }
        throw new UnsupportedOperationException("This strategy doesn't support multiple parameters");
    }

    boolean supports(FormulaService formulaService);

}
