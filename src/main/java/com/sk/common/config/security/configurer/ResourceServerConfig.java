package com.sk.common.config.security.configurer;

import com.sk.common.config.security.CustomJwtAuthenticationConverter;
import com.sk.common.config.security.CustomJwtDecoder;
import com.sk.common.filter.ExceptionHandlerFilter;
import com.sk.common.filter.HttpLoggingFilter;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.ErrorRes;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final CustomJwtDecoder customJwtDecoder;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
    private final HttpLoggingFilter httpLoggingFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

//    @Bean
//    @Order(2) // Resource Server가 나중에 처리되도록 우선순위 설정
//    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .securityMatcher("/api/**") // Resource Server 경로만 매칭
//            .csrf(AbstractHttpConfigurer::disable)
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/api/auth/**", "/v1/sample/**").permitAll()
//                .anyRequest().authenticated()
//            )
//            .oauth2ResourceServer(oauth2 -> oauth2
//                .jwt(jwt -> jwt
//                    .decoder(customJwtDecoder)
//                    .jwtAuthenticationConverter(customJwtAuthenticationConverter))
//                .authenticationEntryPoint((request, response, authException) -> {
//                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
//                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                    ErrorRes errorRes = new ErrorRes(
//                        HttpStatus.UNAUTHORIZED.value(),
//                        "Unauthorized",
//                        authException.getMessage()
//                    );
//                    JsonUtil.getMapper().writeValue(response.getOutputStream(), errorRes);
//                })
//                .accessDeniedHandler((request, response, accessDeniedException) -> {
//                    response.setStatus(HttpStatus.FORBIDDEN.value());
//                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                    ErrorRes errorRes = new ErrorRes(
//                        HttpStatus.FORBIDDEN.value(),
//                        "Forbidden",
//                        accessDeniedException.getMessage()
//                    );
//                    JsonUtil.getMapper().writeValue(response.getOutputStream(), errorRes);
//                })
//            )
//            .headers(headers -> headers
//                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
//            .sessionManagement(session -> session
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//
//        return http.build();
//    }

}
