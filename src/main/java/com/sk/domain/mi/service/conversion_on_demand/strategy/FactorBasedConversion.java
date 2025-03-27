package com.sk.domain.mi.service.conversion_on_demand.strategy;

import com.sk.domain.shared.enums.UnitType;

import java.math.BigDecimal;

public class FactorBasedConversion implements ConversionStrategy {

    private final UnitType fromUnit;
    private final UnitType toUnit;
    private final BigDecimal factor;
    private final String name;

    public FactorBasedConversion(UnitType fromUnit, UnitType toUnit, BigDecimal factor) {
        this.fromUnit = fromUnit;
        this.toUnit = toUnit;
        this.factor = factor;
        this.name = fromUnit.getCode() + "To" + toUnit.getCode() + "Factor";
    }

    @Override
    public String getFormulaServiceNm() {
        return name;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        return value.multiply(factor);
    }

    @Override
    public boolean supports(UnitType fromUnit, UnitType toUnit) {
        return this.fromUnit == fromUnit && this.toUnit == toUnit;
    }

}
