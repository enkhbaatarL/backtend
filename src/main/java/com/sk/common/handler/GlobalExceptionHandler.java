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
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    private static final String ERROR_LOG_FORMAT = "[{}] 에러 발생 - 상세: {}. 원인: {}";

    /**
     * 중앙 집중식 응답 생성 및 로깅
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
     * ObjectError 목록 (FieldError 포함)을 상세 문자열로 변환합니다.
     *
     * @param errors 에러 목록
     * @return 상세 에러 문자열 (쉼표로 구분)
     */
    private String extractValidationErrors(List<? extends ObjectError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return errors.stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    // 필드 에러: 필드명, 거부된 값, 메시지
                    return String.format("[%s: %s, %s: %s, %s: %s]",
                        "field", fieldError.getField(),
                        "rejectedValue", fieldError.getRejectedValue(),
                        "cause", error.getDefaultMessage());
                } else if (error != null) {
                    // 객체 에러 (클래스 레벨 등): 객체명(보통 파라미터명), 메시지
                    return String.format("[%s: %s, %s: %s]",
                        "object", error.getObjectName(),
                        "cause", error.getDefaultMessage());
                } else {
                    return "Unknown validation error";
                }
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * @param ex      MethodArgumentNotValidException (@RequestBody @Valid 실패 시)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = extractValidationErrors(ex.getBindingResult().getAllErrors());
        return createAndLogResponse("Request Body Validation Failed", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    /**
     * @param ex      HandlerMethodValidationException
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String details = ex.getAllErrors().stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    String fieldName = fieldError.getField();
                    Object rejectedValue = fieldError.getRejectedValue();
                    String defaultMessage = error.getDefaultMessage();
                    return String.format("[%s: %s, %s: %s, %s: %s]",
                        "field", fieldName,
                        "rejectedValue", rejectedValue,
                        "cause", defaultMessage);
                } else if (error instanceof ObjectError objectError) {
                    String objectName = objectError.getObjectName();
                    String defaultMessage = error.getDefaultMessage();
                    return String.format("[%s: %s, %s: %s]",
                        "object", objectName,
                        "cause", defaultMessage);
                } else {
                    // 다른 타입의 에러가 있을 경우 대비 (가능성은 낮음)
                    return error.toString();
                }
            })
            .collect(Collectors.joining(", "));
        return createAndLogResponse("Method Parameter Validation Failed", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    /**
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
     * @param ex      NoResourceFoundException (Spring Boot 3.0+ 에서 정적 리소스 등 못찾을 때)
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
     * @param ex      NoHandlerFoundException (DispatcherServlet 설정 필요 - application.properties: spring.mvc.throw-exception-if-no-handler-found=true, spring.web.resources.add-mappings=false)
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
     * @param ex      HttpMessageNotReadableException (JSON 파싱 실패 등)
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return ResponseEntity<Object>
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Throwable rootCause = ex.getRootCause();
        String details = "Request body is missing or cannot be parsed";
        if (rootCause != null) {
            details += ". Cause: " + rootCause.getMessage();
        }
        return createAndLogResponse("Malformed JSON Request", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_JSON, details);
    }

    /**
     * @param ex MethodArgumentTypeMismatchException (@PathVariable, @RequestParam 등 타입 불일치 시)
     * @return ResponseEntity<Object>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String details = String.format("Parameter '%s' should be of type '%s' but was '%s'", ex.getName(), requiredType, ex.getValue());
        return createAndLogResponse("Parameter Type Mismatch", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
    }

    /**
     * @param ex ConstraintViolationException (JSR-303 @PathVariable, @RequestParam @Valid 실패 시)
     * @return ResponseEntity<Object>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
            .map(violation -> String.format("[%s: %s, %s: %s]",
                StringUtils.substringAfterLast(violation.getPropertyPath().toString(), "."), // 필드명 추출 시도
                violation.getInvalidValue(),
                violation.getMessage()))
            .collect(Collectors.joining(", "));
        return createAndLogResponse("Constraint Violation", ex, BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER, details);
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
    public ResponseEntity<Object> handleRestClientErrors(Exception ex) {
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
    public ResponseEntity<Object> handleAppError(AppErrorException ex) {
        return createAndLogResponse("Client Error", ex, BAD_REQUEST, ex.getErrorCode(), ex.getAdditionalMessage());
    }

    @ExceptionHandler(SystemErrorException.class)
    public ResponseEntity<Object> handleSystemError(SystemErrorException ex) {
        return createAndLogResponse("System Error", ex, INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getAdditionalMessage());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception ex) {
        return createAndLogResponse("Unhandled Internal Error", ex, INTERNAL_SERVER_ERROR, ResultCode.Error.SYS_INTERNAL_ERROR, ex.getMessage());
    }

}
