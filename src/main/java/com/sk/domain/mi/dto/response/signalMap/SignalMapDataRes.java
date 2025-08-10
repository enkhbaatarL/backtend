package com.sk.domain.mi.dto.response.signalMap;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record SignalMapDataRes(
        @Schema(description = "시그널맵 타이틀명", example = "")
        String title,
        
        @Schema(description = "시그널맵 업데이트 시간", example = "") 
        String updatedDt,

        @Schema(description = "", example = "") 
        List<ScreenCategoryDto> list
) {
    public SignalMapDataRes(String title, String updatedDt, List<ScreenCategoryDto> list) {
        this.title = title;
        this.updatedDt = updatedDt;
        this.list = list;
    }
}
// @formatter:on
