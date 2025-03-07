package com.sk.common.config.security.configurer;

import com.sk.common.config.security.CustomAccessDeniedHandler;
import com.sk.common.config.security.CustomAuthenticationEntryPoint;
import com.sk.common.config.security.CustomJwtEncoder;
import com.sk.common.config.security.oauth2.CustomAccessTokenResponseHandler;
import com.sk.common.config.security.oauth2.CustomAuthenticationFailureHandler;
import com.sk.common.utils.JwtProperties;
import com.sk.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RegisteredClientRepository registeredClientRepository;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomAccessTokenResponseHandler customAccessTokenResponseHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

        // 토큰 엔드포인트 커스터마이징 (grant_type: client_credentials 지원)
        authorizationServerConfigurer
            .tokenEndpoint(tokenEndpoint ->
                tokenEndpoint
                    .accessTokenResponseHandler(customAccessTokenResponseHandler)
                    .errorResponseHandler(customAuthenticationFailureHandler) // OAuth2TokenEndpointFilter
            );

        // Authorization Server 관련 경로 매칭
        http
            .securityMatcher("/oauth2/token", "/oauth2/jwks") // Authorization Server 경로 제한
            .with(authorizationServerConfigurer, Customizer.withDefaults())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/token", "/oauth2/jwks").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        CustomJwtEncoder jwtEncoder = new CustomJwtEncoder(jwtUtil, registeredClientRepository, jwtProperties);
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

//    @Bean
//    public JWKSource<SecurityContext> jwkSource() throws Exception {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        keyPairGenerator.initialize(2048);
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
//        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
//
//        RSAKey rsaKey = new RSAKey.Builder(publicKey)
//            .privateKey(privateKey)
//            .keyID(UUID.randomUUID().toString())
//            .build();
//
//        JWKSet jwkSet = new JWKSet(rsaKey);
//        return new ImmutableJWKSet<>(jwkSet);
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
