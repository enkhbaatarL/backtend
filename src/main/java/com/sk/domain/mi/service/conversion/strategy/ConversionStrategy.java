package com.sk.domain.mi.service.conversion.strategy;

import com.sk.domain.shared.enums.UnitType;

import java.math.BigDecimal;

public interface ConversionStrategy {

    String getFormulaServiceNm();

    BigDecimal convert(BigDecimal value);

    boolean supports(UnitType fromUnit, UnitType toUnit);

}
