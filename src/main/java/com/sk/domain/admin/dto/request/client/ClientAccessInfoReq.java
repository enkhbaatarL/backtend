package com.sk.domain.admin.dto.request.client;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClientAccessInfoReq(

) {

    @Schema($schema = "ClientAccessInfoReq.Insert")
    public record Insert(

            @Schema(description = "고객회사 번호", example = "1000005")
            Integer customerCompanyNo,

            @Schema(description = "클라이언트 인증 ID", example = "skccapi")
            String clientCertId

    ) {

    }

}
