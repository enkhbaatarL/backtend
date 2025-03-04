package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class ApiErrorException extends RuntimeException {

    private final int statusCode;
    private final String message;

    private ApiErrorException(ResultCode.Error errorEnum, Throwable e) {
        super(errorEnum.getMessage(), e);
        this.statusCode = Integer.parseInt(errorEnum.getCode());
        this.message = errorEnum.getMessage();
    }

    private ApiErrorException(ResultCode.Error errorEnum) {
        super(errorEnum.getMessage(), null);
        this.statusCode = Integer.parseInt(errorEnum.getCode());
        this.message = errorEnum.getMessage();
    }

    private ApiErrorException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.message = message;
    }

    public static ApiErrorException of(int statusCode, String message) {
        return new ApiErrorException(statusCode, message, null);
    }

    public static ApiErrorException of(ResultCode.Error errorEnum) {
        return new ApiErrorException(errorEnum);
    }

    public static ApiErrorException of(ResultCode.Error errorEnum, Throwable e) {
        return new ApiErrorException(errorEnum, e);
    }

    public static ApiErrorException of(int statusCode, String message, Throwable e) {
        return new ApiErrorException(statusCode, message, e);
    }

}
