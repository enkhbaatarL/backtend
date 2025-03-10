package com.sk.common.config.security.configurer;

import lombok.RequiredArgsConstructor;
import org.opensaml.xmlsec.signature.P;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class DefaultConfig {

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    @Order(5)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        if (securityEnabled) {

            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
                );
    //            .exceptionHandling(); todo 추가
            return http.build();

        } else {

            http
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                );

            return http.build();

        }
    }

}
