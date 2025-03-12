package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

public record ProviderReq(
    @NotBlank(message = "프로바이더명은 빈 값일 수 없습니다.") //
    @Length(min = 1, max = 20) //
    String providerNm,

    @NotBlank(message = "프로바이더 코드는 빈 값일 수 없습니다.") //
    @Length(min = 1, max = 6) //
    String providerCd,

    @NotBlank(message = "프로바이더 설명은 빈 값일 수 없습니다.") //
    @Length(min = 1, max = 100) //
    String providerExpn,

    @NotNull(message = "프로바이더 사용유무는 빈 값일 수 없습니다.") //
    boolean isUsed) {
}
