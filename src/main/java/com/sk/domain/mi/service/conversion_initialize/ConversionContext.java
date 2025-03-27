package com.sk.domain.mi.service.conversion_initialize;

import com.sk.domain.mi.service.conversion_initialize.strategy.ConversionStrategy;
import com.sk.domain.mi.service.conversion_initialize.strategy.WaferToChipEaConversion;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ConversionContext {

    private final List<ConversionStrategy> complexStrategies = new ArrayList<>();

    @PostConstruct
    public void init() {
        registerStrategy(new WaferToChipEaConversion());
    }

    public void registerStrategy(ConversionStrategy strategy) {
        complexStrategies.add(strategy);
    }

    public BigDecimal convert(FormulaService formulaService, BigDecimal... value) {
        return complexStrategies.stream()
            .filter(strategy -> strategy.supports(formulaService))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new)
            .convert(value);
    }

    public Optional<ConversionStrategy> findComplexStrategy(FormulaService formulaService) {
        return complexStrategies.stream()
            .filter(strategy -> strategy.supports(formulaService))
            .findFirst();
    }

}
