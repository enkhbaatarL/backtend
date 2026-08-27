package com.sk.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.utils.JsonUtil;
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


    /**
     * SSE 경로(생성 스트림)용 — 409/403만 Agent가 판정한 업무 상태로 보고 status를 보존한다.
     * 409는 B.E가 agentData의 식별자로 자동 재연결하는 원료이고, 403은 FE가 CTA 재조회로 대응하는 계약이다.
     * 그 외 4xx/5xx는 전부 B.E-Agent 구간 문제로 보고 500 + API-004로 내린다
     * (선검증을 통과한 요청이 Agent에서 거절되면 계약 문제라는 전제 — REST 프록시와 동일 정책).
     */
    public static AgentDesignApiException fromAgentBody(int statusValue, String bodyText) {
        return switch (statusValue) {
            case 409 -> new AgentDesignApiException(HttpStatus.CONFLICT, ResultCode.Error.BIZ_DESIGN_OCCUPIED, parseMessage(bodyText), parseData(bodyText));
            case 403 -> new AgentDesignApiException(HttpStatus.FORBIDDEN, ResultCode.Error.BIZ_DESIGN_OCCUPIED, parseMessage(bodyText), parseData(bodyText));
            default -> internalFromAgentBody(bodyText);
        };
    }

    /**
     * REST 프록시용 — Agent 오류는 B.E-Agent 구간 문제라 FE에는 500 + API-004(표준 메시지)로 내린다.
     * Agent 오류 원문(message, data)은 예외에 보존하며 GlobalExceptionHandler가 로그로만 남긴다.
     */
    public static AgentDesignApiException internalFromAgentBody(String bodyText) {
        return new AgentDesignApiException(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.Error.API_RESPONSE_ERROR, parseMessage(bodyText), parseData(bodyText));
    }

    /**
     * Agent 오류 body에서 메시지를 추출한다. 공통 body(status/message/data)의 message를 우선하고,
     * FastAPI 프레임워크 오류({"detail": ...} 형식 — 라우트 없음 404, 요청 검증 422)는 detail을 쓴다.
     * 파싱 불가 본문은 원문을 그대로 반환한다.
     */
    private static String parseMessage(String bodyText) {
        try {
            JsonNode bodyNode = JsonUtil.fromJson(bodyText, JsonNode.class);
            if (bodyNode == null) {
                return bodyText;
            }
            if (!bodyNode.hasNonNull("data") && bodyNode.has("detail")) {
                return bodyNode.get("detail").asText(bodyNode.get("detail").toString());
            }
            return bodyNode.path("message").asText(bodyText);
        } catch (Exception ignored) {
            return bodyText;
        }
    }

    private static JsonNode parseData(String bodyText) {
        try {
            JsonNode bodyNode = JsonUtil.fromJson(bodyText, JsonNode.class);
            return bodyNode != null && bodyNode.hasNonNull("data") ? bodyNode.get("data") : null;
        } catch (Exception ignored) {
            return null;
        }
    }

}
