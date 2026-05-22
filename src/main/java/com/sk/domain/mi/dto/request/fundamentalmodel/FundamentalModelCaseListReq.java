package com.sk.domain.mi.dto.request.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelCaseListReq(

    @Schema(description = "검색 대상 필드", implementation = FundamentalModelCaseSearchType.class, example = "TITLE")
    FundamentalModelCaseSearchType searchType,

    @Schema(description = "검색 키워드", example = "JKM")
    String keyword

) {
}
