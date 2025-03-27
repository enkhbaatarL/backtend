package com.sk.domain.mi.service.conversion_on_demand;

import com.sk.domain.mi.service.conversion_on_demand.strategy.ConversionStrategy;
import com.sk.domain.mi.service.conversion_on_demand.strategy.FactorBasedConversion;
import com.sk.domain.mi.service.conversion_on_demand.strategy.WaferToChipEaConversion;
import com.sk.domain.shared.enums.UnitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component("conversionStrategyFactory_on_demand")
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
