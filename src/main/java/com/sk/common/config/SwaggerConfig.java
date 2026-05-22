package com.sk.common.config;

import com.sk.common.annotation.ApiOrder;
import com.sk.domain.mi.config.FundamentalModelMockProperties;
import com.sk.domain.mi.config.ScenarioWorkbenchMockProperties;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Profile({"local", "tech", "dev", "prod", "testdb"})
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
                    .bearerFormat("JWT")))
            .info(new Info().title("Backend API Docs")
                .description("MI Platform API Docs")
                .version("1.0"));
    }

    @Bean
    public GroupedOpenApi miApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("MI API")
            .displayName("MI 서비스 API")
            .pathsToMatch("/api/v1/mi/**")
            .pathsToExclude("/api/v1/mi/scenario-workbench/**", "/api/v1/mi/fundamental-model/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                   @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("ADMIN API")
            .displayName("관리자 API")
            .pathsToMatch("/api/v1/admin/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi testApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                  @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("TEST API - 테스트 용")
            .displayName("테스트 API")
            .pathsToMatch("/api/v1/test/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    // note: 안정화 후 투머치 문서 모두 삭제예정
    private static final String SW_VERSION_BASE = "v0.5.1";
    private static final DateTimeFormatter SW_DEPLOY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public GroupedOpenApi scenarioWorkbenchApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                               @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer,
                                               ScenarioWorkbenchMockProperties mockProps,
                                               FundamentalModelMockProperties fundamentalModelMockProps) {

        String swDeployDate = LocalDateTime.now().format(SW_DEPLOY_DATE_FORMATTER);

        // note: 안정화 후 투머치 문서 모두 삭제예정
        String description = """
  ## 📋 배포 정보

  | 항목 | 내용 |
  |------|------|
  | **버전** | %s |
  | **배포일** | %s |

  ---

  ## 🔀 데이터 소스 현황

  | API | 데이터 |
  |-----|--------|
  | **이벤트** | |
  | 이벤트 유형 L1 조회 | %s |
  | 이벤트 목록 | %s |
  | 이벤트 상세 | %s |
  | 이벤트 관련 레코드 | %s |
  | 이벤트 기관전망 | %s |
  | **시나리오** | |
  | 시나리오 목록 | %s |
  | 시나리오 상세 | %s |
  | **Overview** | |
  | Overview Event First | %s |
  | Overview Scenario First | %s |
  | Overview Price Forecasts | %s |
  | **Design** | |
  | Design 이벤트 목록 | %s |
  | Design 시나리오 생성 요청 | %s |
  | Design 콜백 수신 | %s |
  | Design 생성된 시나리오 조회 | %s |
  | Design 생성 상태 폴링 | %s |
  | **Model Execution** | |
  | 수급모델 파라미터 조회 | %s |
  | 수급모델 실행 요청 | %s |
  | 수급모델 실행 상태 조회 | %s |
  | 수급모델 실행 콜백 | %s |
  | **Fundamental Model Case** | |
  | Fundamental Model Case 목록 | %s |
  | Fundamental Model Case 업로드 | %s |
  | Fundamental Model Case 상세 | %s |
  | Fundamental Model Case 수정 | %s |
  | Fundamental Model Case 삭제 | %s |
  | Fundamental Model Case 실행 요청 | %s |
  | Fundamental Model Case 상태 조회 | %s |
  | Fundamental Model Case 데이터 초기화 | %s |
  | **Fundamental Model Run** | |
  | Fundamental Model Run 콜백 | %s |

  ---

  ## 수정사항

  <details open>
  <summary><strong>v0.5.10 (2026-05-22)</strong></summary>

  - [Fundamental Model]
    - Case 목록/업로드/상세/수정/삭제/실행/상태/초기화 API 데이터 소스 현황 추가
    - Run 콜백 API 데이터 소스 현황 추가

  </details>

  <details>
  <summary><strong>v0.5.0 (2026-05-15)</strong></summary>

  - [이벤트]
    - `eventCategoryLv1` → `eventTypeLv1`으로 필드명 통일 (DB 스키마와 일치)
    - 이벤트 유형 필터: `List<String>` (코드) → `List<Long>` (PK ID) 변경
    - 요청 파라미터명 `eventTypeLv1` → `eventTypeL1Id`로 변경 (유형 조회 API 응답과 일치)
    - `parseJsonStringList` 로직 `JsonUtil`로 이관
    - Repository `Tuple` → `@QueryProjection` 리팩토링
  - [Mock]
    - 이벤트 유형 L1 ID 기반 필터 동작 지원 (실 DB PK 매핑)
    - 시나리오 태그(tags) mock 데이터 채움 (726개 전체)
  - [Swagger]
    - 데이터 소스 현황에 Model Execution API 추가
  - [공통]
    - `JsonUtil`에 `fromJson(TypeReference)`, `parseJsonStringList` 메서드 추가
    - 한글 깨짐(`@Comment` 인코딩) 수정

  </details>

  <details>
  <summary><strong>v0.4.0 (2026-05-11)</strong></summary>

  - [Design — 신규 도메인]
    - Design 이벤트 목록 조회 API 추가 (`GET /design/events`)
    - 시나리오 생성 요청 API 추가 (`POST /design/generate`)
    - 시나리오 생성 콜백 수신 API 추가 (`POST /design/callback`)
    - 생성된 시나리오 조회 API 추가 (`GET /design/generate/{requestId}/scenarios`)
    - 시나리오 생성 상태 폴링 API 추가 (`GET /design/generate/status`)
    - Mock 데이터 초기화 API 추가 (`POST /design/reset`)
  - [Design 플로우]
    - 생성 요청 시 `READY` -> `PROCESSING` 전환, 5초 후 자동 `COMPLETED` 전환

  </details>

  <details>
  <summary><strong>v0.3.4 (2026-05-08)</strong></summary>

  - [Overview]
    - Overview Event First / Scenario First / Price Forecast API 추가
  - [이벤트]
    - `signalType`, `signalTypeLabel` 필드 추가

  </details>

  <details>
  <summary><strong>v0.3.3 (2026-05-08)</strong></summary>

  - `impactLevel` → `marketImpactLevel` 필드명 변경

  </details>

  <details>
  <summary><strong>v0.3.2 (2026-05-07)</strong></summary>

  - 시나리오 상세: `Signpost 정의` 섹션 추가, `주요 가정` 간소화

  </details>

  ---
  """.formatted(
            SW_VERSION_BASE,
            swDeployDate,
            label(false),
            label(mockProps.getEvents().getApi().isGetList()),
            label(mockProps.getEvents().getApi().isGetDetail()),
            label(mockProps.getEvents().getApi().isGetRecords()),
            label(mockProps.getEvents().getApi().isGetOutlooks()),
            label(mockProps.getScenarios().getApi().isGetList()),
            label(mockProps.getScenarios().getApi().isGetDetail()),
            label(mockProps.getOverview().getApi().isGetEvents()),
            label(mockProps.getOverview().getApi().isGetScenarios()),
            label(mockProps.getOverview().getApi().isGetPriceForecasts()),
            label(mockProps.getDesign().getApi().isGetEvents()),
            label(mockProps.getDesign().getApi().isPostGenerate()),
            label(mockProps.getDesign().getApi().isPostCallback()),
            label(mockProps.getDesign().getApi().isGetGeneratedScenarios()),
            label(mockProps.getDesign().getApi().isGetStatuses()),
            label(mockProps.getModelExecutions().getApi().isGetParameters()),
            label(mockProps.getModelExecutions().getApi().isSubmit()),
            label(mockProps.getModelExecutions().getApi().isGetStatus()),
            label(mockProps.getModelExecutions().getApi().isCallback()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isListCases()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isCreateCase()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isGetDetail()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isUpdate()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isDelete()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isRun()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isGetStatus()),
            label(fundamentalModelMockProps.getFundamentalModelCases().getApi().isReset()),
            label(fundamentalModelMockProps.getFundamentalModelRuns().getApi().isCallback())
        );

        return GroupedOpenApi.builder()
            .group("SK Gas API")
            .displayName("SK Gas API")
            .pathsToMatch("/api/v1/mi/scenario-workbench/**", "/api/v1/mi/fundamental-model/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOpenApiCustomizer(openApi -> openApi.info(
                new Info()
                    .title("SK Gas - Scenario Workbench & Fundamental Model API")
                    .description(description)
                    .version(SW_VERSION_BASE)
            ))
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    private String label(boolean isMock) {
        return isMock ? "🟡 Mock" : "🟢 DB";
    }

    @Bean
    public GroupedOpenApi etcApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                 @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("ETC API")
            .displayName("기타 API")
            .pathsToMatch("/api/**", "/saml2/**", "/oauth2/**")
            .pathsToExclude("/api/v1/mi/**", "/api/v1/admin/**", "/api/v1/test/**")
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
