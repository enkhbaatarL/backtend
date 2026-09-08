package com.sk.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Saml2 saml2;
    private Oauth2 oauth2;

    /**
     * 특정 로그인 ID에 대해 SAML2 기본 만료 시간(saml2) 대신 적용할 예외 설정.
     * 위에서부터 순서대로 매칭되며, 먼저 매칭된 설정을 사용한다.
     */
    private List<Saml2Override> saml2Overrides = new ArrayList<>();

    private String privateKey;
    private String publicKey;

    /**
     * 로그인 ID에 해당하는 SAML2 액세스 토큰 만료 시간 조회 (예외 설정 없으면 기본 설정)
     */
    public Duration resolveSaml2AccessTokenExpiry(String userLoginId) {
        return findSaml2Override(userLoginId)
            .map(Saml2Override::getAccessTokenExpiry)
            .orElse(saml2.getAccessTokenExpiry());
    }

    /**
     * 로그인 ID에 해당하는 SAML2 리프레시 토큰 만료 시간 조회 (예외 설정 없으면 기본 설정)
     */
    public Duration resolveSaml2RefreshTokenExpiry(String userLoginId) {
        return findSaml2Override(userLoginId)
            .map(Saml2Override::getRefreshTokenExpiry)
            .orElse(saml2.getRefreshTokenExpiry());
    }

    private Optional<Saml2Override> findSaml2Override(String userLoginId) {
        if (userLoginId == null || saml2Overrides == null) {
            return Optional.empty();
        }
        return saml2Overrides.stream()
            .filter(override -> override.matches(userLoginId))
            .findFirst();
    }

    @Getter
    @Setter
    public static class Saml2 {
        private Duration accessTokenExpiry;
        private Duration refreshTokenExpiry;
    }

    @Getter
    @Setter
    public static class Oauth2 {
        private Duration accessTokenExpiry;
        private Duration refreshTokenExpiry;
    }

    @Getter
    @Setter
    public static class Saml2Override {
        /** 이 설정을 적용할 로그인 ID 목록 (대소문자 무시) */
        private List<String> loginIds = new ArrayList<>();
        private Duration accessTokenExpiry;
        private Duration refreshTokenExpiry;

        private boolean matches(String userLoginId) {
            if (loginIds == null) {
                return false;
            }
            String target = normalize(userLoginId);
            return loginIds.stream()
                .filter(Objects::nonNull)
                .anyMatch(loginId -> normalize(loginId).equals(target));
        }

        private static String normalize(String loginId) {
            return loginId.trim().toLowerCase(Locale.ROOT);
        }
    }

}
