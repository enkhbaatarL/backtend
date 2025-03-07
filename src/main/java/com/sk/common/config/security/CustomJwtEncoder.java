package com.sk.common.config.security;

import com.sk.common.utils.JwtProperties;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.shared.entity.master.Oauth2RegisteredClient;
import com.sk.domain.shared.repository.master.Oauth2RegisteredClientRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional
public class CustomJwtEncoder implements JwtEncoder {

    private final JwtUtil jwtUtil;
    private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;
    private final JwtProperties jwtProperties;

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
        JwtClaimsSet claims = parameters.getClaims();
        Map<String, Object> headers = parameters.getJwsHeader().getHeaders();

        String clientId = claims.getSubject();

        // OAuth2Client에서 tenant와 permissions 정보 조회
        Oauth2RegisteredClient client = oauth2RegisteredClientRepository.findByClientId(clientId)
            .orElseThrow(() -> new JwtEncodingException("Client not found"));

        // permissions todo 변경
        Set<String> permissions = new HashSet<>(List.of("REFINITIV_VIEW"));

        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry());

        String token = Jwts.builder()
            .subject(clientId)
            .claim("scope", client.getScopes())
            .claim("tenant", client.getTenant())
            .claim("permissions", permissions)  // 복사된 permissions 사용
            .issuedAt(new Date())
            .expiration(expiryDate)
            .signWith(jwtUtil.getKey())
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
