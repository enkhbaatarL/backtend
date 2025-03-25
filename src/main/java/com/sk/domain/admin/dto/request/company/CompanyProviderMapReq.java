package com.sk.domain.admin.dto.request.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// @formatter:off
public record CompanyProviderMapReq(

        @NotNull
        @Schema(description = "프로바이더 번호", example = "159")
        Integer providerNo

) {

    public record Insert(

            @NotNull
            @Schema(description = "프로바이더 번호", example = "159")
            Integer providerNo

    ) {
    }

    public record Update(

            @NotNull
            @Schema(description = "사용 여부", example = "boolean")
            Boolean isUsed

    ) {
    }

}
// @formatter:on