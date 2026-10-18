package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.common.enums.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 입력 편집 경로의 값/수식/내부 예외를 노출하지 않는 오류. */
@Getter
public final class InputDraftException extends RuntimeException {
    private final HttpStatus status;
    private final ResultCode.Error code;
    private final String reason;
    private final Long revision;
    private InputDraftException(HttpStatus status, ResultCode.Error code, String reason, Long revision) {
        super("입력 편집 요청을 처리할 수 없습니다.");
        this.status=status; this.code=code; this.reason=reason; this.revision=revision;
    }
    public static InputDraftException invalid() { return new InputDraftException(HttpStatus.BAD_REQUEST,ResultCode.Error.REQ_INVALID_PARAMETER,"INVALID_REQUEST",null); }
    public static InputDraftException json() { return new InputDraftException(HttpStatus.BAD_REQUEST,ResultCode.Error.REQ_INVALID_JSON,"INVALID_REQUEST",null); }
    public static InputDraftException missing() { return new InputDraftException(HttpStatus.NOT_FOUND,ResultCode.Error.RSC_NOT_FOUND,null,null); }
    public static InputDraftException conflict(String reason, Long revision) { return new InputDraftException(HttpStatus.CONFLICT,ResultCode.Error.BIZ_RULE_VIOLATION,reason,revision); }
    public static InputDraftException large() { return new InputDraftException(HttpStatus.PAYLOAD_TOO_LARGE,ResultCode.Error.REQ_INVALID_PARAMETER,"PAYLOAD_TOO_LARGE",null); }
    public static InputDraftException responseLarge() { return new InputDraftException(HttpStatus.UNPROCESSABLE_ENTITY,ResultCode.Error.BIZ_RULE_VIOLATION,"RESPONSE_TOO_LARGE",null); }
    public static InputDraftException internal() { return new InputDraftException(HttpStatus.INTERNAL_SERVER_ERROR,ResultCode.Error.SYS_INTERNAL_ERROR,null,null); }
}
