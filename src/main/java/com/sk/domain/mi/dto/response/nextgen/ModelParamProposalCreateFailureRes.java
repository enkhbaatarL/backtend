package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelParamProposalCreateFailureRes(

    @Schema(description = "실패 사유",
        example = "파라미터로 반영할 변경 항목이 없습니다.")
    String failureReason
) {
}
