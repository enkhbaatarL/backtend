package com.sk.domain.mi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MiCompanyLogoRes(

    @Schema(description = "고객회사번호", example = "1")
    Integer customerCompanyNo,

    @Schema(
        description = "로고 이미지 URL, 회사 전용 로고가 없으면 null",
        example = "https://dev-mi.sk.com/images/sk-mi-d-osb-03/CPNYLOGO/3f2a8c1e-0d44-4b7b-9a11-2c5e7f6d8a90.png"
    )
    String logoUrl

) {

    public static MiCompanyLogoRes of(Integer customerCompanyNo, String logoUrl) {
        return new MiCompanyLogoRes(customerCompanyNo, logoUrl);
    }

}
