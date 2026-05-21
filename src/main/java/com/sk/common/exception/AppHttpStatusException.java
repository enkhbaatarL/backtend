package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppHttpStatusException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ResultCode.Error errorCode;
    private final String additionalMessage;

    private AppHttpStatusException(HttpStatus httpStatus, ResultCode.Error errorCode, String additionalMessage) {
        super(errorCode.getMessage());
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.additionalMessage = additionalMessage;
    }

    public static AppHttpStatusException of(HttpStatus httpStatus, ResultCode.Error errorCode, String additionalMessage) {
        return new AppHttpStatusException(httpStatus, errorCode, additionalMessage);
    }
}
