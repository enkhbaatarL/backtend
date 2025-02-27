package com.sk.mi.security;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sk.mi.util.JwtProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sk.mi.entity.master.OAuth2Client;
import com.sk.mi.repository.master.OAuth2ClientRepository;
import com.sk.mi.util.JwtUtil;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class CustomJwtEncoder implements JwtEncoder {
    private final JwtUtil jwtUtil;
    private final OAuth2ClientRepository oauth2ClientRepository;
    private final JwtProperties jwtProperties;

    @Override
    public Jwt encode(JwtEncoderParameters parameters) throws JwtEncodingException {
        JwtClaimsSet claims = parameters.getClaims();
        Map<String, Object> headers = parameters.getJwsHeader().getHeaders();

        // scope를 Collection으로 가져와서 문자열로 변환
        Collection<String> scopes = claims.getClaim("scope");
        String scope = String.join(" ", scopes);
        String clientId = claims.getSubject();
        
        // OAuth2Client에서 tenant와 permissions 정보 조회
        OAuth2Client client = oauth2ClientRepository.findByClientId(clientId)
            .orElseThrow(() -> new JwtEncodingException("Client not found"));

        // permissions를 새로운 Set으로 복사 (지연 로딩 문제 해결)
        Set<String> permissions = new HashSet<>(client.getPermissions());

        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry());
        
        String token = Jwts.builder()
            .subject(clientId)
            .claim("scope", scope)
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