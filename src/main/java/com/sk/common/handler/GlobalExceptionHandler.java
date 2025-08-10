package com.sk.common.handler;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_LOG_FORMAT = "[{}] 에러 발생 - 상세: {}. 원인: {}";

    private static final String PARAMETER_KEY = "parameter";
    private static final String FIELD_KEY = "field";
    private static final String REJECTED_VALUE_KEY = "rejectedValue";
    private static final String CAUSE_KEY = "cause";
    private static final String OBJECT_KEY = "object";

    /**
     * 중앙 집중식 응답 생성 및 로깅
     * 모든 예외 핸들러는 해당 메서드를 호출하여 일관된 형식의 응답과 로그를 생성합니다.
     *
     * @param type    로그 및 응답에 사용할 에러 타입 문자열
     * @param ex      발생한 예외 객체
     * @param status  HTTP 상태 코드
     * @param code    커스텀 결과 코드
     * @param details 응답 및 로그에 포함될 상세 정보 (null 가능)
     * @return ResponseEntity 객체
     */
    private ResponseEntity<Object> createAndLogResponse(String type, Exception ex, HttpStatus status, ResultCode.Error code, String details) {
        String detailMessage = StringUtils.defaultIfBlank(details, "N/A");

        String causeMessage = ex.getMessage() != null ? ex.getMessage() : "No specific cause message";
        log.error(ERROR_LOG_FORMAT, type, detailMessage, causeMessage, ex);

        MDCUtil.setValue(MDCUtil.RESPONSE_ERROR_MSG, String.format("%s(%s) - %s", code.getMessage(), code.getCode(), detailMessage));

        BaseResponse<Object> body = ObjectUtils.isEmpty(details) ? BaseResponseFactory.create(code) : BaseResponseFactory.createDetail(code, details);

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Spring의 `ObjectError` 목록(주로 유효성 검사 실패 시 발생)을 상세한 정보를 담은 하나의 문자열로 변환합니다.
     * FieldError의 경우 [필드명, 거부된 값, 원인 메시지] 형식으로, 일반 ObjectError의 경우 [객체명, 원인 메시지] 형식으로 만듭니다.
     *
     * @param errors FieldError 또는 ObjectError를 포함하는 목록
     * @return 파싱된 상세 에러 문자열
     */
    private String extractValidationErrors(List<? extends ObjectError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return errors.stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return String.format("[%s: %s, %s: %s, %s: %s]",
                        FIELD_KEY, fieldError.getField(),
                        REJECTED_VALUE_KEY, fieldError.getRejectedValue(),
                        CAUSE_KEY, error.getDefaultMessage());
                } else if (error != null) {
                    return String.format("[%s: %s, %s: %s]",
                        OBJECT_KEY, error.getObjectName(),
                        CAUSE_KEY, error.getDefaultMessage());
                } else {
                    return "Unknown validation error";
                }
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * 파라미터 유효성 검증 실패 시 발생하는 예외처리입니다.
     *
     * @param ex      MethodArgumentNotValidException (@RequestBody @Valid 실패 시)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = extractValidationErrors(ex.getBindingResult().getAllErrors());
        return createAndLogResponse("Request Body Validation Failed", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    /**
     * 메서드 파라미터 유효성 검증 실패 시 발생하는 예외처리입니다.
     *
     * @param ex      HandlerMethodValidationException (@RequestParam @Min(1) 실패 시)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = ex.getAllErrors().stream()
            .map(this::formatValidationError)
            .collect(Collectors.joining(", "));
        return createAndLogResponse("Method Parameter Validation Failed", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    /**
     * 유효성 검사 에러(MessageSourceResolvable)를 포맷에 맞는 문자열로 변환하는 헬퍼 메서드입니다.
     * FieldError, ObjectError 등 다양한 유효성 에러 타입을 처리합니다.
     *
     * @param error 변환할 유효성 검사 에러 객체
     * @return 포맷팅된 에러 문자열
     */
    private String formatValidationError(MessageSourceResolvable error) {
        if (error instanceof FieldError fieldError) {
            return String.format("[%s: %s, %s: %s, %s: %s]",
                FIELD_KEY, fieldError.getField(),
                REJECTED_VALUE_KEY, fieldError.getRejectedValue(),
                CAUSE_KEY, error.getDefaultMessage());
        }
        if (error instanceof ObjectError objectError) {
            return String.format("[%s: %s, %s: %s]",
                OBJECT_KEY, objectError.getObjectName(),
                CAUSE_KEY, objectError.getDefaultMessage());
        }
        if (error instanceof ParameterValidationResult paramError) {
            return String.format("[%s: %s, %s: %s, %s: %s]",
                PARAMETER_KEY, paramError.getMethodParameter().getParameterName(),
                REJECTED_VALUE_KEY, paramError.getArgument(),
                CAUSE_KEY, error.getDefaultMessage());
        }
        return String.format("[%s: %s]", CAUSE_KEY, error.getDefaultMessage());
    }

    /**
     * 파라미터 누락 시 발생하는 예외처리입니다.
     *
     * @param ex      MissingServletRequestParameterException (@RequestParam 누락 시)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Required parameter '%s' of type %s is missing", ex.getParameterName(), ex.getParameterType());
        return createAndLogResponse("Missing Request Parameter", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }


    /**
     * 지원하지 않는 HTTP 메소드 호출 시 발생하는 예외처리입니다.
     *
     * @param ex      HttpRequestMethodNotSupportedException (지원하지 않는 HTTP 메소드)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Method '%s' is not supported for this request. Supported methods are %s", ex.getMethod(), ex.getSupportedHttpMethods());
        return createAndLogResponse("Method Not Supported", ex, METHOD_NOT_ALLOWED, ResultCode.Error.REQ_METHOD_NOT_ALLOWED, details);
    }

    /**
     * 리소스가 존재하지 않을 때 발생하는 예외처리입니다. (Spring Framework 6+)
     *
     * @param ex      NoResourceFoundException
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return createAndLogResponse("Resource Not Found", ex, NOT_FOUND, ResultCode.Error.RSC_PATH_NOT_FOUND, ex.getResourcePath());
    }

    /**
     * 리소스 핸들러가 존재하지 않을 때 발생하는 예외처리입니다. (Spring Framework 6-)
     *
     * @param ex      NoHandlerFoundException
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return createAndLogResponse("Handler Not Found", ex, NOT_FOUND, ResultCode.Error.RSC_PATH_NOT_FOUND, ex.getRequestURL());
    }

    /**
     * HTTP 메시지 파싱 실패 시 발생하는 예외처리입니다.
     *
     * @param ex      HttpMessageNotReadableException (JSON 파싱 실패 등)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Throwable rootCause = ex.getRootCause();
        String details = "Request body is missing or cannot be parsed. Cause: %s".formatted(Optional.ofNullable(rootCause).map(Throwable::getMessage).orElse(null));
        return createAndLogResponse("Malformed JSON Request", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_JSON, details);
    }

    /**
     * 파일 업로드 크기 초과 시 발생하는 예외처리입니다.
     *
     * @param ex      MaxUploadSizeExceededException (파일 크기 초과 시)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return createAndLogResponse("Max Upload-Size Exceeded", ex, BAD_REQUEST, ResultCode.Error.FILE_SIZE_ERROR, ex.getMessage());
    }

    /**
     * 지원하지 않는 HTTP 파라미터 타입 호출 시 타입 불일치로 발생하는 예외처리입니다.
     *
     * @param ex      TypeMismatchException (@RequestParam 타입 불일치)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {
            String requiredType = mismatchEx.getRequiredType() != null ? mismatchEx.getRequiredType().getSimpleName() : "unknown";
            String details = String.format("Parameter '%s' should be of type '%s' but was '%s'", mismatchEx.getName(), requiredType, mismatchEx.getValue());
            return createAndLogResponse("Parameter Type Mismatch", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
        }

        String details = String.format("Failed to convert value of type '%s' to required type '%s'",
            ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null",
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return createAndLogResponse("Parameter Type Mismatch", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Content-Type '%s' is not supported. Supported media types are %s", ex.getContentType(), ex.getSupportedMediaTypes());
        return createAndLogResponse("Media Type Not Supported", ex, UNSUPPORTED_MEDIA_TYPE, ResultCode.Error.REQ_INVALID, details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Could not find acceptable representation. Supported media types are %s", ex.getSupportedMediaTypes());
        return createAndLogResponse("Media Type Not Acceptable", ex, NOT_ACCEPTABLE, ResultCode.Error.REQ_INVALID, details);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Required path variable '%s' is missing in request URI", ex.getVariableName());
        return createAndLogResponse("Missing Path Variable", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Required request part '%s' is not present", ex.getRequestPartName());
        return createAndLogResponse("Missing Request Part", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Error binding request parameters. %s", ex.getMessage());
        return createAndLogResponse("Request Binding Error", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return createAndLogResponse("Async Request Timeout", ex, SERVICE_UNAVAILABLE, ResultCode.Error.SYS_INTERNAL_ERROR, "Request processing timed out.");
    }

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(ErrorResponseException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return createAndLogResponse("Error Response Exception", ex, (HttpStatus) ex.getStatusCode(), ResultCode.Error.SYS_INTERNAL_ERROR, ex.getDetailMessageCode());
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Conversion of value '%s' to type '%s' is not supported.", ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return createAndLogResponse("Conversion Not Supported", ex, INTERNAL_SERVER_ERROR, ResultCode.Error.SYS_INTERNAL_ERROR, details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = String.format("Failed to write response. %s", ex.getMessage());
        return createAndLogResponse("Response Write Error", ex, INTERNAL_SERVER_ERROR, ResultCode.Error.SYS_INTERNAL_ERROR, details);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex, WebRequest request) {
        return createAndLogResponse("Async Request Not Usable", ex, SERVICE_UNAVAILABLE, ResultCode.Error.SYS_INTERNAL_ERROR, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return createAndLogResponse("Internal Server Error", ex, (HttpStatus) statusCode, ResultCode.Error.SYS_INTERNAL_ERROR, ex.getMessage());
    }

    @ExceptionHandler(CustomAuthenticationException.class)
    private ResponseEntity<Object> handleCustomAuthentication(CustomAuthenticationException ex) {
        String details = String.format("AuthComponent: %s, Message: %s", ex.getAuthComponent(), ex.getMessage());
        return createAndLogResponse("Authentication Failed", ex, UNAUTHORIZED, ex.getErrorCode(), details);
    }

    @ExceptionHandler(CustomAuthorizationException.class)
    private ResponseEntity<Object> handleCustomAuthorization(CustomAuthorizationException ex) {
        return createAndLogResponse("Authorization Failed", ex, FORBIDDEN, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class) // Spring Security의 기본 인가 예외
    private ResponseEntity<Object> handleSpringAuthorization(AuthorizationDeniedException ex) {
        return createAndLogResponse("Authorization Denied", ex, FORBIDDEN, ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, ex.getMessage());
    }

    @ExceptionHandler(SignatureException.class) // JWT 서명 검증 실패
    private ResponseEntity<Object> handleJwtSignature(SignatureException ex) {
        return createAndLogResponse("Invalid JWT Signature", ex, UNAUTHORIZED, ResultCode.Error.AUTH_TOKEN_INVALID, "Invalid JWT signature");
    }

    @ExceptionHandler(MalformedJwtException.class) // JWT 형식 오류
    private ResponseEntity<Object> handleMalformedJwt(MalformedJwtException ex) {
        return createAndLogResponse("Malformed JWT", ex, BAD_REQUEST, ResultCode.Error.AUTH_TOKEN_INVALID, "Malformed JWT format");
    }

    @ExceptionHandler({RestClientException.class, ResourceAccessException.class})
    private ResponseEntity<Object> handleRestClientErrors(Exception ex) {
        String type = "API Call Failed";
        ResultCode.Error code = ResultCode.Error.API_CALL_FAILED;
        String message = ex.getMessage();

        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof SocketTimeoutException) {
            type = "API Timeout";
            code = ResultCode.Error.API_TIMEOUT;
            message = "Timeout occurred while calling external API. " + rootCause.getMessage();
        } else {
            message = "Error occurred while calling external API. " + ex.getMessage();
        }

        return createAndLogResponse(type, ex, INTERNAL_SERVER_ERROR, code, message);
    }

    @ExceptionHandler(AppErrorException.class)
    private ResponseEntity<Object> handleAppError(AppErrorException ex) {
        return createAndLogResponse("Client Error", ex, BAD_REQUEST, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(SystemErrorException.class)
    private ResponseEntity<Object> handleSystemError(SystemErrorException ex) {
        return createAndLogResponse("System Error", ex, INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<Object> handleUncaughtException(Exception ex) {
        return createAndLogResponse("Unhandled Internal Error", ex, INTERNAL_SERVER_ERROR, ResultCode.Error.SYS_INTERNAL_ERROR, ex.getMessage());
    }

}
