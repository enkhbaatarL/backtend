package com.sk.domain.mi.conversion.composer.impl;

import com.sk.domain.mi.conversion.composer.StrategyComposer;
import com.sk.domain.mi.conversion.composer.ChainedConversionStrategy;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.util.List;

/**
 * 체이닝 방식으로 전략을 조합하는 구현체 (A -> B -> C)
 */
public class ChainComposer implements StrategyComposer {

    @Override
    public ConversionStrategy compose(List<ConversionStrategy> strategies, FormulaService targetService) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("At least one strategy must be provided");
        }

        if (strategies.size() == 1) {
            return strategies.get(0); // 단일 전략인 경우 그대로 반환
        }

        // 순차적으로 전략들을 체이닝
        ConversionStrategy result = strategies.get(0);
        for (int i = 1; i < strategies.size(); i++) {
            result = new ChainedConversionStrategy(result, strategies.get(i), targetService);
        }

        return result;
    }

}
