//package com.sk.common.config.oauth;
//
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
//import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
//
//    @Override
//    public void customize(JwtEncodingContext context) {
//        OAuth2Authorization authorization = context.getAuthorization();
//        OAuth2AuthorizationRequest authorizationRequest = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
//        RegisteredClient registeredClient = context.getRegisteredClient();
//
//        // ✅ 추가할 클레임 설정
//        context.getClaims().claim("client_id", registeredClient.getClientId());
//        context.getClaims().claim("tenant", registeredClient.getClientSettings().getSetting("tenant"));
//        context.getClaims().claim("permissions", registeredClient.getClientSettings().getSetting("permissions"));
//        context.getClaims().claim("scope", registeredClient.getScopes()); // ✅ Scope 추가
//
//        // ✅ Security Context에서 사용자 정보 추가 (client_credentials는 사용자 정보 없음)
//        if (authorization.getPrincipalName() != null) {
//            context.getClaims().claim("sub", authorization.getPrincipalName());
//        }
//    }
//}
//
