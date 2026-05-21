package com.sk.domain.mi.dto.request.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelRunReq(

    @Schema(description = "FE tracing용 optional 값. idempotency key로 사용하지 않습니다.", example = "client-request-001")
    String requestedByClientId

) {
}
