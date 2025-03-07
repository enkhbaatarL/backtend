package com.sk.common.config.security.oauth2;

import com.sk.domain.mi.dto.response.OAuth2Res;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Component
public class CustomAccessTokenResponseConverter implements Converter<OAuth2AccessTokenAuthenticationToken, OAuth2Res> {

    @Override
    public OAuth2Res convert(OAuth2AccessTokenAuthenticationToken authentication) {

        OAuth2AccessToken accessToken = authentication.getAccessToken();

        String tokenValue = accessToken.getTokenValue();
        String tokenType = accessToken.getTokenType().getValue();
        Instant expiresAt = accessToken.getExpiresAt();
        Instant issuedAt = accessToken.getIssuedAt();
        // todo 필요 시 추가
//        Set<String> scopes = accessToken.getScopes();

        // expiresInSeconds 변환
        long expiresIn = Duration.between(Instant.now(), expiresAt).getSeconds();
        String issuedAtFormatted = ObjectUtils.isNotEmpty(issuedAt) ? DateTimeFormatter.ISO_INSTANT.format(issuedAt) : null;

        return OAuth2Res.of(tokenValue, tokenType, expiresIn, issuedAtFormatted);

    }
}
