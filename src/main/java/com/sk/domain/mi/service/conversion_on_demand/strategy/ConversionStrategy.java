package com.sk.domain.mi.service.conversion_on_demand.strategy;

import com.sk.domain.shared.enums.UnitType;

import java.math.BigDecimal;

public interface ConversionStrategy {

    String getFormulaServiceNm();

    BigDecimal convert(BigDecimal value);

    default BigDecimal convert(BigDecimal... values) {
        if (values.length == 1) {
            return convert(values[0]);
        }
        throw new UnsupportedOperationException("This strategy doesn't support multiple parameters");
    }

    boolean supports(UnitType fromUnit, UnitType toUnit);

}
