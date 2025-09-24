package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.RenderAs;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record Series(

    String seriesId,
    String label,
    RenderAs renderAs,
    List<BigDecimal> values,
    boolean isVisible,
    String color
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

    public static Series ofWithColor(String seriesId, String label, RenderAs renderAs, List<BigDecimal> values, boolean isVisible, String color) {
        return Series.builder()
            .seriesId(seriesId)
            .label(label)
            .renderAs(renderAs)
            .values(values)
            .isVisible(isVisible)
            .color(color)
            .build();
    }

}
