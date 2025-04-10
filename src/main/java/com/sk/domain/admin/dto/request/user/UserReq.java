package com.sk.domain.admin.dto.request.user;

import java.util.List;

import org.hibernate.validator.constraints.UniqueElements;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record UserReq() {
    @Schema(name = "userInsert")
    public record Insert(

        @UniqueElements(message = "IM코드 목록에 중복된 값이 있습니다.")
        @NotEmpty(message = "IM코드는 필수 입력 사항입니다.")
        @Schema(name = "imCd", description = "IM코드", example = "[\"NO07\", \"NO08\"]")
        List<String> imCd

    ) {}
}
