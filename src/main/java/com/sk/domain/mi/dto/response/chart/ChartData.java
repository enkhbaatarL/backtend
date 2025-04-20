package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.ChartType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 단일 차트(메인 또는 비교군) 데이터를 나타내는 DTO
 *
 * @param chartType 차트 종류 (예: "CANDLE", "METRIC")
 * @param columns   데이터에 포함된 컬럼명 목록 (순서 정보 제공 가능)
 * @param schema    각 컬럼에 대한 메타데이터 목록 (렌더링 힌트, 레이블 등)
 * @param data      실제 차트 데이터 (컬럼명을 키로, 값 배열을 값으로 가짐). 값 배열의 타입은 날짜(String) 또는 숫자(Number/Double)일 수 있음.
 */
@Schema(description = "단일 차트 데이터 (메인/비교군)")
public record ChartData(

    @Schema(description = "차트 종류", example = "CANDLE")
    ChartType chartType,

    @Schema(description = "데이터 컬럼명 목록")
    List<String> columns,

    @Schema(description = "각 컬럼의 스키마 정보 목록")
    List<SchemaEntry> schema,

    @Schema(description = "실제 차트 데이터 (Key: 컬럼명, Value: 데이터 배열)")
    Map<String, List<?>> data

) {}
