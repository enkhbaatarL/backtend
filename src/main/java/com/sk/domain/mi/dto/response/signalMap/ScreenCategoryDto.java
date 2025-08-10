package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

// @formatter:off
public record ScreenCategoryDto(
    String categoryName,
    List<ScreenDetailCategoryDto> items
) {

    public ScreenCategoryDto(String categoryName, List<ScreenDetailCategoryDto> items) {
        this.categoryName = categoryName;
        this.items = items;
    }
}
// @formatter:on