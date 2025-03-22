package com.sk.domain.admin.dto.request;

import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TestLoginReq(

        @NotEmpty @Schema(description = "사용자 아이디", example = "mk.jang@twolinecode.com") String loginUserId,

        @NotNull @Schema(description = "사용자 역할", example = "USER") UserRoleCd userRoleCd

) {
}
