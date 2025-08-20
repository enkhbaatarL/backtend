package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;

public record SignalMapDataCache(
    ScreenCategoryAndTopDto data) {

    @Builder
    @JsonCreator
    public SignalMapDataCache(ScreenCategoryAndTopDto data) {
        this.data = data;
    }

    public static SignalMapDataCache of(ScreenCategoryAndTopDto data) {
        return SignalMapDataCache.builder()
                .data(data)
                .build();
    }
}
