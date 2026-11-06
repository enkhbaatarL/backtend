package com.sk.domain.mi.controller.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.EventVerificationConflictException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EventVerificationUpdateController.class)
public class EventVerificationUpdateExceptionHandler {
    @ExceptionHandler(EventVerificationConflictException.class)
    public ResponseEntity<BaseResponse<EventVerificationConflictException.Data>> conflict(EventVerificationConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(BaseResponseFactory.create(exception.getErrorCode(), exception.getData()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<BaseResponse<Void>> invalidInput(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.REQ_INVALID_PARAMETER,
                "양의 정수 Event ID, 필수 UUID verificationId와 선택적 ISO 날짜·시간 문자열 expectedUpdatedAt을 전달해 주세요. 사용자 ID·수정 내용은 입력할 수 없습니다."));
    }
}
