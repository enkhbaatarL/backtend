package com.sk.domain.admin.dto.request.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CompanyProviderMapReq(
    ) {

    @Schema(name = "CompanyProviderMapReq.Insert")
    public record Insert(

        @NotNull
        @Schema(description = "프로바이더 번호", example = "159")
        Integer providerNo

    ) {
    }

    @Schema(name = "CompanyProviderMapReq.Update")
    public record Update(

        @NotNull
        @Schema(description = "사용 여부", example = "boolean")
        Boolean isUsed

    ) {
    }

}
