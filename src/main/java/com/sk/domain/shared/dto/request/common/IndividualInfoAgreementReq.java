package com.sk.domain.shared.dto.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "개인정보동의여부 변경 요청")
public record IndividualInfoAgreementReq(

    @Schema(description = "개인정보동의여부", example = "true")
    @NotNull(message = "개인정보동의여부는 필수입니다.")
    Boolean isIndividualInfoAgreement

) {}
