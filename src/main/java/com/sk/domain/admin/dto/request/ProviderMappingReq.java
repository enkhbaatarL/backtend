package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProviderMappingReq(

    @NotNull
    Integer providerNo

) {
}
