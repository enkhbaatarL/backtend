package com.sk.domain.mi.conversion_initialize;

import com.sk.domain.mi.conversion_initialize.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_initialize.strategy.FactorBasedConversion;
import com.sk.domain.mi.conversion_initialize.strategy.WaferToChipEaConversion;
import com.sk.domain.shared.enums.UnitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component("conversionStrategyFactory_initialize")
public class ConversionStrategyFactory {

    public ConversionStrategy createFactorStrategy(UnitType fromUnit, UnitType toUnit, BigDecimal factor) {
        return new FactorBasedConversion(fromUnit, toUnit, factor);
    }

    public List<ConversionStrategy> createComplexStrategies() {
        List<ConversionStrategy> strategies = new ArrayList<>();
        strategies.add(new WaferToChipEaConversion());
        return strategies;
    }

}
