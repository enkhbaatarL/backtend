package com.sk.common.config.security.jwt;

import com.sk.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
        return new Jwt(token, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), Map.of("typ", "JWT", "alg", Jwts.SIG.HS256), claims);
    }

}
