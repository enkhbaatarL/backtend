package com.sk.domain.mi.exception;

import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import lombok.Getter;

/**
 * Agent 스트리밍 처리 중 발생하는 예외
 */
@Getter
public class AgentStreamException extends RuntimeException {

    private final AnswerStatusCd statusCode;

    public AgentStreamException(String message, AnswerStatusCd statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public AgentStreamException(String message, AnswerStatusCd statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
