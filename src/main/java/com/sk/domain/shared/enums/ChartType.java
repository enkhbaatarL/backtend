package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ChartType {

    CANDLE(List.of(
        ChartRenderType.DATE,
        ChartRenderType.CLOSE,
        ChartRenderType.OPEN,
        ChartRenderType.HIGH,
        ChartRenderType.LOW
    )),
    METRIC(List.of(
        ChartRenderType.DATE,
        ChartRenderType.MAIN,
        ChartRenderType.REFERENCE
    ));

    private final List<ChartRenderType> renderTypes;

    public ChartRenderType resolveRenderType(int index) {
        return index < renderTypes.size() ? renderTypes.get(index) : ChartRenderType.REFERENCE;
    }

}
