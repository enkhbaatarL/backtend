package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.List.*;

@Getter
@RequiredArgsConstructor
public enum ChartType {

    CANDLE(of(RenderAs.CLOSE, RenderAs.OPEN, RenderAs.HIGH, RenderAs.LOW)),

    LINE(of(RenderAs.MAIN, RenderAs.REFERENCE));

    private final List<RenderAs> orderOfIndex;

    public RenderAs resolveRenderAs(int index) {
        return index < orderOfIndex.size() ? orderOfIndex.get(index) : RenderAs.REFERENCE;
    }

}
