package com.sk.domain.mi.dto.response.insights;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;

public record FocusedDataCache(
        List<FocusedDataDto> data) {

    @Builder
    @JsonCreator
    public FocusedDataCache(List<FocusedDataDto> data) {
        this.data = data;
    }

    public static FocusedDataCache of(List<FocusedDataDto> data) {
        return FocusedDataCache.builder()
                .data(data)
                .build();
    }
}
