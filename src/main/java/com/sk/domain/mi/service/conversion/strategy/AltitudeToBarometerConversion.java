package com.sk.domain.mi.service.conversion.strategy;

import com.sk.domain.shared.enums.UnitType;

import java.math.BigDecimal;

public class AltitudeToBarometerConversion implements ConversionStrategy {

    @Override
    public String getFormulaServiceNm() {
        return "altitudeToBarometer";
    }

    @Override
    public BigDecimal convert(BigDecimal altitude) {
        return null;
    }

    @Override
    public boolean supports(UnitType fromUnit, UnitType toUnit) {
        return fromUnit == UnitType.METER && toUnit == UnitType.HPA;
    }

}
