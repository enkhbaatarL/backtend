package com.sk.common.config.security.saml;

import java.io.IOException;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomSamlAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        log.error("SAML 인증 실패 : {}", exception.getMessage());

        if (exception instanceof Saml2AuthenticationException saml2Ex) {
            String errorCode = saml2Ex.getSaml2Error().getErrorCode();
            String description = saml2Ex.getSaml2Error().getDescription();

            ResultCode.Error resultCode = CustomAuthenticationException.mapAuthExceptionToResultCode(exception);
            throw CustomAuthenticationException.of(AuthComponent.SAML_SERVICE_PROVIDER, resultCode,
                    ObjectUtils.isNotEmpty(description) ? description : errorCode, exception);
        } else {
            throw CustomAuthenticationException.of(AuthComponent.SAML_SERVICE_PROVIDER, ResultCode.Error.AUTH_REQUIRED,
                    exception.getMessage(), exception);
        }
    }
}