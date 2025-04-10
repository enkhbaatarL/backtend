package com.sk.common.config.security.jwt;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JwtUtil;
import com.sk.common.utils.Properties.JwtProperties;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomJwtEncoder implements JwtEncoder {

    private final JwtUtil jwtUtil;
    private final RegisteredClientRepository registeredClientRepository;
    private final JwtProperties jwtProperties;

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
        JwtClaimsSet claims = parameters.getClaims();
        Map<String, Object> headers = parameters.getJwsHeader().getHeaders();

        String clientId = claims.getSubject();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        RegisteredClient registeredClient;
        if (auth instanceof OAuth2ClientAuthenticationToken clientAuth && clientAuth.isAuthenticated()) {
            registeredClient = clientAuth.getRegisteredClient();
        } else {
            registeredClient = registeredClientRepository.findByClientId(clientId);
        }

        Optional.ofNullable(registeredClient).orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER));

        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry());

        String token = Jwts.builder()
            .header().add("typ", "JWT").and()
            .subject(clientId)
            .claim("companyNo", registeredClient.getClientSettings().getSetting("companyNo")) // 회사번호
            .claim("companyCd", registeredClient.getClientSettings().getSetting("companyCd")) // 회사코드
            .claim("role", registeredClient.getClientSettings().getSetting("role")) // 역할
            .issuedAt(new Date())
            .expiration(expiryDate)
            .signWith(jwtUtil.getSecretKey(), Jwts.SIG.HS256)
            .compact();

        return new Jwt(
            token,
            claims.getIssuedAt(),
            expiryDate.toInstant(),
            headers,
            claims.getClaims()
        );

    }
}
