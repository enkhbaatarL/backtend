package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record TestVerifyTokenReq(

        @NotEmpty @Schema(description = "토큰", example = "1234567890") String token

) {
}
