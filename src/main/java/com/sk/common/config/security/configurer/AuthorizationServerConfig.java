package com.sk.common.config.security.configurer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.sk.common.config.security.CustomJwtEncoder;
import com.sk.common.config.security.oauth2.CustomAccessTokenResponseConverter;
import com.sk.common.config.security.oauth2.CustomAuthenticationFailureHandler;
import com.sk.common.config.security.oauth2.CustomCustomRegisteredClientRepository;
import com.sk.common.utils.JsonUtil;
import com.sk.common.utils.JwtProperties;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.mi.dto.response.OAuth2Res;
import com.sk.domain.shared.repository.master.Oauth2RegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;
    private final CustomAccessTokenResponseConverter customAccessTokenResponseConverter;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final JwtProperties jwtProperties;
    private final JwtUtil jwtUtil;
    private final CustomCustomRegisteredClientRepository customCustomRegisteredClientRepository;

    @Bean
    @Order(1) // Authorization Server가 먼저 처리되도록 우선순위 설정
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

        // 토큰 엔드포인트 커스터마이징 (client_credentials 지원)
        authorizationServerConfigurer
            .tokenEndpoint(tokenEndpoint ->
                tokenEndpoint
                    .accessTokenResponseHandler((request, response, authentication) -> {
                        OAuth2Res OAuth2Res = customAccessTokenResponseConverter.convert((OAuth2AccessTokenAuthenticationToken) authentication);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(JsonUtil.getMapper().writeValueAsString(OAuth2Res));
                    })
                    .errorResponseHandler(customAuthenticationFailureHandler)
            );

        // Authorization Server 관련 경로만 매칭
        http
            .securityMatcher("/oauth2/token", "/oauth2/jwks") // Authorization Server 경로 제한
            .with(authorizationServerConfigurer, Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/token", "/oauth2/jwks").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\": \"authentication_failed\", \"message\": \"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\": \"access_denied\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                })
            );;

        return http.build();
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() { // 와일드카드로 수정
        CustomJwtEncoder jwtEncoder = new CustomJwtEncoder(jwtUtil, oauth2RegisteredClientRepository, jwtProperties);
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        return new DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            new OAuth2AccessTokenGenerator()
        );
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8080")
            .tokenEndpoint("/oauth2/token")
            .jwkSetEndpoint("/oauth2/jwks")
            .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
