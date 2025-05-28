package com.sk.domain.mi.dto.response.signalMap;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record SignalMapDataRes(
        @Schema(description = "시그널맵 타이틀명", example = "")
        String title,
        
        @Schema(description = "시그널맵 업데이트 시간", example = "") 
        LocalDateTime updatedDt,

        @Schema(description = "Financial", example = "") 
        List<SignalMapDataDto> a,
        
        @Schema(description = "시그널맵 업데이트 시간", example = "") 
        List<SignalMapDataDto> b,
        
        @Schema(description = "시그널맵 업데이트 시간", example = "") 
        List<SignalMapDataDto> c,
        
        @Schema(description = "시그널맵 업데이트 시간", example = "") 
        List<SignalMapDataDto> d
) {

}
// @formatter:on
