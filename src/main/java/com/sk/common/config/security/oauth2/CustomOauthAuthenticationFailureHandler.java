package com.sk.common.config.security.oauth2;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomOauthAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {

        String errorMessage;

        if (exception instanceof OAuth2AuthenticationException oauth2Ex) {
            String errorCode = oauth2Ex.getError().getErrorCode();
            String description = oauth2Ex.getError().getDescription();

            // OAuth2 오류 코드에 따른 ResultCode 매핑
            ResultCode.Error resultCode = CustomAuthenticationException.mapAuthExceptionToResultCode(exception);

            throw  CustomAuthenticationException.of(AuthComponent.OAUTH2_SERVER, resultCode, ObjectUtils.isNotEmpty(description) ? description : errorCode, exception);
        } else {
            throw  CustomAuthenticationException.of(AuthComponent.GENERIC_AUTH, ResultCode.Error.AUTH_TOKEN_INVALID, exception.getMessage(), exception);
        }

    }

}
