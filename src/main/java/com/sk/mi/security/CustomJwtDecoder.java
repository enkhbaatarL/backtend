package com.sk.mi.security;

import com.sk.mi.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    private final JwtUtil jwtUtil;

    @Override
    public Jwt decode(String token) throws JwtException {
        Claims claims = jwtUtil.extractClaims(token);
        // tenant와 permissions 정보가 포함된 claims 처리
        return new Jwt(token, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), Map.of("alg", "HS256"), claims);
    }
}
