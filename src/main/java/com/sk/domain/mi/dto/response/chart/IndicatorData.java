package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.IndicatorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 단일 보조 지표 데이터를 나타내는 DTO
 *
 * @param indicatorType 보조 지표 종류 (예: "MA", "BB")
 * @param columns       데이터에 포함된 컬럼명 목록 (순서 정보 제공 가능)
 * @param schema        각 컬럼에 대한 메타데이터 목록 (렌더링 힌트, 레이블 등)
 * @param data          실제 보조 지표 데이터 (컬럼명을 키로, 값 배열을 값으로 가짐). 값 배열의 타입은 날짜(String) 또는 숫자(Number/Double)일 수 있음.
 */
@Schema(description = "단일 보조 지표 데이터")
public record IndicatorData(

    @Schema(description = "보조 지표 종류", example = "MA")
    IndicatorType indicatorType,

    @Schema(description = "데이터 컬럼명 목록")
    List<String> columns,

    @Schema(description = "각 컬럼의 스키마 정보 목록")
    List<SchemaEntry> schema,

    @Schema(description = "실제 보조 지표 데이터 (Key: 컬럼명, Value: 데이터 배열)")
    Map<String, List<?>> data

) {

    @Builder
    public IndicatorData(IndicatorType indicatorType, List<String> columns, List<SchemaEntry> schema, Map<String, List<?>> data) {
        this.indicatorType = indicatorType;
        this.columns = columns;
        this.schema = schema;
        this.data = data;
    }

    public static IndicatorData of(IndicatorType type, List<String> columns, List<SchemaEntry> schema, Map<String, List<?>> data) {
        return IndicatorData.builder()
            .indicatorType(type)
            .columns(columns)
            .schema(schema)
            .data(data)
            .build();
    }

}
