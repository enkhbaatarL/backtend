package com.sk.domain.mi.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;

public record QnaReq(

) {

    @Schema(name = "QnaReq.Insert")
    public record Insert(

        @Schema(description = "질의문", example = "삼성전자 작년 실적을 비교해줘")
        String question

    ) {
    }

}
