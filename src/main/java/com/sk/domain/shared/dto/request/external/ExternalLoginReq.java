package com.sk.domain.shared.dto.request.external;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "외부 로그인 요청")
public record ExternalLoginReq(

    @Schema(description = "로그인 ID", example = "user123", maxLength = 50)
    @NotBlank(message = "로그인 ID는 필수입니다.")
    @Size(min = 3, max = 50, message = "로그인 ID는 3자 이상 50자 이하여야 합니다.")
    String loginId,

    @Schema(description = "비밀번호", example = "password123", maxLength = 100)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 1, max = 200, message = "비밀번호는 1자 이상 200자 이하여야 합니다.")
    String password

) {}