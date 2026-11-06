package com.sk.domain.mi.controller.nextgen;

import com.sk.common.enums.ResultCode;
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

/** 검증 API의 422 계약만 적용하며 다른 API의 기존 400 정책은 유지한다. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EventVerificationController.class)
public class EventVerificationExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<BaseResponse<Void>> invalidInput(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(BaseResponseFactory.createDetail(ResultCode.Error.REQ_INVALID_PARAMETER,
                "miEventIds에 중복 없는 양의 정수 1~5개만 전달해 주세요. 추가 필드는 허용하지 않습니다."));
    }
}
