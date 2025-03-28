package com.sk.domain.mi.conversion_on_demand.strategy;

import com.sk.domain.mi.conversion_on_demand.strategy.composite.ChainedConversionStrategy;

public abstract class AbstractComposableConversionStrategy implements ComposableConversionStrategy {

    @Override
    public ComposableConversionStrategy andThen(ComposableConversionStrategy next) {
        return new ChainedConversionStrategy(this, next);
    }

}
