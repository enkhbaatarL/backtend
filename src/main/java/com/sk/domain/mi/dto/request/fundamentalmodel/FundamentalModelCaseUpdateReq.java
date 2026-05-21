package com.sk.domain.mi.dto.request.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelCaseUpdateReq(

    @Schema(description = "Scenario Case 제목. trim 후 1-200자 필수입니다.", example = "Updated JKM price upside case")
    String title,

    @Schema(description = "Scenario Case 설명. nullable/optional입니다.", example = "Updated memo")
    String description

) {
}
