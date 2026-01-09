package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.RenderAs;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record Series(

    String seriesId,
    String label,
    RenderAs renderAs,
    List<BigDecimal> values,
    boolean isVisible,
    String color,
    BigDecimal dtick,
    List<BigDecimal> range,
    Boolean isBidirectional
) {

    private static SeriesBuilder builderOf(String seriesId, String label, RenderAs renderAs,
        List<BigDecimal> values, boolean isVisible) {
        return Series.builder()
            .seriesId(seriesId)
            .label(label)
            .renderAs(renderAs)
            .values(values)
            .isVisible(isVisible);
    }

    public static Series of(String seriesId, String label, RenderAs renderAs,
        List<BigDecimal> values, boolean isVisible) {
        return builderOf(seriesId, label, renderAs, values, isVisible).build();
    }

    public static Series ofWithChartOption(String seriesId, String label, RenderAs renderAs,
        List<BigDecimal> values, boolean isVisible, String color, String dtick, String rangeStr,
        Boolean isBidirectional) {
        return Series.builder()
            .seriesId(seriesId)
            .label(label)
            .renderAs(renderAs)
            .values(values)
            .isVisible(isVisible)
            .color(color)
            .dtick(new BigDecimal(dtick))
            .range(parseRangeString(rangeStr))
            .isBidirectional(isBidirectional)
            .build();
    }

    public static List<BigDecimal> parseRangeString(String rangeStr) {
        if (rangeStr == null || rangeStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<BigDecimal> result = new ArrayList<>();

        String cleanStr = rangeStr.trim()
            .replaceAll("^\\[", "")
            .replaceAll("\\]$", "")
            .trim();

        if (cleanStr.isEmpty()) {
            return result;
        }

        String[] parts = cleanStr.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    result.add(new BigDecimal(trimmed));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format in range: " + trimmed,
                        e);
                }
            }
        }
        return result;
    }
}
