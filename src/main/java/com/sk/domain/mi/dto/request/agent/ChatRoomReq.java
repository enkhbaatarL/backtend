package com.sk.domain.mi.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomReq(

    @Schema(description = "제목", example = "삼성전자 주가 문의")
    String title

) {

}
