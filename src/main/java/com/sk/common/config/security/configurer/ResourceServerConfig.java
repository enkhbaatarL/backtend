package com.sk.common.config.security.configurer;

import com.sk.common.config.security.CustomAccessDeniedHandler;
import com.sk.common.config.security.CustomAuthenticationEntryPoint;
import com.sk.common.config.security.jwt.CustomJwtAuthenticationConverter;
import com.sk.common.config.security.jwt.CustomJwtDecoder;
import com.sk.common.config.security.mcp.McpAuthenticationProvider;
import com.sk.common.config.security.mcp.McpCredentialVerifier;
import com.sk.common.filter.ExceptionHandlerFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final CustomJwtDecoder customJwtDecoder;
    private final ExceptionHandlerFilter exceptionHandlerFilter;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    // [MCP 인증] MCP 체인의 Basic Auth 검증기
    private final McpCredentialVerifier mcpCredentialVerifier;

    /**
     * [MCP 인증] MCP API 는 tmd_mcpauthinfom 의 키로 Basic Auth 인증합니다. (이전: 개발용 우회 체인 @Order(3) + denyAll)
     * 사용자 위임(X-User-Id)과 회사 일치 검사는 McpUserContextInterceptor 에서 수행합니다.
     */
    @Bean
    @Order(4)
    public SecurityFilterChain mcpBasicAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
            .securityMatcher("/api/v1/mcp/mi/next-gen/**")
            .csrf(AbstractHttpConfigurer::disable)
            // [MCP 인증] denyAll → Basic Auth. Provider 는 빈이 아니라 이 체인 전용으로 생성
            .authenticationManager(new ProviderManager(new McpAuthenticationProvider(mcpCredentialVerifier)))
            .httpBasic(basic -> basic.authenticationEntryPoint(customAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(new CustomAccessDeniedHandler()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(5)
    public SecurityFilterChain tokenByPassSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
            .securityMatcher("/api/v1/auth/**", "/api/v1/test/**", "/api/v1/saml2/**", "/api/v1/external/**",
                "/api/v1/common/health-check",
                "/api/v1/admin/provider-management/goods-datas/upload-temp/structured-data/**", // TODO 인증 넣어야 함.
                "/api/v1/mi/internal/newsletter/publish",
                "/api/v1/mi/internal/newsletter/render-data",  // chart-capture JWT로 자체 인증
                "/api/v1/mi/fundamental-model/runs/*/callback"  // k8s Job pod 내부 호출 — 네트워크 격리로 보호, ingress 차단 필요
            )
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
            );

        return http.build();
    }

    @Bean
    @Order(6)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
            .securityMatcher("/api/**") // Resource Server 경로 매칭
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(customJwtDecoder)
                    .jwtAuthenticationConverter(customJwtAuthenticationConverter))
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
            );

        return http.build();

    }

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
