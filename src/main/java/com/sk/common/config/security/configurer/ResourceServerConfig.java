package com.sk.common.config.security.configurer;

import com.sk.common.config.security.CustomAccessDeniedHandler;
import com.sk.common.config.security.CustomAuthenticationEntryPoint;
import com.sk.common.config.security.CustomJwtAuthenticationConverter;
import com.sk.common.config.security.CustomJwtDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class ResourceServerConfig {

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    private final CustomJwtDecoder customJwtDecoder;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {

        if (securityEnabled) {

            http
                .securityMatcher("/api/**") // Resource Server 경로 매칭
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(customJwtAuthenticationConverter))
                    .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                    .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();

        } else {

            http
                .securityMatcher("/api/**") // Resource Server 경로 매칭
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                );

            return http.build();
        }

    }

}
