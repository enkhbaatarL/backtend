package com.sk.common.config;

import com.sk.common.annotation.ApiOrder;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Profile({"local", "dev"})
@Configuration
@RequiredArgsConstructor
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
    public GroupedOpenApi miApi(OpenApiCustomizer sortPathsOpenApiCustomizer, OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("MI API")
            .displayName("MI 서비스 API")
            .pathsToMatch("/api/v1/mi/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi(OpenApiCustomizer sortPathsOpenApiCustomizer, OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("ADMIN API")
            .displayName("관리자 API")
            .pathsToMatch("/api/v1/admin/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi etcApi(OpenApiCustomizer sortPathsOpenApiCustomizer, OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("ETC API")
            .displayName("기타 API")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/v1/mi/**", "/api/v1/admin/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public OperationCustomizer apiOrderOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiOrder apiOrder = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiOrder.class);
            int order = (apiOrder != null) ? apiOrder.value() : Integer.MAX_VALUE;
            operation.addExtension("x-order", order);
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer sortPathsOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            Map<String, PathItem> originalPaths = openApi.getPaths();
            Map<String, PathItem> sortedPathsMap = new LinkedHashMap<>();

            originalPaths.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> getMinOrder(entry.getValue())))
                .forEachOrdered(entry -> sortedPathsMap.put(entry.getKey(), entry.getValue()));

            // Map을 Paths 객체로 변환
            Paths sortedPaths = new Paths();
            sortedPathsMap.forEach(sortedPaths::addPathItem);
            openApi.setPaths(sortedPaths);
        };
    }

    private int getMinOrder(PathItem pathItem) {
        int min = Integer.MAX_VALUE;
        if (pathItem.getGet() != null) {
            min = Math.min(min, getOrder(pathItem.getGet()));
        }
        if (pathItem.getPost() != null) {
            min = Math.min(min, getOrder(pathItem.getPost()));
        }
        if (pathItem.getPut() != null) {
            min = Math.min(min, getOrder(pathItem.getPut()));
        }
        if (pathItem.getDelete() != null) {
            min = Math.min(min, getOrder(pathItem.getDelete()));
        }
        if (pathItem.getPatch() != null) {
            min = Math.min(min, getOrder(pathItem.getPatch()));
        }
        return min;
    }

    private int getOrder(Operation operation) {
        if (operation.getExtensions() != null && operation.getExtensions().get("x-order") != null) {
            Object ext = operation.getExtensions().get("x-order");
            if (ext instanceof Number) {
                return ((Number) ext).intValue();
            } else if (ext instanceof String) {
                try {
                    return Integer.parseInt((String) ext);
                } catch (NumberFormatException e) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

}
