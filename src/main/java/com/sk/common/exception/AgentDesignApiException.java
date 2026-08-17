package com.sk.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 외부 Agent API 오류 응답 예외 (시나리오 설계 v4).
 *
 * Agent 오류 body의 data 객체(error_name, scenarioGroupId, operationId)는 B.E 내부 처리용으로만 보존한다 —
 * 409 자동 재연결이 data의 식별자를 소비한다. FE 응답에는 싣지 않으며(GlobalExceptionHandler가
 * HTTP status 유지 + code/message만 래핑), FE는 오류 수신 후 이벤트 조합 설계 상태 조회 API 재조회로 CTA를 갱신한다.
 */
@Getter
public class AgentDesignApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ResultCode.Error errorCode;
    private final String agentMessage;
    /** Agent 오류 body의 data 원문 (error_code, error_name, scenarioGroupId, operationId 등). 없으면 null */
    private final JsonNode agentData;

    private AgentDesignApiException(HttpStatus httpStatus, ResultCode.Error errorCode, String agentMessage, JsonNode agentData) {
        super(agentMessage);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.agentMessage = agentMessage;
        this.agentData = agentData;
    }

    public static AgentDesignApiException of(HttpStatus httpStatus, ResultCode.Error errorCode, String agentMessage, JsonNode agentData) {
        return new AgentDesignApiException(httpStatus, errorCode, agentMessage, agentData);
    }

}
