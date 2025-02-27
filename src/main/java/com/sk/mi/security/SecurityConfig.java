package com.sk.mi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.mi.response.ErrorResponse;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomJwtDecoder customJwtDecoder;
  private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/**")  // OAuth2 이외의 모든 요청 매칭
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth ->  auth
            .requestMatchers("/api/auth/**", "swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/oauth2/token").permitAll()
            .anyRequest().authenticated()
        )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(customJwtAuthenticationConverter))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    ErrorResponse errorResponse = new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        authException.getMessage()
                    );
                    
                    objectMapper.writeValue(response.getOutputStream(), errorResponse);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    ErrorResponse errorResponse = new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        accessDeniedException.getMessage()
                    );
                    
                    objectMapper.writeValue(response.getOutputStream(), errorResponse);
                })
            )
        // 동일한 origin 만 허용
        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
