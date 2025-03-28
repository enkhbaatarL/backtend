package com.sk.domain.mi.conversion_on_demand.strategy;

public interface ComposableConversionStrategy extends ConversionStrategy {

    ComposableConversionStrategy andThen(ComposableConversionStrategy next);

}
