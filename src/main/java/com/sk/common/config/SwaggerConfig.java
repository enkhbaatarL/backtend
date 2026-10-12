package com.sk.common.config;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.enums.ResultCode;
import com.sk.common.interceptor.McpUserContextInterceptor;
import com.sk.common.model.BaseResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;

@Profile({"local", "tech", "dev", "prod", "testdb"})
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private static final String APPLICATION_JSON = "application/json";
    private static final String BASE_RESPONSE_SCHEMA = "BaseResponse";
    private static final String BASE_RESPONSE_SCHEMA_REF = "#/components/schemas/" + BASE_RESPONSE_SCHEMA;
    // [MCP 인증] MCP API 문서의 Basic Auth 보안 스킴 이름
    private static final String MCP_SECURITY_SCHEME = "McpBasicAuth";

    // note: 안정화 후 투머치 문서 모두 삭제예정
    private static final String SW_VERSION_BASE = "v1.3.2";
    private static final DateTimeFormatter SW_DEPLOY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    public GlobalOpenApiCustomizer modelParamProposalNullableSchemaCustomizer() {
        return openApi -> {
            if (openApi.getOpenapi() == null || !openApi.getOpenapi().startsWith("3.1")
                || openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Schema<?> response = openApi.getComponents().getSchemas().get("CurrentModelParamProposalRes");
            if (response == null || response.getProperties() == null) {
                return;
            }
            // OpenAPI 3.1은 nullable을 직렬화하지 않으므로 enum/$ref도 null을 허용하는 합집합으로 표현한다.
            response.getProperties().replaceAll((name, property) -> {
                if (!Boolean.TRUE.equals(property.getNullable())) {
                    return property;
                }
                property.setNullable(null);
                Schema<?> nullSchema = new Schema<>();
                nullSchema.addType("null");
                return new Schema<>().description(property.getDescription()).anyOf(List.of(property, nullSchema));
            });
        };
    }

    @Bean
    public GroupedOpenApi miApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("MI API")
            .displayName("MI 서비스 API")
            .pathsToMatch("/api/v1/mi/**")
            .pathsToExclude("/api/v1/mi/next-gen/**", "/api/v1/mi/fundamental-model/**")
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

    @Bean
    public GroupedOpenApi nextGenApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                               @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {

        String swDeployDate = LocalDateTime.now().format(SW_DEPLOY_DATE_FORMATTER);

        // note: 안정화 후 투머치 문서 모두 삭제예정
        String description = """
  ## 📋 배포 정보

  | 항목 | 내용 |
  |------|------|
  | **버전** | %s |
  | **배포일** | %s |

  ---

  ## 수정사항
  <details open>
    <summary><strong>v1.3.2 (2026-10-08)</strong></summary>

  - [Event Keyword Alert — 신규 도메인] (태그 01-1)
    - 이벤트 키워드 알림 설정 API 추가 (Event List 툴바 알림 버튼 → 키워드 알림 설정 팝업)
      - 목록 조회 `GET /api/v1/mi/next-gen/event-keywords` — `keywordCount`/`maxKeywordCount`(20)/`keywords`
      - 등록 `POST /api/v1/mi/next-gen/event-keywords` — 정규화 후 2~30자, 사용자당 최대 20개, 중복 시 `RSC-003`
      - 알림 ON/OFF `PATCH /api/v1/mi/next-gen/event-keywords/{keywordSettingId}/notification-use`
      - 삭제 `DELETE /api/v1/mi/next-gen/event-keywords/{keywordSettingId}`

  </details>

  <details>
    <summary><strong>v1.3.1 (2026-10-05)</strong></summary>

  - [Scenario Design — 동일 조합 중복 생성 허용]
    - 이벤트 조합 설계 상태 조회 API 응답 교체 — `designStatus` 등 CTA 분기 필드 제거, 구성이 같은 그룹 목록(`sameCombinationGroupCount`/`sameCombinationGroups`) 제공 (이벤트 1개 선택 시 빈 목록)
    - 시나리오 그룹 생성 API: 동일 조합 그룹이 있어도 항상 새로 생성, 동일 조합 자동 재연결 제거, 요청 본문 `operationId`/`lastSequence` 제거
    - 시나리오 그룹 생성 스트림 재연결 API 추가 (`GET .../{scenarioGroupId}/creation/stream?lastSequence=`)
  - [Event]
    - 이벤트 연결 시나리오 그룹 목록 API: `creatorName`(생성자명)/`createdAt`(생성일시) 추가
    - 사용자 등록 이벤트 구분: 목록 `sourceType`/`mine` 필터 + 행 `sourceType`, 상세 `sourceType`/`registrantName`, 그룹 상세·설계 화면 `eventBundles[].sourceType` 추가

  </details>

  <details>
    <summary><strong>v1.3.0 (2026-08-18)</strong></summary>

  - [Scenario Design — v4 멀티 이벤트 전환]
    - 이벤트 조합 설계 상태 조회 API 추가 (`GET .../scenario-groups/design-status`) — 이벤트 목록 플로팅바 CTA 분기
    - 멀티 이벤트 기반 시나리오 그룹 생성 API 추가 (SSE, `POST .../scenario-groups/creation/stream`, 이벤트 1~5개) — 재연결 커서 쌍(`operationId`+`lastSequence`) 지원
    - 설계 화면 데이터 조회 API 추가 (`GET .../{scenarioGroupId}/presentation`) — 표/근거/생성·정련 상태/분기값 옵션 단일 조회
    - 정련 구독 SSE 경로 변경: `GET .../{scenarioGroupId}/design/stream` → `.../{scenarioGroupId}/refinement/stream`
    - SSE keep-alive 통일: `:hb` 약 20초 → `: keep-alive` 약 15초 (생성/정련 두 채널 동일)
    - 전체 초기화 API 응답 본문 제거 (초기화 후 설계 화면 데이터 조회 API 재조회)
    - v1 설계 3종(설계 시작 / 세션 상태 조회 / 분기항목 템플릿) Deprecated
  - [Event]
    - 이벤트 연결 시나리오 그룹 목록 API 추가 (`GET .../{eventId}/scenario-groups`, 그룹 N 펼침 패널)
    - 목록: 시나리오 표기 필드 정리 — `scenarioDesignStatus`/`designStep`/`occupied`/`occupantUserId·Name`/`scenarioGroupId` 제거, `scenarioGroupCount`만 제공 (0이면 설계 아이콘, 1 이상이면 그룹 N 버튼)
    - 상세: `relatedEventScenario`(단수) 제거 → `relatedEventScenarios`(그룹별 표 스택)로 통일, 행에 `scenarioRole` 역할 배지 추가, 셀에 `valueId`/`rationale`(근거) 추가
    - 실패 방치 그룹 제외 규칙을 목록/상세/패널/폴링에 일관 적용 (설계중인데 최신 생성 작업이 실패로 끝난 그룹은 미노출, 잠정 규칙)
    - 이벤트 시나리오 설계 가능 상태 조회 API Deprecated (이벤트 조합 설계 상태 조회 API로 대체)
  - [Scenario Group]
    - 그룹 상세: `baseEvent`(단수) 제거 → `eventBundles`(구성 이벤트 목록) + `evidence`(생성 근거 마크다운, '근거 보기') 추가
    - 시나리오 행 응답 전반(그룹 목록 / 개별 목록 / 그룹 상세 / 시나리오 상세)에 `scenarioRole`/`scenarioRoleLabel` 역할 배지 추가 (`ceiling` 상단 / `floor` 하단 / `mixed` 혼합, v4 대표 시나리오 3종만)
    - v4(definitions 기반) 그룹 표 렌더 지원 — 헤더/셀 근거 필드(`description`/`rationale`) 추가, 확률 0~1 스케일 자동 변환 (v1 그룹은 기존 그대로)
  - [공통]
    - 시나리오 그룹 `status`/`stage` 등 enum 직렬화를 소문자 code로 통일 (예: `CREATING` → `creating`) — 그룹 N 펼침 패널 응답 영향

  </details>

  <details>
    <summary><strong>v1.2.63 (2026-07-06)</strong></summary>

  - [Price Interpretation — 신규 도메인]
    - 시나리오별 가격전망 해석 API 추가: SSE 구독 / section 조회 / 생성 / 수정 / 초기화 (태그 04-1)
    - Agent 생성 콜백 수신 API 추가 (태그 04-2, `POST .../price-interpretations/callback`)
    - 시나리오 그룹 상세 응답에 가격전망 해석 section(`priceInterpretation`) embed
    - 생성은 증분 + 재실행: (수급모델 실행완료 AND (해석 미보유 / 실패 / 수급모델 재실행됨)) 시나리오만 대상
    - 수정은 last-write-wins, 초기화는 시나리오 개별 — Agent 원본 복원
  - [Swagger]
    - 데이터 소스 현황에 시나리오별 가격전망 해석 섹션 추가 (생성 API mock 시 지연 후 콜백 도착까지 시뮬레이션)

  </details>

  <details>
    <summary><strong>v1.2.62 (2026-06-18)</strong></summary>

  - [Event]
    - 이벤트 목록에 `occupied`(설계 점유 여부) · `occupantUserId`(점유자) 추가
  - [Scenario Design]
    - 설계 세션 SSE에 keep-alive 하트비트(`:hb`, 약 20초 주기) 추가 — 연결 유지용, FE 처리 불필요
  - [Scenario Group]
    - CME 벤치마크 그래프 조회 API 추가 (dedicated 전용, `GET .../scenario-groups/graph/cme-benchmark`)
  - [File Crypto]
    - `status` API 경로 수정
  - [Model Execution]
    - 모델 실행 접수, 모델 실행 상태 조회 살DB 연동

  </details>

  <details>
    <summary><strong>v1.2.5 (2026-06-17)</strong></summary>

  - [Scenario Group]
    - 그룹 그래프에 MI Base 기준선(`baseSeries`) 추가 (generated marker로 지정된 current MI Base의 latest completed run 가격)
    - 그래프 시리즈에 `status`(판정상태/폐기여부) · `modelRunning`(수급모델 실행중) 필드 추가
    - 그룹 삭제 시 하위 시나리오 + signpost 까지 함께 soft-delete (기존: signpost 제외)
  - [Scenario Design — 신규 도메인]
    - 설계 시작/상태 조회/분기항목 템플릿/검토 요청/Signpost 생성/확정/초기화 API 추가 (진행 단계 SSE 알림)
    - 데이터 소스 현황에 시나리오 설계 섹션 추가
  - [Event]
    - 이벤트 설계가능 조회 API 추가, 목록/상세에 라이브 설계 상태(설계중/설계완료) 반영

  </details>

  <details>
    <summary><strong>v1.2.4 (2026-06-16)</strong></summary>

  - [Fundamental Model]
    - Mock 인프라 전체 철거: 모든 Fundamental Model Case/Run API를 DB(real) 구현으로 단일화
    - Mock 서비스(`FundamentalModelCaseMockService`) · 라우팅 델리게이터(`FundamentalModelCaseServiceDelegator`) · Mock 토글 설정(`FundamentalModelMockProperties`, `mi.fundamental-model.mock.*`) 제거
    - [Mock 전용] Case 데이터 초기화 API(`POST .../fundamental-model/cases/reset`) 삭제 (데이터 소스 현황에서도 제외)
    - 앱 기동 시 mock seed 로딩 제거

  </details>

  <details>
  <summary><strong>v1.2.3 (2026-06-11)</strong></summary>

  - [Model Execution]
    - Price chart에 monthly/yearly series 필드 분리 추가
  - [Scenario Group]
    - 시나리오 상태 변경 시 그룹 status 재도출 (실현 시나리오 존재 시 realized, 실현 되돌림 시 created 환원)

  </details>

  <details>
  <summary><strong>v1.2.2 (2026-06-11)</strong></summary>

  - [Scenario Group]
    - 적용 완료된 DB schema(`04_incremental_apply_2506111441.sql`) 기준으로 Scenario 상태 매핑 정리
    - 삭제 컬럼(`feasibility_score`, `probability_level`, `judgment_status_code`) 참조 제거
    - 신규 `tmm_mi_scenario_m.status` 기반으로 시나리오 상태 조회/변경 처리
    - 외부 API 상태 계약은 기존 `pending` / `realized` / `discarded` 유지
    - DB 상태값 호환 매핑: `creating` / `created` / null / blank → `pending`, `realized` → `realized`, `discarded` → `discarded`
    - 상태 변경 저장 매핑: `pending` → DB `created`, `realized` → DB `realized`, `discarded` → DB `discarded`
    - 폐기 시나리오 확률 재분배 기준을 신규 DB `status = discarded`로 변경

  </details>

  <details>
  <summary><strong>v1.2.1 (2026-06-10)</strong></summary>

  - [Overview / Scenario / Design]
    - AS-IS API(Deprecated) 모두 삭제

  </details>

  <details>
  <summary><strong>v1.1.11 (2026-06-09)</strong></summary>

  - [Scenario Group]
    - 시나리오 판정 상태 일괄 변경 API 추가
    - 시나리오 그룹 삭제 API 추가

  </details>

  <details>
  <summary><strong>v1.1.10 (2026-06-09)</strong></summary>

  - [Model Execution]
    - 수급모델 실행 요청: parameter(JSON) 기반 → 입력 파일 3개 multipart 업로드 방식으로 변경
      - 요청 형식 `application/json` → `multipart/form-data` (`files` 파트, 입력 파일 3개)
      - 요청 DTO(`ModelExecutionSubmitReq` · `ModelExecutionTableReq` · `ModelExecutionTableRowReq`) 제거
    - 실행 접수 응답 / 실행 상태 응답에 업로드 파일 메타데이터 `files[]` 추가
      - `fileNo`(DB `tcm_filel.file_no` 매핑) · `fileIndex` · `originalFileName` · `contentType` · `size`
    - [Mock 전용] 수급모델 실행 데이터 초기화 API(`POST .../model-executions/reset`)를 데이터 소스 현황에 추가

  </details>

  <details>
  <summary><strong>v1.1.0 (2026-06-04)</strong></summary>

  - [Scenario Group — 신규 도메인]
    - 시나리오 그룹 목록(그룹 모드) / 개별 시나리오 목록(개별 모드) API 추가
    - 그룹 가격전망 그래프(단일 인덱스, `indexName`) / 그래프 엑셀 다운로드(전 인덱스 시트) API 추가
    - 시나리오 그룹 상세(요약 + 가정·실행가능성 표) / 개별 시나리오 상세(내러티브 · 주요 가정 · Signpost) API 추가
    - 시나리오 판정 상태 변경 API 추가 (대기 `pending` / 실현 `realized` / 폐기 `discarded`, PATCH)
    - [Mock 전용] 시나리오 상태 변경 초기화 API 추가 (`POST .../scenario-status/reset` — 변경분을 초기 설정값으로 복원)
  - [Deprecated]
    - Overview / Scenario / Design API deprecated 처리 (Scenario Group 도메인으로 대체)

  </details>

  <details>
  <summary><strong>v1.0.0 (2026-05-28)</strong></summary>

  - [이벤트 목록]
    - 조회 파라미터 `suggestedOnly` 추가 (시나리오화 추천 항목만 보기 토글)
    - `isScenarioSuggested` 필드 추가 (시나리오화 추천 여부)
    - `scenarioSuggestReason` 필드 추가 (시나리오화 추천 근거. 추천 아님일 때 null)
    - `indexImpacts[]`에 `impactTerm` / `impactTermLabel` 추가 (영향 시점: 단기 `short` / 중장기 `mid_long`)
  - [이벤트 상세]
    - `impactAnalysis.indexImpacts[]` 구조 변경: 인덱스별 `shortTerm` / `midLongTerm` (단기·중장기) 슬롯 + `analysisText` (인덱스별 분석 본문)
    - `driverTreeScoreboard` 구조 변경: 인덱스(JKM/TTF/HH)별 그룹(`indexName` + `drivers[]`)으로 묶고, 각 driver에 `shortTerm` / `midLongTerm` 슬롯 포함
    - `fundamentalDeepdive` 필드 제거
    - `relatedEventScenario` 현재 항상 null 반환 (관련 테이블 미준비)
  - [이벤트 관련 레코드]
    - `summarySourceId` 필드 추가 (요약 원문 식별자 PK)

  </details>

  <details>
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
            swDeployDate
        );

        return GroupedOpenApi.builder()
            .group("SK Gas API")
            .displayName("SK Gas API")
            .pathsToMatch("/api/v1/mi/next-gen/**", "/api/v1/mi/fundamental-model/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOpenApiCustomizer(openApi -> openApi.info(
                new Info()
                    .title("SK Gas - Next Gen MI & Fundamental Model API")
                    .description(description)
                    .version(SW_VERSION_BASE)
            ))
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi mcpApi(
        @Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
        @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer
    ) {
        return GroupedOpenApi.builder()
            .group("MCP API")
            .displayName("MCP API")
            .pathsToMatch("/api/v1/mcp/**")
            .addOpenApiCustomizer(this::customizeMcpOpenApi)
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    @Bean
    public GroupedOpenApi etcApi(@Qualifier("sortPathsOpenApiCustomizer") OpenApiCustomizer sortPathsOpenApiCustomizer,
                                 @Qualifier("apiOrderOperationCustomizer") OperationCustomizer apiOrderOperationCustomizer) {
        return GroupedOpenApi.builder()
            .group("ETC API")
            .displayName("기타 API")
            .pathsToMatch("/api/**", "/saml2/**", "/oauth2/**")
            .pathsToExclude("/api/v1/mi/**", "/api/v1/admin/**", "/api/v1/test/**", "/api/v1/mcp/**")
            .addOpenApiCustomizer(sortPathsOpenApiCustomizer)
            .addOperationCustomizer(apiOrderOperationCustomizer)
            .build();
    }

    private void customizeMcpOpenApi(OpenAPI openApi) {
        if (openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null) {
            openApi.getComponents().getSecuritySchemes().remove("BearerAuth");
        }
        registerBaseResponseSchema(openApi);
        // [MCP 인증] 인증 없음 → Basic Auth 를 MCP 문서 전체의 보안 요구로 등록
        openApi.getComponents().addSecuritySchemes(MCP_SECURITY_SCHEME,
            new SecurityScheme()
                .type(Type.HTTP)
                .scheme("basic")
                .description("관리자 화면에서 발급한 MCP 인증 ID 와 시크릿"));
        openApi.setSecurity(List.of(new SecurityRequirement().addList(MCP_SECURITY_SCHEME)));
        if (openApi.getPaths() == null) {
            return;
        }

        openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(operation -> {
                // [MCP 인증] operation 별 보안 요구도 Basic Auth
                operation.setSecurity(List.of(new SecurityRequirement().addList(MCP_SECURITY_SCHEME)));
                List<io.swagger.v3.oas.models.parameters.Parameter> parameters =
                    operation.getParameters() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(operation.getParameters());
                boolean userIdAlreadyPresent = parameters.stream()
                    .anyMatch(parameter -> "header".equals(parameter.getIn())
                        && McpUserContextInterceptor.X_USER_ID.equalsIgnoreCase(parameter.getName()));
                if (!userIdAlreadyPresent) {
                    parameters.add(0, new io.swagger.v3.oas.models.parameters.Parameter()
                        .name(McpUserContextInterceptor.X_USER_ID)
                        .in("header")
                        .required(true)
                        .description("위임할 MI Platform 사용자 로그인 ID. 같은 로그인 ID가 여러 사용자와 일치하면 첫 번째 조회 결과를 사용합니다.")
                        .schema(new StringSchema().example("mcp-user@sk.com")));
                }
                operation.setParameters(parameters);
                operation.getResponses().addApiResponse(
                    "400",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("요청 파라미터가 유효하지 않습니다. BaseResponse의 code는 REQ-003입니다.")
                        .content(mcpErrorContent(
                            ResultCode.Error.REQ_INVALID_PARAMETER,
                            "요청 파라미터 오류")));
                operation.getResponses().addApiResponse(
                    "401",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description(
                            "Basic Auth 인증정보가 없거나(AUTH-001) 틀렸습니다(AUTH-002). "
                                + "X-User-Id 값의 문제는 401이 아니라 403으로 응답합니다.")
                        .content(mcpErrorContent(
                            ResultCode.Error.AUTH_REQUIRED,
                            "인증 컨텍스트 생성 실패")));
                operation.getResponses().addApiResponse(
                    // [MCP 인증] 401 = Basic Auth 문제, 403 = X-User-Id 위임 불가(누락·사용자 없음·회사 불일치)
                    "403",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description(
                            "키 인증은 통과했으나 X-User-Id로 위임할 수 없습니다. "
                                + "X-User-Id가 누락·공백이거나, 해당 로그인 ID의 유효 사용자가 없거나, "
                                + "사용자의 회사가 인증 키의 회사와 다릅니다. BaseResponse의 code는 AUTH-007입니다.")
                        .content(mcpErrorContent(
                            ResultCode.Error.AUTH_ACCESS_DENIED,
                            "사용자 위임 불가")));
            });
    }

    private void registerBaseResponseSchema(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        ModelConverters.getInstance()
            .read(BaseResponse.class)
            .forEach(openApi.getComponents()::addSchemas);
    }

    private Content mcpErrorContent(ResultCode.Error error, String summary) {
        Schema<?> schema = new Schema<>().$ref(BASE_RESPONSE_SCHEMA_REF);
        Example example = new Example()
            .summary(summary)
            .value(errorExample(error));
        return new Content().addMediaType(
            APPLICATION_JSON,
            new MediaType()
                .schema(schema)
                .addExamples(error.name(), example));
    }

    private Map<String, Object> errorExample(ResultCode.Error error) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("status", false);
        example.put("code", error.getCode());
        example.put("message", error.getMessage());
        example.put("data", null);
        return example;
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
