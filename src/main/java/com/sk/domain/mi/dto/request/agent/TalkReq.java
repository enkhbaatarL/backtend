package com.sk.domain.mi.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TalkReq(

    @Schema(description = "챗봇대화 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID talkId,

    @Schema(description = "질의문", example = "삼성전자 작년 실적을 비교해줘")
    String question

) {

}
