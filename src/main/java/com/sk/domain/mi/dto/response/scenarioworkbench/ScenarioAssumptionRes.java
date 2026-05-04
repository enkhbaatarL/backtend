package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record ScenarioAssumptionRes(

    @Schema(description = "가정 항목명", example = "갈등 기간")
    String label,

    @Schema(description = "가정 값", example = "6개월 이상")
    String value,

    @Schema(description = "보조 텍스트 (부가 설명)", example = "완전 복구 기준")
    String supportingText,

    @Schema(description = "값 방향", example = "down")
    Direction direction

) {

    @Getter
    @RequiredArgsConstructor
    public enum Direction {
        UP("up"),
        DOWN("down"),
        NEUTRAL("neutral");

        @JsonValue
        private final String code;
    }

}
