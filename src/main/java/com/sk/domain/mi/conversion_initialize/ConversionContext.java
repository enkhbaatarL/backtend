package com.sk.domain.mi.conversion_initialize;

import com.sk.domain.mi.conversion_initialize.strategy.ConversionStrategy;
import com.sk.domain.shared.enums.UnitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component("conversionContext_initialize")
public class ConversionContext {

    private final List<ConversionStrategy> strategies = new ArrayList<>();

    public void registerStrategy(ConversionStrategy strategy) {
        strategies.add(strategy);
    }

    public BigDecimal convert(UnitType fromUnit, UnitType toUnit, BigDecimal... value) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(fromUnit, toUnit))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No conversion strategy found from " + fromUnit + " to " + toUnit))
            .convert(value);
    }

}
