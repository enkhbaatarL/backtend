package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FundamentalModelCaseListRes(

    @Schema(
        description = "화면에 표시되는 current MI Base(0~1건)와 필터된 cases.size()의 합",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Integer totalCount,

    @Schema(
        description = "검색 조건과 무관하게 고정 반환되는 current MI Base. 지정된 MI Base가 없으면 null",
        nullable = true,
        types = {"object", "null"}
    )
    FundamentalModelCaseListItemRes miBase,

    @Schema(
        description = "검색 조건이 적용된 active INDEPENDENT case 목록. 없으면 빈 배열",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    List<FundamentalModelCaseListItemRes> cases

) {
}
