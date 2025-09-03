package com.sk.domain.shared.dto.request.external;

import jakarta.validation.constraints.NotBlank;

public record ExternalLoginReq(
    @NotBlank(message = "로그인 ID는 필수입니다.")
    String loginId,

    @NotBlank(message = "비밀번호는 필수입니다.")
    String password
) {}