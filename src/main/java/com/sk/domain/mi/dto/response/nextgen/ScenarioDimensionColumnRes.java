package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 그룹의 동적 표 헤더 정의.
 * description/rationale은 v4(definitions 기반) 그룹에서만 채워지며 v1 template 기반 그룹은 null이다.
 */
public record ScenarioDimensionColumnRes(

    @Schema(description = "차원 키", example = "trigger_timing")
    String key,

    @Schema(description = "차원 라벨 (표 헤더)", example = "촉발 시점")
    String label,

    @Schema(description = "분기항목 설명", example = "유럽향 화물 유인이 유지됩니까?")
    String description,

    @Schema(description = "분기항목 선정 근거", example = "지역 스프레드 지속 여부가 JKM 압력을 가름함")
    String rationale

) {}
