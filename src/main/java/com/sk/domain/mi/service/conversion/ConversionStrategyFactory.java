package com.sk.domain.mi.service.conversion;

import com.sk.domain.mi.service.conversion.strategy.AltitudeToBarometerConversion;
import com.sk.domain.mi.service.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.service.conversion.strategy.FactorBasedConversion;
import com.sk.domain.shared.enums.UnitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConversionStrategyFactory {

    public ConversionStrategy createFactorStrategy(UnitType fromUnit, UnitType toUnit, BigDecimal factor) {
        return new FactorBasedConversion(fromUnit, toUnit, factor);
    }

    public List<ConversionStrategy> createComplexStrategies() {
        List<ConversionStrategy> strategies = new ArrayList<>();
        strategies.add(new AltitudeToBarometerConversion());
        return strategies;
    }

}
