package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record UserReq() {
    public record Insert(

        @NotEmpty(message = "IM코드는 필수 입력 사항입니다.")
        @Schema(description = "IM코드", example = "")
        String imCd

    ) {}
}
