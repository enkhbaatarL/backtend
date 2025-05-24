package com.sk.domain.admin.dto.request.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ClientAccessInfoReq(

) {

    @Schema($schema = "ClientAccessInfoReq.Insert")
    public record Insert(

        @Schema(description = "고객회사 번호", example = "1000005")
        @NotNull
        Integer customerCompanyNo,

        @Schema(
            description = "클라이언트 인증 ID",
            example = "skcc-api",
            pattern = "^[A-Za-z-]+$"
        )
        @NotBlank
        @Pattern(
            regexp = "^[A-Za-z-]+$",
            message = "Only uppercase and lowercase English letters and hyphens are allowed."
        )
        String clientCertId

    ) {

    }

}
