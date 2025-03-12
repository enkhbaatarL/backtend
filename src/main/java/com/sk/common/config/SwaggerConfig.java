package com.sk.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev"})
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components().addSecuritySchemes("BearerAuth",
                new SecurityScheme()
                    .type(Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ))
            .info(new Info().title("Backend API Docs")
                .description("MI Platform API Docs")
                .version("1.0"));
    }

    @Bean
    public GroupedOpenApi companyApi() {
        return GroupedOpenApi.builder()
            .group("MI API")
            .displayName("MI 서비스 API")
            .pathsToMatch("/api/v1/mi/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("ADMIN API")
            .displayName("관리자 API")
            .pathsToMatch("/api/v1/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allOtherApis() {
        return GroupedOpenApi.builder()
            .group("ETC API")
            .displayName("기타 API")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/v1/mi/**", "/api/v1/admin/**")
            .build();
    }

}
