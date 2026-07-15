package com.sk.common.config.security;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

// 인증 실패 처리
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public CustomAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * 인증 실패 응답을 이 자리에서 즉시 생성
     * 예외를 던져 필터 체인 밖(ExceptionHandlerFilter)에서 처리하게 하면 응답 로깅과 MDC 정리가 끝난 뒤에야 에러 바디가 쓰여 res 로그의 body가 비고 uuid가 유실되며 에러 로그도 중복된다.
     * resolver를 직접 호출해 GlobalExceptionHandler가 같은 포맷의 응답을 이 시점에 만들도록 한다.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {

        AuthComponent authComponent = AuthComponent.determineAuthComponent(request);
        ResultCode.Error resultCode = CustomAuthenticationException.mapAuthExceptionToResultCode(authException);

        handlerExceptionResolver.resolveException(request, response, null, CustomAuthenticationException.of(authComponent, resultCode, authException.getMessage(), authException));

    }

}
