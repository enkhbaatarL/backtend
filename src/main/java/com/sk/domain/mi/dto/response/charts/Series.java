package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.RenderAs;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record Series(

    String seriesId,
    String label,
    RenderAs renderAs,
    List<BigDecimal> values,
    boolean isVisible
) {
    private static SeriesBuilder builderOf(String seriesId, String label, RenderAs renderAs, List<BigDecimal> values, boolean isVisible) {
        return Series.builder()
            .seriesId(seriesId)
            .label(label)
            .renderAs(renderAs)
            .values(values)
            .isVisible(isVisible);
    }
    public static Series of(String seriesId, String label, RenderAs renderAs, List<BigDecimal> values, boolean isVisible) {
        return builderOf(seriesId, label, renderAs, values, isVisible).build();
    }

}
