package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ProviderReq(
        @NotBlank(message = "프로바이더명은 빈 값일 수 없습니다.") //
        @Length(min = 1, max = 20) //
        String providerName,

        @NotBlank(message = "프로바이더 설명은 빈 값일 수 없습니다.") //
        @Length(min = 1, max = 100) //
        String providerDesc,

        @NotBlank(message = "프로바이더 코드는 빈 값일 수 없습니다.") //
        @Length(min = 1, max = 6) //
        String providerCode,

        @NotBlank(message = "프로바이더 사용유무는 빈 값일 수 없습니다.") //
        @Length(min = 1, max = 1) //
        String providerUseYn) {
}
