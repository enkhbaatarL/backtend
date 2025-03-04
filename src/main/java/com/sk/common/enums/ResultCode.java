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
        INVALID_REQUEST("E400", "Required value is invalid."),
        UNAUTHORIZED("E401", "Unauthorized request."),
        PATH_NOT_FOUND("E404", "Invalid path. (No URL provided)"),
        METHOD_NOT_ALLOWED("E405", "Method not allowed."),
        INVALID_JSON_FORMAT("E410", "The JSON format is invalid."),
        DATA_NOT_FOUND("E450", "Data not found."),
        INVALID_PARAMETER("E451", "Invalid parameters."),
        TOKEN_EXPIRED("E472", "Token has expired."),
        INVALID_CREDENTIAL("E473", "Credential is invalid."),
        API_CALL_FAILED("E544", "Internal API call failed."),
        DB_CONNECTION_ERROR("E550", "Database connection error."),
        UNKNOWN("E599", "Unknown error.");

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

