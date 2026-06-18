package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes.BaseSeries;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * CME 벤치마크 기준선 그래프 데이터 (단일 인덱스 × 단일 기간단위).
 * 전용 DB({@code midb-skgas})에서 조회하는 dedicated 전용 응답.
 */
public record CmeBenchmarkGraphRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "기간 단위 (monthly/yearly)", example = "monthly")
    GraphPeriodUnit periodUnit,

    @Schema(description = "기간 단위 라벨", example = "월별")
    String periodUnitLabel,

    @Schema(description = "통화 (USD 고정)", example = "USD")
    String currency,

    @Schema(description = "단위 (MMBtu 고정)", example = "MMBtu")
    String unit,

    @ArraySchema(
        arraySchema = @Schema(description = "x축 기간 목록 (월별 yyyy-MM / 연도별 yyyy)"),
        schema = @Schema(description = "기간", example = "2026-06")
    )
    List<String> dates,

    @Schema(description = "기준선 시리즈 (CME)")
    List<BaseSeries> baseSeries

) {

    private static final String CURRENCY = "USD";
    private static final String UNIT = "MMBtu";
    private static final String CME_SERIES_ID = "CME";
    private static final String CME_SERIES_NAME = "CME";

    /**
     * @param dates  기간 표기 목록 (values와 동일 순서)
     * @param values dates와 동일 순서의 CME 값 (비어 있으면 baseSeries 빈 배열)
     */
    public static CmeBenchmarkGraphRes of(IndexName indexName, GraphPeriodUnit periodUnit, List<String> dates, List<BigDecimal> values) {
        List<BaseSeries> baseSeries = values == null || values.isEmpty()
            ? List.of()
            : List.of(new BaseSeries(CME_SERIES_ID, CME_SERIES_NAME, values));

        return new CmeBenchmarkGraphRes(
            indexName.getCode(),
            periodUnit,
            periodUnit.getLabel(),
            CURRENCY,
            UNIT,
            dates,
            baseSeries
        );
    }
}
