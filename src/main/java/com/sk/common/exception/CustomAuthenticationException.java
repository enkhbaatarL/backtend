package com.sk.common.exception;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.Getter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.security.SignatureException;

/**
 * 커스텀 인증 Exception Class
 */
@Getter
public class CustomAuthenticationException extends RuntimeException {

    private final AuthComponent authComponent;
    private final ResultCode.Error errorCode;
    private final String errorDescription;

    private CustomAuthenticationException(AuthComponent authComponent, ResultCode.Error errorCode, String errorDescription) {
        super(errorDescription);
        this.authComponent = authComponent;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    private CustomAuthenticationException(AuthComponent authComponent, ResultCode.Error errorCode, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.authComponent = authComponent;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    // 기본 팩토리 메소드 - 원인 예외 없음
    public static CustomAuthenticationException of(AuthComponent authComponent, ResultCode.Error errorCode) {
        return new CustomAuthenticationException(authComponent, errorCode, errorCode.getMessage());
    }

    // 사용자 정의 메시지가 있는 팩토리 메소드
    public static CustomAuthenticationException of(AuthComponent authComponent, ResultCode.Error errorCode, String errorDescription) {
        return new CustomAuthenticationException(authComponent, errorCode, errorDescription);
    }

    // 원인 예외가 있는 팩토리 메소드
    public static CustomAuthenticationException of(AuthComponent authComponent, ResultCode.Error errorCode, Throwable cause) {
        return new CustomAuthenticationException(authComponent, errorCode, errorCode.getMessage(), cause);
    }

    // 사용자 정의 메시지와 원인 예외가 모두 있는 팩토리 메소드
    public static CustomAuthenticationException of(AuthComponent authComponent, ResultCode.Error errorCode, String errorDescription, Throwable cause) {
        return new CustomAuthenticationException(authComponent, errorCode, errorDescription, cause);
    }

    public static ResultCode.Error mapAuthExceptionToResultCode(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return ResultCode.Error.AUTH_INVALID_CREDENTIALS;
        } else if (exception instanceof UsernameNotFoundException) {
            return ResultCode.Error.RSC_DATA_NOT_FOUND;
        } else if (exception instanceof LockedException) {
            return ResultCode.Error.AUTH_ACCOUNT_LOCKED;
        } else if (exception instanceof DisabledException) {
            return ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS;
        } else if (exception instanceof AccountExpiredException) {
            return ResultCode.Error.AUTH_ACCOUNT_LOCKED;
        } else if (exception instanceof CredentialsExpiredException) {
            return ResultCode.Error.AUTH_TOKEN_EXPIRED;
        } else if (exception instanceof InsufficientAuthenticationException) {
            return ResultCode.Error.AUTH_REQUIRED;
        } else if (exception instanceof OAuth2AuthenticationException oauth2Ex) {
            String errorCode = oauth2Ex.getError().getErrorCode();
            return switch (errorCode) {
                case "invalid_client", "invalid_grant", "invalid_request" -> ResultCode.Error.AUTH_INVALID_CREDENTIALS;
                case "invalid_scope", "unauthorized_client", "access_denied" -> ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS;
                case "unsupported_grant_type" -> ResultCode.Error.REQ_INVALID_PARAMETER;
                default -> ResultCode.Error.SYS_UNKNOWN;
            };
        } else if (exception.getCause() instanceof ExpiredJwtException) {
            return ResultCode.Error.AUTH_TOKEN_EXPIRED;
        } else if (exception.getCause() instanceof SignatureException || exception.getCause() instanceof MalformedJwtException) {
            return ResultCode.Error.AUTH_TOKEN_INVALID;
        } else {
            return ResultCode.Error.AUTH_REQUIRED;
        }
    }

}
