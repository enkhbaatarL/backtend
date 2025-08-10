package com.sk.domain.mi.conversion;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion.strategy.ResultWrapperConversionStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 변환 전략들을 관리하고 접근하는 컨텍스트 클래스
 */
@Component
@RequiredArgsConstructor
public class UnitConversionContext {


    private final List<ConversionStrategy> strategies;
    private final Map<FormulaService, ConversionStrategy> strategyMap = new EnumMap<>(FormulaService.class);

    @PostConstruct
    public void init() {
        strategies.forEach(strategy -> strategyMap.put(strategy.getFormulaService(), strategy));
    }

    public Optional<ConversionStrategy> findStrategy(FormulaService formulaService) {
        return Optional.ofNullable(strategyMap.get(formulaService));
    }

    public BigDecimal convert(FormulaService formulaService, BigDecimal values) {
        return findStrategy(formulaService)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_COMPONENT_NOT_FOUND,
                "Conversion strategy not found for 'FormulaService': %s".formatted(formulaService.getCode())))
            .convert(values);
    }

    public List<BigDecimal> convertTimeSeries(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize) {
        return findStrategy(formulaService)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_COMPONENT_NOT_FOUND,
                "Conversion strategy not found for 'FormulaService': %s".formatted(formulaService.getCode())))
            .convertTimeSeries(timeSeries, windowSize);
    }

    @SuppressWarnings("unchecked")
    public <R extends ConversionResult> List<R> convertTimeSeriesResult(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize) {

        ConversionStrategy conversionStrategy = findStrategy(formulaService)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_COMPONENT_NOT_FOUND,
                "Conversion strategy not found for 'FormulaService': %s".formatted(formulaService.getCode())));

        if (conversionStrategy instanceof ResultWrapperConversionStrategy) {
            ResultWrapperConversionStrategy<R> resultWrapperConversionStrategy = (ResultWrapperConversionStrategy<R>) conversionStrategy;
            return resultWrapperConversionStrategy.convertTimeSeries(timeSeries, windowSize, resultWrapperConversionStrategy.resultType());
        }

        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Conversion strategy(%s) does not support complex result types".formatted(formulaService.getCode()));
    }

}
