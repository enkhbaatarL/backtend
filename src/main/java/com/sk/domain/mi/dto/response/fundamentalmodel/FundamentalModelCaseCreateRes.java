package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record FundamentalModelCaseCreateRes(

    @Schema(description = "Scenario Case ID", example = "101")
    Long caseId,

    @Schema(description = "제목", example = "JKM price upside case")
    String title,

    @Schema(description = "설명", example = "May demand shock scenario case")
    String description,

    @Schema(description = "생성 직후 상태", implementation = FundamentalModelCaseStatus.class, example = "PENDING")
    FundamentalModelCaseStatus status,

    @ArraySchema(schema = @Schema(implementation = FundamentalModelCaseFileRes.class))
    List<FundamentalModelCaseFileRes> files,

    @Schema(description = "case 생성 시각")
    LocalDateTime createdAt,

    @Schema(description = "case 기준 상태 조회 endpoint")
    String statusEndpoint

) {
}
