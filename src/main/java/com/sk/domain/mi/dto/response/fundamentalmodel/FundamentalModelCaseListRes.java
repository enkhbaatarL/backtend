package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelCaseListRes(

    @Schema(description = "필터 적용 후 전체 case 수", example = "5")
    Integer totalCount,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelCaseListItemRes.class))
    List<FundamentalModelCaseListItemRes> cases

) {
}
