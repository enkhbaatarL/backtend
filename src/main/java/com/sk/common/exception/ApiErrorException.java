package com.sk.common.exception;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

/**
 * 애플리케이션 내부에서 API 호출하고 예외가 발생 했을 때, 사용되는 에러 예외 클래스
 *  - HTTP Status Code: 500
 *  - statusCode: 외부 API가 응답한 HTTP status (통신 자체가 실패해 응답이 없으면 0)
 *  - errorCode: 오류 분류 (API_TIMEOUT/API_SERVICE_UNAVAILABLE/API_CALL_FAILED 등, 미지정이면 null —
 *    GlobalExceptionHandler가 API_RESPONSE_ERROR로 응답)
 */
@Getter
public class ApiErrorException extends RuntimeException {

    private final int statusCode;
    private final String message;
    private final ResultCode.Error errorCode;

    private ApiErrorException(ResultCode.Error errorEnum, Throwable e) {
        super(errorEnum.getMessage(), e);
        this.statusCode = 0;
        this.message = errorEnum.getMessage();
        this.errorCode = errorEnum;
    }

    private ApiErrorException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.message = message;
        this.errorCode = null;
    }

    public static ApiErrorException of(int statusCode, String message) {
        return new ApiErrorException(statusCode, message, null);
    }

    public static ApiErrorException of(ResultCode.Error errorEnum) {
        return new ApiErrorException(errorEnum, null);
    }

    public static ApiErrorException of(ResultCode.Error errorEnum, Throwable e) {
        return new ApiErrorException(errorEnum, e);
    }

    public static ApiErrorException of(int statusCode, String message, Throwable e) {
        return new ApiErrorException(statusCode, message, e);
    }

}
