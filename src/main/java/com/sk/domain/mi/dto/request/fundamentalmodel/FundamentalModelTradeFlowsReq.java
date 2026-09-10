package com.sk.domain.mi.dto.request.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowSelector;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowUnit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelTradeFlowsReq(

    @Schema(description = "Trade 유형", implementation = TradeFlowSelector.class, example = "LNG")
    TradeFlowSelector selector,

    @Schema(description = "조회 연도. 2023~2030 범위 필수", example = "2026")
    Integer year,

    @Schema(description = "수치 단위. 미전달 시 MTPA", implementation = TradeFlowUnit.class, example = "MTPA")
    TradeFlowUnit unit,

    @ArraySchema(schema = @Schema(implementation = TradeFlowDirection.class, example = "TO_COUNTRY"))
    List<TradeFlowDirection> types,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelRegion.class, example = "ASIA"))
    List<FundamentalModelRegion> regions,

    @ArraySchema(schema = @Schema(
        description = "Country 식별자. COUNTRY:{REGION}:{country} 형식이며 regions와 함께 전달하면 AND 조건입니다.",
        example = "COUNTRY:ASIA:Japan"
    ))
    List<String> countryKeys

) {
}
