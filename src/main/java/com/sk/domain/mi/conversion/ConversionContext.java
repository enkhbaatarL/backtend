package com.sk.domain.mi.conversion;

import com.sk.domain.mi.conversion.composer.StrategyComposer;
import com.sk.domain.mi.conversion.composer.impl.ChainComposer;
import com.sk.domain.mi.conversion.composer.impl.DelegationComposer;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion.strategy.basic.StandardDeviationConversion;
import com.sk.domain.mi.conversion.strategy.complex.BollingerBandsConversion;
import com.sk.domain.mi.conversion_on_demand.FormulaService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 변환 전략들을 관리하고 접근하는 컨텍스트 클래스
 */
@Component
public class ConversionContext {

    private final List<ConversionStrategy> strategies = new ArrayList<>();
    private final Map<String, StrategyComposer> composers = new HashMap<>();

    @PostConstruct
    public void init() {
        // 기본 전략 등록
//        registerStrategy(new WaferToChipEaConversion());
        registerStrategy(new MovingAverageConversion());
        registerStrategy(new StandardDeviationConversion());

        // 복합 전략 등록
        registerStrategy(new BollingerBandsConversion(new BigDecimal("2")));

        // 컴포저 등록
        composers.put("chain", new ChainComposer());
        composers.put("delegation", new DelegationComposer());
    }

    /**
     * 전략 등록
     */
    public void registerStrategy(ConversionStrategy strategy) {
        strategies.add(strategy);
    }

    /**
     * 단일 값 변환
     */
    public BigDecimal convert(FormulaService formulaService, BigDecimal... values) {
        return findStrategy(formulaService)
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found for: " + formulaService))
            .convert(values);
    }

    /**
     * 시계열 데이터 변환
     */
    public List<BigDecimal> convertTimeSeries(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize) {
        return findStrategy(formulaService)
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found for: " + formulaService))
            .convertTimeSeries(timeSeries, windowSize);
    }

    /**
     * 특정 결과 타입을 반환하는 시계열 데이터 변환
     */
    public <T> List<T> convertTimeSeries(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize, Class<T> resultType) {
        return findStrategy(formulaService)
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found for: " + formulaService))
            .convertTimeSeries(timeSeries, windowSize, resultType);
    }

    /**
     * 전략 조합 생성 (체이닝 방식)
     */
    public ConversionStrategy createChainedStrategy(List<ConversionStrategy> strategies, FormulaService targetService) {
        return composers.get("chain").compose(strategies, targetService);
    }

    /**
     * 전략 조합 생성 (위임 방식)
     */
    public ConversionStrategy createDelegatingStrategy(List<ConversionStrategy> strategies, FormulaService targetService) {
        return composers.get("delegation").compose(strategies, targetService);
    }

    /**
     * 특정 서비스에 대한 전략 찾기
     */
    public Optional<ConversionStrategy> findStrategy(FormulaService formulaService) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(formulaService))
            .findFirst();
    }
}
