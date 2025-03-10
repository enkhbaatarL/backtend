package com.sk.common.enums;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public enum AuthComponent {

    OAUTH2_SERVER,
    OAUTH2_RESOURCE,
    SAML_SERVICE_PROVIDER,
    TOKEN_VALIDATION,
    GENERIC_AUTH;

    public static AuthComponent determineAuthComponent(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/oauth2/token")) {
            return AuthComponent.OAUTH2_SERVER;
        } else if (uri.startsWith("/api/")) {
            return AuthComponent.OAUTH2_RESOURCE;
        } else if (uri.startsWith("/saml/")) {
            return AuthComponent.SAML_SERVICE_PROVIDER;
        } else {
            return AuthComponent.GENERIC_AUTH;
        }
    }

}
