package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

// @formatter:off
public record ScreenCategoryAndTopDto(
    List<SignalMapDataDto> topImpactVariables,
    List<ScreenCategoryDto> list
) {

    public ScreenCategoryAndTopDto(List<SignalMapDataDto> topImpactVariables, List<ScreenCategoryDto> list) {
        this.topImpactVariables = topImpactVariables;
        this.list = list;
    }
}
// @formatter:on
