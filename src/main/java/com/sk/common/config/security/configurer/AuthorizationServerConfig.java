package com.sk.common.config.security.configurer;

import com.sk.common.config.security.CustomAccessDeniedHandler;
import com.sk.common.config.security.CustomAuthenticationEntryPoint;
import com.sk.common.config.security.jwt.CustomJwtEncoder;
import com.sk.common.config.security.oauth2.AesPasswordEncoder;
import com.sk.common.config.security.oauth2.CustomAccessTokenResponseHandler;
import com.sk.common.config.security.oauth2.CustomAuthenticationFailureHandler;
import com.sk.common.filter.ExceptionHandlerFilter;
import com.sk.common.utils.JwtUtil;
import com.sk.common.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    @Value("${server.domain}")
    private String serverDomain;

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final ExceptionHandlerFilter exceptionHandlerFilter;
    private final RegisteredClientRepository registeredClientRepository;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomAccessTokenResponseHandler customAccessTokenResponseHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        if (securityEnabled) {

            OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

            // 토큰 엔드포인트 커스터마이징 (grant_type: client_credentials 지원)
            authorizationServerConfigurer
                .clientAuthentication(clientAuth -> clientAuth.errorResponseHandler(customAuthenticationFailureHandler)) // BasicAuthenticationFilter
                .tokenEndpoint(tokenEndpoint ->
                    tokenEndpoint
                        .accessTokenResponseHandler(customAccessTokenResponseHandler)
                        .errorResponseHandler(customAuthenticationFailureHandler) // OAuth2TokenEndpointFilter
                );

            // Authorization Server 관련 경로 매칭
            http
                .cors(withDefaults())
                .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
                .securityMatcher("/api/v1/oauth2/token") // Authorization Server 경로 제한
                .with(authorizationServerConfigurer, withDefaults())
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                    .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/v1/oauth2/token").permitAll()
                    .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();

        } else {

            http
                .cors(withDefaults())
                .securityMatcher("/api/v1/oauth2/token")
                .authorizeHttpRequests(authorize -> authorize
                    .anyRequest().permitAll() // 보안 비활성화 시 모든 요청 허용
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();
        }
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
            .issuer(serverDomain)
            .tokenEndpoint("/api/v1/oauth2/token")
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${aes.secret-key}") String secretKey) {
        return new AesPasswordEncoder(secretKey);
    }

}
