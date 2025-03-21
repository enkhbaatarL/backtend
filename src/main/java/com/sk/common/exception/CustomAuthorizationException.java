package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class CustomAuthorizationException extends RuntimeException {

    private final ResultCode.Error errorCode;
    private final String additionalMessage;

    private CustomAuthorizationException(ResultCode.Error errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = null;
    }

    private CustomAuthorizationException(ResultCode.Error errorCode, String additionalMessage, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = additionalMessage;
    }

    public static CustomAuthorizationException of(ResultCode.Error errorCode) {
        return new CustomAuthorizationException(errorCode, null);
    }

    public static CustomAuthorizationException of(ResultCode.Error errorCode, String additionalMessage) {
        return new CustomAuthorizationException(errorCode, additionalMessage, null);
    }

}
