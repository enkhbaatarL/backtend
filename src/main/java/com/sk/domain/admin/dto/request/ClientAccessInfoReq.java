package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClientAccessInfoReq(

) {

    public record Insert(

        @Schema(description = "고객회사 번호", example = "")
        Integer customerCompanyNo,

        @Schema(description = "클라이언트 인증 ID", example = "")
        String clientCertId,

        @Schema(description = "클라이언트 시크릿키", example = "")
        String clientSecretKey,

        @Schema(description = "인증유형 명", example = "")
        String certTypeNm,

        @Schema(description = "인증방법 명", example = "")
        String certMethodNm,

        @Schema(description = "허용권한범위 명", example = "")
        String permissionAuthScopeNm

    ) {

    }

    public record Update(

        @Schema(description = "클라이언트 접속 등록번호", example = "")
        Integer clientAccessRegNo,

        @Schema(description = "고객회사 번호", example = "")
        Integer customerCompanyNo,

        @Schema(description = "클라이언트 인증 ID", example = "")
        String clientCertId,

        @Schema(description = "클라이언트 시크릿키", example = "")
        String clientSecretKey,

        @Schema(description = "인증유형 명", example = "")
        String certTypeNm,

        @Schema(description = "인증방법 명", example = "")
        String certMethodNm,

        @Schema(description = "허용권한범위 명", example = "")
        String permissionAuthScopeNm

    ) {

    }

}
