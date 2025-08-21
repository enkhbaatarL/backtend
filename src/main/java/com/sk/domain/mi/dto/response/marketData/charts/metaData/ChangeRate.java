package com.sk.domain.mi.dto.response.marketData.charts.metaData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ChangeRate (
    @Schema(description = "비교수치 이름", example = "YoY") String label,
    @Schema(description = "비교수치 Rate", example = "1.0") Double value
){
    @Builder
    public ChangeRate(String label, Double value){
        this.label = label;
        this.value = value;
    }
}
