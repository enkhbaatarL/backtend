package com.sk.common.utils;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.domain.shared.entity.master.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private SecretKey secretKey;
    private final JwtProperties jwtProperties;

    // 비밀 키 초기화
    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }

    // 공통 토큰 빌더 생성 메서드
    private JwtBuilder createTokenBuilder(String subject) {
        return Jwts.builder()
            .subject(subject)
            .issuedAt(new Date())
            .signWith(secretKey, Jwts.SIG.HS256); // 명시적으로 HS256 지정
    }

    // 액세스 토큰 생성
    public String generateAccessToken(User user) {
        return createTokenBuilder(user.getUsername())
            .claim("tenant", user.getCompany().getName())
            .claim("role", user.getRole().name())
            .claim("permissions", user.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .expiration(Date.from(Instant.now().plusMillis(jwtProperties.getAccessTokenExpiry())))
            .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(String username) {
        return createTokenBuilder(username)
            .expiration(Date.from(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiry())))
            .compact();
    }

    // 클레임 추출 (예외 처리 추가)
    public Claims extractClaims(String token) {

        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw CustomAuthenticationException.of(AuthComponent.TOKEN_VALIDATION, ResultCode.Error.AUTH_TOKEN_EXPIRED, e);
        } catch (MalformedJwtException e) {
            throw CustomAuthenticationException.of(AuthComponent.TOKEN_VALIDATION, ResultCode.Error.AUTH_TOKEN_INVALID, e);
        } catch (Exception e) {
            throw CustomAuthenticationException.of(AuthComponent.TOKEN_VALIDATION, ResultCode.Error.AUTH_TOKEN_INVALID, e);
        }

    }

    // 사용자 이름 추출
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        return extractClaims(token)
            .getExpiration()
            .before(Date.from(Instant.now()));
    }

    // 토큰 유효성 검사 (추가 유틸 메서드)
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !isTokenExpired(token);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
