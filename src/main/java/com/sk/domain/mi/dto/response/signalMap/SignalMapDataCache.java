package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;

public record SignalMapDataCache(
        List<ScreenCategoryDto> data) {

    @Builder
    @JsonCreator
    public SignalMapDataCache(List<ScreenCategoryDto> data) {
        this.data = data;
    }

    public static SignalMapDataCache of(List<ScreenCategoryDto> data) {
        return SignalMapDataCache.builder()
                .data(data)
                .build();
    }
}
