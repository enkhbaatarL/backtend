package com.sk.common.config.security;

import com.sk.common.utils.JwtProperties;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.shared.entity.master.Oauth2RegisteredClient;
import com.sk.domain.shared.repository.master.Oauth2RegisteredClientRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry());

        String token = Jwts.builder()
            .subject(clientId)
            .claim("scope", registeredClient.getScopes())
            .claim("tenant", registeredClient.getClientSettings().getSetting("tenant"))
            .claim("role", registeredClient.getClientSettings().getSetting("role"))
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
