package com.sk.domain.mi.dto.response.chart2;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스키마의 각 항목(컬럼)에 대한 정보를 나타내는 DTO
 *
 * @param name 컬럼명 (data 객체의 키와 일치)
 * @param renderAs 프론트엔드 렌더링 힌트 (예: "date", "open", "ma", "bb")
 * @param label 화면에 표시될 컬럼 레이블 (예: "날짜", "시가")
 */
@Schema(description = "차트 스키마 항목 정보")
public record SchemaEntry(

    @Schema(description = "데이터 컬럼명 (data 객체의 키와 일치)", example = "open_1")
    String name,

    @Schema(description = "프론트엔드 렌더링 힌트", example = "open")
    String renderAs,

    @Schema(description = "화면에 표시될 컬럼 레이블", example = "시가")
    String label

) {}
