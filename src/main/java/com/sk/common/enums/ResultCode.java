package com.sk.common.enums;

import lombok.Getter;

import java.util.Arrays;

public class ResultCode {

    @Getter
    public enum Success {
        OK("S200", "Success.");

        private final String code;
        private final String message;

        Success(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    @Getter
    public enum Error {
        INVALID_REQUEST("E400", "The required value is invalid."),
        UNAUTHORIZED("E401", "You are not authorized to make this request."),
        PATH_NOT_FOUND("E404", "The requested path is invalid or missing."),
        METHOD_NOT_ALLOWED("E405", "The HTTP method used is not allowed for this request."),
        INVALID_JSON_FORMAT("E410", "The provided JSON format is incorrect."),
        DATA_NOT_FOUND("E450", "The requested data could not be found."),
        INVALID_PARAMETER("E451", "One or more parameters are invalid."),
        TOKEN_EXPIRED("E472", "Your token has expired. Please log in again."),
        INVALID_CREDENTIAL("E473", "The provided credentials are incorrect. Please try again."),
        API_CALL_FAILED("E544", "An internal API call has failed. Please try again later."),
        DB_CONNECTION_ERROR("E550", "A database connection error has occurred. Please contact support if the issue persists."),
        UNKNOWN("E599", "An unknown error has occurred. Please try again later.");

        private final String code;
        private final String message;

        Error(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    public static boolean isSuccess(String code) {
        return Arrays.stream(Success.values())
            .anyMatch(success -> success.getCode().equals(code));
    }

}

