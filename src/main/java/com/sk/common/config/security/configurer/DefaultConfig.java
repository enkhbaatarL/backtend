package com.sk.common.config.security.configurer;

import com.sk.common.filter.ExceptionHandlerFilter;
import com.sk.common.filter.HttpLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@RequiredArgsConstructor
public class DefaultConfig {

    private final HttpLoggingFilter httpLoggingFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

//    @Bean
//    @Order(3)
//    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .addFilterBefore(exceptionHandlerFilter, SecurityContextHolderFilter.class)
//            .addFilterAfter(httpLoggingFilter, SecurityContextHolderFilter.class)
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
//                .anyRequest().authenticated()
//            );
//        return http.build();
//    }

}
