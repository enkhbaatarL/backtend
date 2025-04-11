package com.sk.common.enums;

import lombok.Getter;

import java.util.Arrays;

// @formatter:off
public class ResultCode {

    @Getter
    public enum Success {
        OK("SUC-000", "Success.");

        private final String code;
        private final String message;

        Success(String code, String message) {
            this.code = code;
            this.message = message;
        }

    }

    @Getter
    public enum Error {

        // AUTH: 인증/인가 관련 오류
        AUTH_REQUIRED("AUTH-001", "Authentication is required for this request."),
        AUTH_INVALID_CREDENTIALS("AUTH-002", "The provided credentials are incorrect."),
        AUTH_TOKEN_EXPIRED("AUTH-003", "Token has expired."),
        AUTH_TOKEN_INVALID("AUTH-004", "The provided token is invalid or malformed."),
        AUTH_INSUFFICIENT_PERMISSIONS("AUTH-005", "You don't have sufficient permissions for this operation."),
        AUTH_ACCOUNT_LOCKED("AUTH-006", "Your account has been locked. Please contact support."),
        AUTH_ACCESS_DENIED("AUTH-007", "Access denied. You do not have permission to access this resource."),

        // REQ: 요청/입력 관련 오류
        REQ_INVALID("REQ-001", "The request is invalid."),
        REQ_MISSING_PARAMETER("REQ-002", "Required parameter is missing."),
        REQ_INVALID_PARAMETER("REQ-003", "One or more parameters are invalid."),
        REQ_INVALID_FORMAT("REQ-004", "The request format is incorrect."),
        REQ_INVALID_JSON("REQ-005", "The provided JSON format is incorrect."),
        REQ_METHOD_NOT_ALLOWED("REQ-006", "The HTTP method used is not allowed for this request."),

        // RSC: 리소스 관련 오류
        RSC_NOT_FOUND("RSC-001", "The requested resource could not be found."),
        RSC_PATH_NOT_FOUND("RSC-002", "The requested path is invalid or missing."),
        RSC_ALREADY_EXISTS("RSC-003", "The resource already exists."),
        RSC_DATA_NOT_FOUND("RSC-004", "The requested data could not be found."),
        RSC_COMPONENT_NOT_FOUND("RSC-005", "The required system component could not be found."),

        // BIZ: 비즈니스 로직 관련 오류
        BIZ_RULE_VIOLATION("BIZ-001", "The operation violates a business rule."),
        BIZ_DATA_INTEGRITY("BIZ-002", "The operation would violate data integrity."),
        BIZ_STATE_CONFLICT("BIZ-003", "The operation cannot be performed in the current state."),
        BIZ_VALIDATION_FAILED("BIZ-004", "Business validation failed."),

        // API: 외부 시스템 및 서비스 관련 오류
        API_CALL_FAILED("API-001", "An external API call has failed."),
        API_TIMEOUT("API-002", "The external API call timed out."),
        API_SERVICE_UNAVAILABLE("API-003", "An external service is currently unavailable."),
        API_RESPONSE_ERROR("API-004", "Received an error response from external API."),

        // DB: 데이터베이스 관련 오류
        DB_CONNECTION_ERROR("DB-001", "A database connection error has occurred."),
        DB_QUERY_ERROR("DB-002", "An error occurred while executing the database query."),
        DB_TRANSACTION_FAILED("DB-003", "Database transaction failed."),

        // SYS: 서버 및 시스템 관련 오류
        SYS_INTERNAL_ERROR("SYS-001", "An internal server error has occurred."),
        SYS_MAINTENANCE("SYS-002", "The system is currently under maintenance."),
        SYS_UNKNOWN("SYS-999", "An unknown error has occurred. Please try again later."),

        // FILE: 파일 관련 오류
        FILE_INVALID("FILE-001", "The file is invalid."),
        FILE_FORMAT_ERROR("REQ-007", "This file format is not allowed."),
        FILE_SIZE_ERROR("REQ-008", "File size exceeds the maximum limit"),

        // SEC: 보안 및 암복호화 관련 오류
        SEC_ENCRYPTION_FAILED("SEC-001", "Failed to encrypt data using AES."),
        SEC_DECRYPTION_FAILED("SEC-002", "Failed to decrypt data using AES."),
        SEC_INVALID_KEY("SEC-003", "The provided AES secret key is invalid.");


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
// @formatter:on
