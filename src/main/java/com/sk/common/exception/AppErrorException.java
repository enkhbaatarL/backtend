package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class AppErrorException extends RuntimeException {

    private final ResultCode.Error errorCode;
    private final String additionalMessage;

    private AppErrorException(ResultCode.Error errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = null;
    }

    private AppErrorException(ResultCode.Error errorCode, String additionalMessage, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.additionalMessage = additionalMessage;
    }

    public static AppErrorException of(ResultCode.Error errorCode) {
        return new AppErrorException(errorCode, null);
    }

    public static AppErrorException of(ResultCode.Error errorCode, String additionalMessage) {
        return new AppErrorException(errorCode, additionalMessage, null);
    }

}
