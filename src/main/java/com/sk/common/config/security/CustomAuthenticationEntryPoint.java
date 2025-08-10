package com.sk.common.config.security;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

// 인증 실패 처리
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {

        AuthComponent authComponent = AuthComponent.determineAuthComponent(request);
        ResultCode.Error resultCode = CustomAuthenticationException.mapAuthExceptionToResultCode(authException);

        throw CustomAuthenticationException.of(authComponent, resultCode, authException.getMessage(), authException);

    }

}
