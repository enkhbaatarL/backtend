//package com.sk.common.config;
//
//import java.util.Set;
//import java.util.UUID;
//
//import com.sk.common.config.security.CustomJwtEncoder;
//import org.springframework.boot.autoconfigure.security.SecurityProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
//import org.springframework.security.oauth2.core.OAuth2Token;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
//import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
//import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
//import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
//import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
//import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
//import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
//import org.springframework.security.web.SecurityFilterChain;
//
//import com.sk.domain.shared.repository.master.JpaRegisteredClientRepository;
//import com.sk.domain.shared.repository.master.OAuth2ClientRepository;
//import com.sk.common.utils.JwtProperties;
//import com.sk.common.utils.JwtUtil;
//
//import lombok.RequiredArgsConstructor;
//
//@Configuration
//@RequiredArgsConstructor
//public class AuthorizationServerConfig {
//
//    private final JwtUtil jwtUtil;
//    private final JwtProperties jwtProperties;
//    private final JpaRegisteredClientRepository registeredClientRepository;
//    private final OAuth2ClientRepository oauth2ClientRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Bean
//    @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
//    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
//        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
//
//        http.securityMatcher("/oauth2/**")  // OAuth2 관련 엔드포인트만 매칭
//            .with(authorizationServerConfigurer, config -> {})
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/token"))
//            .authorizeHttpRequests(authorize -> authorize
//                .requestMatchers("/oauth2/token").permitAll()
//                .anyRequest().authenticated()
//            );
//
//        return http.build();
//    }
//
//    @Bean
//    public RegisteredClient initializeRegisteredClient() {
//        RegisteredClient registeredClient = null;
//        if (registeredClientRepository.findByClientId("mi-client") == null) {
//            registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
//                    .clientId("mi-client")
//                    .clientSecret(passwordEncoder.encode("secret"))
//                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                    .scope("read")
//                    .scope("write")
//                    .clientSettings(ClientSettings.builder()
//                        .setting("tenant", "skinnovation")
//                        .setting("permissions", Set.of("REFINITIV_VIEW", "REFINITIV_DOWNLOAD"))
//                        .build())
//                        .build();
//
//            registeredClientRepository.save(registeredClient);
//        }
//        return registeredClient;
//    }
//
//    @Bean
//    public OAuth2TokenGenerator<OAuth2Token> tokenGenerator() {
//        CustomJwtEncoder jwtEncoder = new CustomJwtEncoder(jwtUtil, oauth2ClientRepository, jwtProperties);
//        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
//        return new DelegatingOAuth2TokenGenerator(
//            jwtGenerator,
//            new OAuth2AccessTokenGenerator()
//        );
//    }
//
//    @Bean
//    public AuthorizationServerSettings authorizationServerSettings() {
//        return AuthorizationServerSettings.builder().build();
//    }
//}
