package com.sk.common.config.security.configurer;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import com.sk.common.config.security.saml.CustomSamlAdminAuthenticationSuccessHandler;
import com.sk.common.config.security.saml.CustomSamlAuthenticationFailureHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SamlAdminServiceProviderConfig {

    private final CustomSamlAdminAuthenticationSuccessHandler customSamlAdminAuthenticationSuccessHandler;
    private final CustomSamlAuthenticationFailureHandler customSamlAuthenticationFailureHandler;

    @Bean
    @Order(4)
    SecurityFilterChain samlAdminSecurityFilterChain(HttpSecurity http) throws Exception {
        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
        http
                .securityMatcher("/login/saml2/admin", "/login/saml2/sso/skt-admin") // SAML 로그인
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login/saml2/admin").authenticated()
                        .anyRequest().permitAll())
                .saml2Login(saml2 -> saml2
                        .authenticationManager(new ProviderManager(authenticationProvider))
                        .loginPage("/saml2/authenticate?registrationId=skt-admin")
                        .successHandler(customSamlAdminAuthenticationSuccessHandler)
                        .failureHandler(customSamlAuthenticationFailureHandler))
                .saml2Logout(withDefaults());
        return http.build();
    }
}
