package com.sk.domain.shared.client.dto.response;

import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;

/**
 * [MCP Event Generate] 이벤트 생성 서버 응답 envelope (status: success | error).
 */
public record EventGenerationAgentRes(

    String status,
    String message,
    EventGenerationRes data

) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

}
