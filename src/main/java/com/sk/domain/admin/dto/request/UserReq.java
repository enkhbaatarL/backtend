package com.sk.domain.admin.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record UserReq() {
    @Schema(name = "userInsert")
    public record Insert(

        @NotEmpty(message = "IM코드는 필수 입력 사항입니다.")
        @Schema(name = "imCd", description = "IM코드", example = "[\"NO07\", \"NO08\"]")
        List<String> imCd

    ) {}
}
