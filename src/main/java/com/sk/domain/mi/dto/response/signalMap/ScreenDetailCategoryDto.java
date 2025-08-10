package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

// @formatter:off
public record ScreenDetailCategoryDto(
        String categoryName,
        List<SignalMapDataDto> items) {

    public ScreenDetailCategoryDto(String categoryName, List<SignalMapDataDto> items) {
        this.categoryName = categoryName;
        this.items = items;
    }
}
// @formatter:on
