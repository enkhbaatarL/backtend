package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 그룹의 동적 표 헤더 정의.
 */
public record ScenarioDimensionColumnRes(

    @Schema(description = "차원 키", example = "trigger_timing")
    String key,

    @Schema(description = "차원 라벨 (표 헤더)", example = "촉발 시점")
    String label

) {}
