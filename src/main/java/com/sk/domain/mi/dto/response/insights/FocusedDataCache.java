package com.sk.domain.mi.dto.response.insights;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;

public record FocusedDataCache(
        List<FocusedDataRes> data) {

    @Builder
    @JsonCreator
    public FocusedDataCache(List<FocusedDataRes> data) {
        this.data = data;
    }

    public static FocusedDataCache of(List<FocusedDataRes> data) {
        return FocusedDataCache.builder()
                .data(data)
                .build();
    }
}
