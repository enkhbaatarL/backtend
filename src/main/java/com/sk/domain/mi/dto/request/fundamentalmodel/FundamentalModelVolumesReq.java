package com.sk.domain.mi.dto.request.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelVolumesReq(

    @Schema(description = "조회 segment", implementation = FundamentalModelSegment.class, example = "GLOBAL")
    FundamentalModelSegment segment,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelDataType.class, example = "DEMAND"))
    List<FundamentalModelDataType> types,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelRegion.class, example = "ASIA"))
    List<FundamentalModelRegion> regions,

    @Schema(description = "시간 단위. 미전달 시 MONTHLY입니다.", implementation = FundamentalModelGranularity.class, example = "MONTHLY")
    FundamentalModelGranularity granularity,

    @ArraySchema(schema = @Schema(
        description = "선택 기간. granularity=YEARLY이면 yyyy, MONTHLY이면 yyyy-MM 형식입니다. 미전달 시 전체 기간입니다.",
        example = "2026-01"
    ))
    List<String> periods

) {
}
