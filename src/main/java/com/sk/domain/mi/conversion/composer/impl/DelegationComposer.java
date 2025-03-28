package com.sk.domain.mi.conversion.composer.impl;

import com.sk.domain.mi.conversion.composer.DelegatingConversionStrategy;
import com.sk.domain.mi.conversion.composer.StrategyComposer;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.util.List;

/**
 * 위임 방식으로 전략을 조합하는 구현체 (A, B -> C)
 * 여러 전략에게 작업을 위임하고 그 결과를 조합
 */
public class DelegationComposer implements StrategyComposer {

    @Override
    public ConversionStrategy compose(List<ConversionStrategy> strategies, FormulaService targetService) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("At least one strategy must be provided");
        }

        if (strategies.size() == 1) {
            return strategies.get(0); // 단일 전략인 경우 그대로 반환
        }

        // 위임 방식의 전략 생성
        return new DelegatingConversionStrategy(strategies, targetService);
    }

}
