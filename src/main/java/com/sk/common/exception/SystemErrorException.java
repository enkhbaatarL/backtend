package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class SystemErrorException extends RuntimeException {

    private final ResultCode.Error errorCode;
    private final String additionalMessage;

    private SystemErrorException(ResultCode.Error errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = null;
    }

    private SystemErrorException(ResultCode.Error errorCode, String additionalMessage, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = additionalMessage;
    }

    public static SystemErrorException of(ResultCode.Error errorCode) {
        return new SystemErrorException(errorCode, null);
    }

    public static SystemErrorException of(ResultCode.Error errorCode, String additionalMessage) {
        return new SystemErrorException(errorCode, additionalMessage, null);
    }

}
