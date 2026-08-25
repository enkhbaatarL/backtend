package com.sk.common.config;

import com.sk.common.converter.JsonValueEnumConverterFactory;
import com.sk.common.interceptor.ApiAccessLogInterceptor;
import com.sk.common.interceptor.McpUserContextInterceptor;
import com.sk.common.resolver.UserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserArgumentResolver userArgumentResolver;
    private final McpUserContextInterceptor mcpUserContextInterceptor;
    private final ApiAccessLogInterceptor apiAccessLogInterceptor;
    private final JsonValueEnumConverterFactory jsonValueEnumConverterFactory;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mcpUserContextInterceptor)
            .addPathPatterns("/api/v1/mcp/mi/next-gen/**");
        registry.addInterceptor(apiAccessLogInterceptor)
            .addPathPatterns("/api/**"); // API 경로에만 적용
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(jsonValueEnumConverterFactory);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }

}
