package com.sk.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 외부 Agent API 오류 응답 예외 (시나리오 설계 v4).
 *
 * Agent 계약상 FE는 오류 응답의 data.error_name으로 분기하고 data의 기존 리소스 ID
 * (scenarioGroupId, operationId)로 재연결/화면 이동을 수행한다. 이를 위해 Agent 오류 body의
 * data 객체를 원문 그대로 보존해 BaseResponse.data로 통과시킨다.
 * GlobalExceptionHandler가 HTTP status를 그대로 유지한 채 BaseResponse로 래핑한다.
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
