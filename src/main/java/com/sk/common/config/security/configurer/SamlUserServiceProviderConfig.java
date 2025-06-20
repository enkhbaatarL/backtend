package com.sk.common.config.security.configurer;

import static org.springframework.security.config.Customizer.withDefaults;

import com.sk.common.config.security.saml.CustomSamlAuthenticationFailureHandler;
import com.sk.common.config.security.saml.CustomSamlUserAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SamlUserServiceProviderConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    private final CustomSamlUserAuthenticationSuccessHandler customSamlUserAuthenticationSuccessHandler;
    private final CustomSamlAuthenticationFailureHandler customSamlAuthenticationFailureHandler;

    @Bean
    @Order(2)
    SecurityFilterChain samlUserSecurityFilterChain(HttpSecurity http) throws Exception {
        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        // .securityMatcher("/login/saml2/user", "/saml2/sso/mi", "/saml2/authenticate/**",
        //         "/saml2/service-provider-metadata/mi", "/saml/metadata", "/saml/SingleLogout",
        // "/login/saml2/saml-callback")
        .securityMatcher("/saml2/**", "/saml/**", "/login/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .saml2Login(
            saml2 ->
                saml2
                    .authenticationManager(new ProviderManager(authenticationProvider))
                    .successHandler(customSamlUserAuthenticationSuccessHandler)
                    .failureHandler(customSamlAuthenticationFailureHandler))
        .saml2Logout(withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}
