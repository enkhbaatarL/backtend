package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelVolumeUnit;
import com.sk.domain.shared.enums.fundamentalmodel.VolumesRowKind;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

public record FundamentalModelVolumesRes(

    @Schema(description = "case ID", example = "106")
    Long caseId,

    @Schema(description = "조회 segment", implementation = FundamentalModelSegment.class, example = "GLOBAL")
    FundamentalModelSegment segment,

    @Schema(description = "시간 단위", implementation = FundamentalModelGranularity.class, example = "MONTHLY")
    FundamentalModelGranularity granularity,

    @Schema(description = "수치 단위. 현재는 MTPA 고정입니다.", implementation = FundamentalModelVolumeUnit.class, example = "MTPA")
    FundamentalModelVolumeUnit unit,

    @Schema(description = "실제 적용된 필터", implementation = VolumesAppliedFilters.class)
    VolumesAppliedFilters appliedFilters,

    @ArraySchema(schema = @Schema(implementation = VolumesColumn.class))
    List<VolumesColumn> columns,

    @ArraySchema(schema = @Schema(implementation = VolumesRow.class))
    List<VolumesRow> rows

) {

    public record VolumesAppliedFilters(

        @ArraySchema(schema = @Schema(implementation = FundamentalModelDataType.class, example = "DEMAND"))
        List<FundamentalModelDataType> types,

        @ArraySchema(schema = @Schema(implementation = FundamentalModelRegion.class, example = "ASIA"))
        List<FundamentalModelRegion> regions,

        @ArraySchema(schema = @Schema(
            description = "정규화된 Country key 목록. 미전달 시 빈 배열이며 선택 Region의 전체 Country가 적용됩니다.",
            example = "COUNTRY:ASIA:Japan"
        ))
        List<String> countryKeys,

        @ArraySchema(schema = @Schema(
            description = "요청으로 선택한 기간. 미전달 시 빈 배열이며 전체 기간이 적용됩니다.",
            example = "2026-01"
        ))
        List<String> periods

    ) {
    }

    public record VolumesColumn(

        @Schema(description = "안정적인 기간 key. MONTHLY이면 yyyy-MM 형식, YEARLY이면 yyyy 형식입니다.", example = "2026-01")
        String key,

        @Schema(description = "표시 라벨. MONTHLY이면 26-01 형식, YEARLY이면 2026 형식입니다.", example = "26-01")
        String label,

        @Schema(description = "연도", example = "2026")
        Integer year,

        @Schema(description = "월. YEARLY이면 null입니다.", example = "1")
        Integer month

    ) {
    }

    public record VolumesRow(

        @Schema(description = "데이터 타입", implementation = FundamentalModelDataType.class, example = "DEMAND")
        FundamentalModelDataType type,

        @Schema(description = "데이터 타입 화면 표시명", example = "Gas Demand")
        String typeLabel,

        @Schema(description = "지역", implementation = FundamentalModelRegion.class, example = "ASIA")
        FundamentalModelRegion region,

        @Schema(description = "국가명", example = "China")
        String country,

        @Schema(description = "행 구분", implementation = VolumesRowKind.class, example = "COUNTRY")
        VolumesRowKind rowKind,

        @Schema(description = "행 화면 표시명. REGION_SUBTOTAL은 지역명, COUNTRY는 국가명, TYPE_TOTAL은 Total입니다.", example = "China")
        String rowLabel,

        @Schema(description = "column key별 수치 map. key는 columns[].key와 동일합니다. JSON number이며 소수 다섯째 자리까지 0 방향 절삭합니다. Country 필터 합계는 원본 leaf 합산 후 한 번만 절삭합니다.", example = "{\"2026-01\":15.12345,\"2026-02\":0.28947}")
        @JsonSerialize(contentUsing = FundamentalModelFiveScaleTruncatingNumberSerializer.class)
        Map<String, Double> values

    ) {
    }
}
