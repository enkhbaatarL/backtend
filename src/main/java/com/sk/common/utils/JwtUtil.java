package com.sk.common.utils;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;

import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.common.properties.JwtProperties;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.service.RefreshTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${server.domain}")
    private String miDefaultDomain;

    private SecretKey secretKey;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    // 비밀 키 초기화
    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }

    // 공통 토큰 빌더 생성 메서드
    private JwtBuilder createTokenBuilder(String subject) {
        return Jwts.builder()
            .header().type("JWT").and()
            .subject(subject)
            .issuedAt(new Date())
            .signWith(secretKey, Jwts.SIG.HS256); // 명시적으로 HS256 지정
    }

    // 액세스 토큰 생성 (By SAML2 인증 방식)
    public String generateAccessToken(User user) {
        return createTokenBuilder(user.getUserLoginId())
            .claim(Claim.USER_NO.getCode(), user.getUserNo())
            .claim(Claim.COMPANY_NO.getCode(), user.getCustomerCompany().getCustomerCompanyNo())
            .claim(Claim.COMPANY_CD.getCode(), user.getCustomerCompany().getCustomerCompanyCd())
            .claim(Claim.ROLE.getCode(), user.getUserRoleCd().getCode())
            .claim(Claim.OTHER_SERVICE_ACCESS.getCode(), ObjectUtils.defaultIfNull(user.getIsOtherServiceAccessPossible(), false))
            .claim(Claim.COMPANY_DOMAIN.getCode(), ObjectUtils.defaultIfNull(user.getCustomerCompany().getCustomerCompanyMipUrl(), miDefaultDomain))
            .claim(Claim.AUTH_METHOD.getCode(), Claim.AuthMethodValue.SAML2.getCode()) // 인증 방식 추가
            .expiration(Date.from(Instant.now().plusMillis(jwtProperties.getAccessTokenExpiry())))
            .compact();
    }

    // 액세스 토큰 생성 (By Oauth2 client_credentials 인증 방식)
    // USER_NO, COMPANY_DOMAIN 생략됨
    public String generateAccessToken(String clientId, RegisteredClient registeredClient) {
        return createTokenBuilder(clientId)
            .claim(Claim.COMPANY_NO.getCode(), registeredClient.getClientSettings().getSetting(Claim.COMPANY_NO.getCode())) // 회사번호
            .claim(Claim.COMPANY_CD.getCode(), registeredClient.getClientSettings().getSetting(Claim.COMPANY_CD.getCode())) // 회사코드
            .claim(Claim.ROLE.getCode(), registeredClient.getClientSettings().getSetting(Claim.ROLE.getCode()))
            .claim(Claim.OTHER_SERVICE_ACCESS.getCode(), Boolean.FALSE)
            .claim(Claim.AUTH_METHOD.getCode(), Claim.AuthMethodValue.OAUTH2_CLIENT_CREDENTIALS.getCode()) // 인증 방식 추가
            .expiration(Date.from(Instant.now().plusMillis(jwtProperties.getAccessTokenExpiry())))
            .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(User user) {
        Instant expirationInstant = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiry());
        long expirationUnixTime = expirationInstant.getEpochSecond();

        String refreshToken = createTokenBuilder(user.getUserLoginId())
            .expiration(Date.from(expirationInstant))
            .compact();

        // Redis 저장
        refreshTokenService.saveRefreshToken(user.getUserNo(), refreshToken, expirationUnixTime);
        return refreshToken;
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

    // 만료 여부 무시하고 Claims 추출 (새 메서드)
    public Claims extractClaimsIgnoreExpiration(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token) // 만료된 경우에도 Claims 반환
                .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료 예외를 무시하고 Claims 반환
            return e.getClaims();
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
            return !isTokenExpired(token);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // 만료 여부 무시하고 토큰 유효성 검사 (새 메서드)
    public boolean isTokenValidIgnoreExpiration(String token) {
        try {
            extractClaimsIgnoreExpiration(token); // 서명과 구조만 확인
            return true;
        } catch (CustomAuthenticationException e) {
            return false; // 서명 오류, 구조 오류 등
        }
    }
}
