package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

public record ProviderReq(
        // @NotEmpty //
        @NotBlank(message = "프로바이더명은 빈 값일 수 없습니다.") //
        @Size(min = 1, max = 30) //
        String providerName,

        @NotEmpty //
        String providerDesc,

        @NotBlank(message = "프로바이더 코드는 빈 값일 수 없습니다.") //
        String providerCode,

        @NotBlank(message = "프로바이더 사용유무는 빈 값일 수 없습니다.") //
        String providerUseYn) {
}
