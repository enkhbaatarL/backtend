package com.sk.common.enums;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public enum AuthComponent {

    OAUTH2_SERVER,
    OAUTH2_RESOURCE,
    SAML_SERVICE_PROVIDER,
    TOKEN_VALIDATION,
    GENERIC_AUTH,
    EXTERNAL_LOGIN;

    public static AuthComponent determineAuthComponent(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/v1/oauth2/token")) {
            return AuthComponent.OAUTH2_SERVER;
        } else if (uri.startsWith("/api/v1/saml2")) {
            return AuthComponent.SAML_SERVICE_PROVIDER;
        } else if (uri.startsWith("/api/v1/external")) {  // External 로그인 경로 추가
            return AuthComponent.EXTERNAL_LOGIN;
        } else if (uri.startsWith("/api/")) {
            return AuthComponent.OAUTH2_RESOURCE;
        } else {
            return AuthComponent.GENERIC_AUTH;
        }
    }

}
