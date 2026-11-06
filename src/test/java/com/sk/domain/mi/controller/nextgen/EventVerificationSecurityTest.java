package com.sk.domain.mi.controller.nextgen;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sk.common.config.security.CustomAuthenticationEntryPoint;
import com.sk.common.config.security.configurer.ResourceServerConfig;
import com.sk.common.config.security.jwt.CustomJwtAuthenticationConverter;
import com.sk.common.config.security.jwt.CustomJwtDecoder;
import com.sk.common.config.security.mcp.McpCredentialVerifier;
import com.sk.common.filter.ExceptionHandlerFilter;
import com.sk.domain.mi.service.nextgen.EventVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class EventVerificationSecurityTest {
    @Test
    void existingResourceServerChainRequiresLoginBeforeCallingVerification() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(SecurityOnlyConfig.class);
            context.refresh();
            var service = mock(EventVerificationService.class);
            var mvc = MockMvcBuilders.standaloneSetup(new EventVerificationController(service))
                .addFilters(context.getBean(FilterChainProxy.class)).build();
            mvc.perform(post("/api/v1/mi/next-gen/events/verifications")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
                .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class SecurityOnlyConfig {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            HandlerExceptionResolver resolver = (request, response, handler, exception) -> {
                response.setStatus(401);
                return new ModelAndView();
            };
            return new ResourceServerConfig(mock(CustomJwtDecoder.class), new ExceptionHandlerFilter(resolver),
                mock(CustomJwtAuthenticationConverter.class), new CustomAuthenticationEntryPoint(resolver),
                mock(McpCredentialVerifier.class)).resourceServerSecurityFilterChain(http);
        }
    }
}
