package com.sk.common.handler;

import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponseFactory;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
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

    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String causeStr = fieldErrors
            .stream()
            .map(s -> String.format("[%s: %s, %s: %s, %s: %s]", "field", s.getField(), "provided value", s.getRejectedValue(), "cause", s.getDefaultMessage()))
            .collect(Collectors.joining(", "));
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", e.getMessage(), e);
        return ResponseEntity.status(BAD_REQUEST)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.INVALID_REQUEST, causeStr));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(METHOD_NOT_ALLOWED)
            .body(BaseResponseFactory.create(ResultCode.Error.METHOD_NOT_ALLOWED));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity
            .status(NOT_FOUND)
            .body(BaseResponseFactory.create(ResultCode.Error.PATH_NOT_FOUND));
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(NOT_FOUND)
            .body(BaseResponseFactory.create(ResultCode.Error.PATH_NOT_FOUND));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(BaseResponseFactory.create(ResultCode.Error.INVALID_JSON_FORMAT));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(BaseResponseFactory.create(ResultCode.Error.UNKNOWN));
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    private ResponseEntity<Object> handleServletException(AuthenticationServiceException e) {
        return ResponseEntity
            .status(UNAUTHORIZED)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.INVALID_CREDENTIAL, e.getMessage()));
    }

    @ExceptionHandler(SignatureException.class)
    private ResponseEntity<Object> handleServletException(SignatureException e) {
        return ResponseEntity
            .status(UNAUTHORIZED)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.INVALID_CREDENTIAL, e.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Object> handleRestClientException(RestClientException ex) {
        if (ex.getCause() instanceof SocketTimeoutException) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(BaseResponseFactory.createDetail(ResultCode.Error.API_CALL_FAILED, ex.getCause().getMessage()));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.API_CALL_FAILED, ex.getMessage()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Object> handleException(ResourceAccessException ex) {
        if (ex.getCause() instanceof SocketTimeoutException) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(BaseResponseFactory.createDetail(ResultCode.Error.API_CALL_FAILED, ex.getCause().getMessage()));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.API_CALL_FAILED, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error("Exception Occurred. -> [내부 에러 발생(Exception)]. message : {}", e.getMessage(), e);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(BaseResponseFactory.create(ResultCode.Error.UNKNOWN));
    }

}
