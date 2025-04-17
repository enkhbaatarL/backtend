package com.sk.common.config.security.configurer;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import com.sk.common.config.security.saml.CustomSamlAuthenticationFailureHandler;
import com.sk.common.config.security.saml.CustomSamlUserAuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SamlUserServiceProviderConfig {

    private final CustomSamlUserAuthenticationSuccessHandler customSamlUserAuthenticationSuccessHandler;
    private final CustomSamlAuthenticationFailureHandler customSamlAuthenticationFailureHandler;

    @Bean
    @Order(3)
    SecurityFilterChain samlUserSecurityFilterChain(HttpSecurity http) throws Exception {
        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
        http
            .cors(withDefaults())
            .securityMatcher("/login/saml2/user", "/login/saml2/sso/mi", "/saml2/authenticate/**", "/login/saml2/saml-callback") // SAML 로그인
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll())
            .saml2Login(saml2 -> saml2
                .authenticationManager(new ProviderManager(authenticationProvider))
                .loginPage("/saml2/authenticate?registrationId=mi")
                .successHandler(customSamlUserAuthenticationSuccessHandler)
                .failureHandler(customSamlAuthenticationFailureHandler))
            .saml2Logout(withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
