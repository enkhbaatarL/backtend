package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record TestLoginReq(

        @NotEmpty @Schema(description = "사용자 아이디", example = "mk.jang@twolinecode.com") String loginUserId

) {
}
