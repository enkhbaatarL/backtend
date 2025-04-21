package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.ChartRenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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
    ChartRenderType renderAs,

    @Schema(description = "화면에 표시될 컬럼 레이블", example = "시가")
    String label,

    @Schema(description = "화면 표시 여부", example = "boolean")
    Boolean hidden

) {

    @Builder
    public SchemaEntry(String name, ChartRenderType renderAs, String label, Boolean hidden) {
        this.name = name;
        this.renderAs = renderAs;
        this.label = label;
        this.hidden = hidden;
    }

    public static SchemaEntry of(String name, ChartRenderType renderAs, String label, Boolean hidden) {
        return SchemaEntry.builder()
            .name(name)
            .renderAs(renderAs)
            .label(label)
            .hidden(hidden)
            .build();
    }

}
