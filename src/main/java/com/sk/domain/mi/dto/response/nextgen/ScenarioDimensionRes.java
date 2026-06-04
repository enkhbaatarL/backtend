package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 차원값 (표/개별 모드의 한 칼럼 셀, 시나리오 상세의 주요 가정 카드). 그룹별 동적.
 */
public record ScenarioDimensionRes(

    @Schema(description = "차원 키 (dimensionColumns의 key와 매칭)", example = "risk_premium")
    String key,

    @Schema(description = "차원 명", example = "리스크 프리미엄")
    String label,

    @Schema(description = "차원 값 코드", example = "mid")
    String value,

    @Schema(description = "차원 값 표시 라벨", example = "중간")
    String valueLabel

) {}
