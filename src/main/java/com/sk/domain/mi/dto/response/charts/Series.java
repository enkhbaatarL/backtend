package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.RenderAs;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

public record Series(

    String seriesId,
    String label,
    RenderAs renderAs,
    List<BigDecimal> values,
    boolean isVisible

) {

    @Builder
    public Series(String seriesId, String label, RenderAs renderAs, List<BigDecimal> values, boolean isVisible) {
        this.seriesId = seriesId;
        this.label = label;
        this.renderAs = renderAs;
        this.values = values;
        this.isVisible = isVisible;
    }

    public static Series of(String seriesId, String label, RenderAs renderAs, List<BigDecimal> values, boolean isVisible) {
        return Series.builder()
            .seriesId(seriesId)
            .label(label)
            .renderAs(renderAs)
            .values(values)
            .isVisible(isVisible)
            .build();
    }

}
