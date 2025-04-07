package com.sk.domain.admin.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ClientAccessSecretRes(

    @Schema(description = "클라이언트 시크릿키", example = "")
    String clientSecretKey

) {

    @Builder
    public ClientAccessSecretRes(String clientSecretKey) {
        this.clientSecretKey = clientSecretKey;
    }

    public static ClientAccessSecretRes of(String clientSecretKey) {
        return ClientAccessSecretRes.builder()
            .clientSecretKey(clientSecretKey)
            .build();
    }

}
