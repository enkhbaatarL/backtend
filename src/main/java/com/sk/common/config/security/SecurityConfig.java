//package com.sk.common.config.security;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.RSAKey;
//import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
//import com.nimbusds.jose.jwk.source.JWKSource;
//import com.nimbusds.jose.proc.SecurityContext;
//import com.sk.common.config.security.oauth2.CustomAccessTokenResponseConverter;
//import com.sk.common.config.security.oauth2.CustomAuthenticationFailureHandler;
//import com.sk.common.config.security.oauth2.CustomOAuthExceptionTranslator;
//import com.sk.common.filter.ExceptionHandlerFilter;
//import com.sk.common.filter.HttpLoggingFilter;
//import com.sk.domain.mi.dto.response.ErrorRes;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.jwt.JwtEncoder;
//import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
//import org.springframework.security.web.SecurityFilterChain;
//
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.interfaces.RSAPrivateKey;
//import java.security.interfaces.RSAPublicKey;
//import java.util.UUID;
//
//@Configuration
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomJwtDecoder customJwtDecoder;
//    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
//    private final ObjectMapper objectMapper;
//    private final HttpLoggingFilter httpLoggingFilter;
//    private final ExceptionHandlerFilter exceptionHandlerFilter;
//
//    private final CustomAccessTokenResponseConverter customAccessTokenResponseConverter;
//    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
//    private final CustomOAuthExceptionTranslator customOAuthExceptionTranslator;
//    private final RegisteredClientRepository registeredClientRepository;
//
//    // Authorization Server 설정
//    @Bean
//    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
//
////        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();
////
////        // 커스텀 응답 컨버터 적용
////        authorizationServerConfigurer
////            .tokenEndpoint(tokenEndpoint ->
////                tokenEndpoint
////                    .accessTokenResponseHandler((request, response, authentication) -> {
////                        OAuth2Res OAuth2Res = customAccessTokenResponseConverter.convert(
////                            (OAuth2AccessTokenAuthenticationToken) authentication
////                        );
////                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
////                        response.getWriter().write(JsonUtil.getMapper().writeValueAsString(OAuth2Res));
////                    })
//////                    .authenticationProvider(new CustomClientAuthenticationProvider(registeredClientRepository))
////                    .errorResponseHandler(customAuthenticationFailureHandler) // 클라이언트 인증 실패는 OAuth2TokenEndpointFilter에서 먼저 처리되며, errorResponseHandler에 도달하지 않음,
////            );
//
//
//        http
////            .with(authorizationServerConfigurer, Customizer.withDefaults())
//            .authorizeHttpRequests(authorize -> authorize
//                .requestMatchers("/oauth2/token").permitAll()
//                .anyRequest().authenticated()
//            )
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/token"))
//            .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                .decoder(customJwtDecoder)
//                                .jwtAuthenticationConverter(customJwtAuthenticationConverter))
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//                            ErrorRes errorRes = new ErrorRes(
//                                    HttpStatus.UNAUTHORIZED.value(),
//                                    "Unauthorized@@",
//                                    authException.getMessage()
//                            );
//
//                            objectMapper.writeValue(response.getOutputStream(), errorRes);
//                        })
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            response.setStatus(HttpStatus.FORBIDDEN.value());
//                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//                            ErrorRes errorRes = new ErrorRes(
//                                    HttpStatus.FORBIDDEN.value(),
//                                    "Forbidden!!",
//                                    accessDeniedException.getMessage()
//                            );
//
//                            objectMapper.writeValue(response.getOutputStream(), errorRes);
//                        })
//                )
//                        .headers(headers -> headers
//                            .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
//        ;
//
//        return http.build();
//    }
//
////    @Bean
////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////        http
////                .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
////                .addFilterAfter(httpLoggingFilter, SecurityContextHolderFilter.class)
////                .securityMatcher("/**")  // OAuth2 이외의 모든 요청 매칭
////                .csrf(AbstractHttpConfigurer::disable)
////                .authorizeHttpRequests(auth -> auth
////                        .requestMatchers("/api/auth/**", "swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/oauth2/token", "/v1/sample/**").permitAll()
////                        .anyRequest().authenticated()
////                )
////                .oauth2ResourceServer(oauth2 -> oauth2
////                        .jwt(jwt -> jwt
////                                .decoder(customJwtDecoder)
////                                .jwtAuthenticationConverter(customJwtAuthenticationConverter))
////                        .authenticationEntryPoint((request, response, authException) -> {
////                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
////                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
////
////                            ErrorRes errorRes = new ErrorRes(
////                                    HttpStatus.UNAUTHORIZED.value(),
////                                    "Unauthorized",
////                                    authException.getMessage()
////                            );
////
////                            objectMapper.writeValue(response.getOutputStream(), errorRes);
////                        })
////                        .accessDeniedHandler((request, response, accessDeniedException) -> {
////                            response.setStatus(HttpStatus.FORBIDDEN.value());
////                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
////
////                            ErrorRes errorRes = new ErrorRes(
////                                    HttpStatus.FORBIDDEN.value(),
////                                    "Forbidden",
////                                    accessDeniedException.getMessage()
////                            );
////
////                            objectMapper.writeValue(response.getOutputStream(), errorRes);
////                        })
////                )
////                // 동일한 origin 만 허용
////                .headers(headers -> headers
////                        .frameOptions(FrameOptionsConfig::sameOrigin))
////                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
////
////        return http.build();
////    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    // JWKSource 설정 (JWT 서명을 위한 RSA 키 쌍 생성)
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
//
//    // JWT Encoder 설정
//    @Bean
//    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
//        return new NimbusJwtEncoder(jwkSource);
//    }
//
//}
