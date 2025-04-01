package com.sk.common.handler;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.MDCUtil;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 예외 핸들러 공통처리 메소드
     */
    private ResponseEntity<Object> buildResponse(Exception ex, HttpStatus status, ResultCode.Error code, String additional) {
        MDCUtil.setValue(MDCUtil.RESPONSE_ERROR_MSG, ex.getMessage());

        BaseResponse<Object> body = ObjectUtils.isEmpty(additional) ?
            BaseResponseFactory.create(code) : BaseResponseFactory.createDetail(code, additional);

        return ResponseEntity
            .status(status)
            .body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String causeStr = fieldErrors
            .stream()
            .map(s -> String.format("[%s: %s, %s: %s, %s: %s]", "field", s.getField(), "provided value", s.getRejectedValue(), "cause", s.getDefaultMessage()))
            .collect(Collectors.joining(", "));
        log.error("Exception Occurred. -> [Request Body Parameters Not Valid]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, causeStr);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String causeStr = ex.getValueResults()
            .stream()
            .flatMap(result -> {
                String fieldName = result.getMethodParameter().getParameterName();
                Object rejectedValue = result.getArgument();
                return result.getResolvableErrors().stream()
                    .map(error -> String.format("[%s: %s, %s: %s, %s: %s]", "field", fieldName, "provided value", rejectedValue, "cause", error.getDefaultMessage()));
            })
            .collect(Collectors.joining(", "));
        log.error("Exception Occurred. -> [Parameters Not Valid]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, causeStr);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, METHOD_NOT_ALLOWED, ResultCode.Error.REQ_METHOD_NOT_ALLOWED, null);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [리소스 경로 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, NOT_FOUND, ResultCode.Error.RSC_PATH_NOT_FOUND, null);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [리소스 경로 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, NOT_FOUND, ResultCode.Error.RSC_PATH_NOT_FOUND, null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [JSON 파싱 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_JSON, null);
    }

    @ExceptionHandler(CustomAuthenticationException.class)
    private ResponseEntity<Object> handleServletException(CustomAuthenticationException ex) {
        log.error("Exception Occurred. -> [인증 에러 발생]. authComponent : [{}], message : [{}]", ex.getAuthComponent(), ex.getMessage(), ex);
        return buildResponse(ex, UNAUTHORIZED, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(CustomAuthorizationException.class)
    private ResponseEntity<Object> handleServletException(CustomAuthorizationException ex) {
        log.error("Exception Occurred. -> [인가 에러 발생], message : [{}]", ex.getMessage(), ex);
        return buildResponse(ex, FORBIDDEN, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    private ResponseEntity<Object> handleServletException(AuthorizationDeniedException ex) {
        log.error("Exception Occurred. -> [인가 에러 발생]. authComponent : [{}], message : [{}]", AuthComponent.OAUTH2_RESOURCE, ex.getMessage(), ex);
        return buildResponse(ex, FORBIDDEN, ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, ex.getMessage());
    }

    @ExceptionHandler(SignatureException.class)
    private ResponseEntity<Object> handleServletException(SignatureException ex) {
        log.error("Exception Occurred. -> [토큰 에러 발생]. authComponent : [{}], message : [{}]", AuthComponent.OAUTH2_RESOURCE, ex.getMessage(), ex);
        return buildResponse(ex, UNAUTHORIZED, ResultCode.Error.AUTH_TOKEN_INVALID, ex.getMessage());
    }

    @ExceptionHandler(MalformedJwtException.class)
    private ResponseEntity<Object> handleServletException(MalformedJwtException ex) {
        log.error("Exception Occurred. -> [토큰 에러 발생]. authComponent : [{}], message : [{}]", AuthComponent.OAUTH2_RESOURCE, ex.getMessage(), ex);
        return buildResponse(ex, BAD_REQUEST, ResultCode.Error.AUTH_TOKEN_INVALID, ex.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Object> handleRestClientException(RestClientException ex) {
        if (ex.getCause() instanceof SocketTimeoutException) {
            return buildResponse(ex, INTERNAL_SERVER_ERROR, ResultCode.Error.API_TIMEOUT, ex.getCause().getMessage());
        }
        return buildResponse(ex, INTERNAL_SERVER_ERROR, ResultCode.Error.API_CALL_FAILED, ex.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Object> handleException(ResourceAccessException ex) {
        if (ex.getCause() instanceof SocketTimeoutException) {
            return buildResponse(ex, INTERNAL_SERVER_ERROR, ResultCode.Error.API_TIMEOUT, ex.getCause().getMessage());
        }
        return buildResponse(ex, INTERNAL_SERVER_ERROR, ResultCode.Error.API_CALL_FAILED, ex.getMessage());
    }

    @ExceptionHandler(AppErrorException.class)
    public ResponseEntity<Object> handleException(AppErrorException ex) {
        log.error("Exception Occurred. -> [클라이언트 요청 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, BAD_REQUEST, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(SystemErrorException.class)
    public ResponseEntity<Object> handleException(SystemErrorException ex) {
        log.error("Exception Occurred. -> [시스템 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return buildResponse(ex, INTERNAL_SERVER_ERROR, ResultCode.Error.SYS_INTERNAL_ERROR, null);
    }

}
