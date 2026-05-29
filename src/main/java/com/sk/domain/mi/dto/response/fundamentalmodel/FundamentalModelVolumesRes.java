package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.VolumesRowKind;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelVolumesRes(

    @Schema(description = "case ID", example = "106")
    Long caseId,

    @Schema(description = "조회 segment", implementation = FundamentalModelSegment.class, example = "GLOBAL")
    FundamentalModelSegment segment,

    @Schema(description = "시간 단위", implementation = FundamentalModelGranularity.class, example = "MONTHLY")
    FundamentalModelGranularity granularity,

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
        List<FundamentalModelRegion> regions

    ) {
    }

    public record VolumesColumn(

        @Schema(description = "연도", example = "2026")
        Integer year,

        @Schema(description = "월. YEARLY이면 null입니다.", example = "1")
        Integer month,

        @Schema(description = "표시 라벨", example = "2026-1")
        String periodLabel

    ) {
    }

    public record VolumesRow(

        @Schema(description = "데이터 타입", implementation = FundamentalModelDataType.class, example = "DEMAND")
        FundamentalModelDataType type,

        @Schema(description = "지역", implementation = FundamentalModelRegion.class, example = "ASIA")
        FundamentalModelRegion region,

        @Schema(description = "국가명", example = "China")
        String country,

        @Schema(description = "행 구분", implementation = VolumesRowKind.class, example = "COUNTRY")
        VolumesRowKind rowKind,

        @ArraySchema(schema = @Schema(description = "columns 순서와 1:1 대응하는 수치", example = "15.1"))
        List<Double> values

    ) {
    }
}
