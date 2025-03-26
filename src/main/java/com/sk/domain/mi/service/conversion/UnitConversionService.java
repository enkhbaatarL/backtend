package com.sk.domain.mi.service.conversion;

import com.sk.domain.mi.service.conversion.strategy.ConversionStrategy;
import com.sk.domain.shared.enums.UnitType;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final UnitConvertRepository unitConvertRepository;
    private final ConversionContext conversionContext;
    private final ConversionStrategyFactory strategyFactory;

    @PostConstruct
    public void init() {
        strategyFactory.createComplexStrategies().forEach(conversionContext::registerStrategy);

        unitConvertRepository.findAll().forEach(entity -> {
            if (StringUtils.isNoneEmpty(entity.getConvertPreviousUnitNm(), entity.getConvertAfterUnitNm())) {
                UnitType fromUnit = UnitType.getUnitTypeByName(entity.getConvertPreviousUnitNm());
                UnitType toUnit = UnitType.getUnitTypeByName(entity.getConvertAfterUnitNm());
                ConversionStrategy strategy = strategyFactory.createFactorStrategy(fromUnit, toUnit, entity.getConvertCoef());
                conversionContext.registerStrategy(strategy);
            }
        });
    }

    public BigDecimal convert(String fromUnitName, String toUnitName, BigDecimal value) {
        UnitType fromUnit = UnitType.getUnitTypeByName(fromUnitName);
        UnitType toUnit = UnitType.getUnitTypeByName(toUnitName);
        return conversionContext.convert(fromUnit, toUnit, value);
    }

}
