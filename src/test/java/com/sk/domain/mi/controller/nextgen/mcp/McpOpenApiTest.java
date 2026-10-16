package com.sk.domain.mi.controller.nextgen.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.config.SwaggerConfig;
import com.sk.common.enums.ResultCode;
import com.sk.common.interceptor.McpUserContextInterceptor;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.mi.service.nextgen.EventGenerationService;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.EventSnapshotService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.shared.service.CommonService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = McpOpenApiTest.TestApplication.class,
    properties = {
        "spring.profiles.active=testdb,dedicated",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=false"
    })
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MCP generated OpenAPI 테스트")
class McpOpenApiTest {

    private static final String MCP_PREFIX = "/api/v1/mcp/mi/next-gen/";
    private static final String CME_PATH =
        "/api/v1/mcp/mi/next-gen/scenario-groups/graph/cme-benchmark";
    // [MCP Feedback] MCP 채널의 쓰기(POST) 경로
    private static final String USER_FEEDBACK_PATH = "/api/v1/mcp/mi/next-gen/user-feedback";
    // [MCP Event Generate] 이벤트 생성(POST) 경로
    private static final String EVENT_GENERATION_PATH = "/api/v1/mcp/mi/next-gen/events/generations";
    private static final Set<String> POST_PATHS = Set.of(USER_FEEDBACK_PATH, EVENT_GENERATION_PATH);
    private static final Set<String> EXPECTED_PATHS = Set.of(
        "/api/v1/mcp/mi/next-gen/events",
        "/api/v1/mcp/mi/next-gen/events/event-types/lv1",
        "/api/v1/mcp/mi/next-gen/events/{eventId}",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/outlooks",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/records",
        "/api/v1/mcp/mi/next-gen/scenario-groups",
        "/api/v1/mcp/mi/next-gen/scenario-groups/scenarios",
        "/api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}",
        "/api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/graph",
        "/api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/price-interpretations",
        "/api/v1/mcp/mi/next-gen/scenario-groups/{scenarioGroupId}/scenarios/{scenarioId}",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/scenario-groups",
        CME_PATH,
        // [MCP Snapshot] 이벤트 Snapshot 6건 추가 (13 → 19)
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/lookup",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/{snapshotId}",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/{snapshotId}/compare",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/{snapshotId}/records",
        "/api/v1/mcp/mi/next-gen/events/{eventId}/snapshots/{snapshotId}/outlooks",
        // [MCP Feedback] 사용자 피드백 등록 1건 추가 (19 → 20)
        USER_FEEDBACK_PATH,
        // [MCP Event Generate] 이벤트 생성 1건 추가 (20 → 21)
        EVENT_GENERATION_PATH);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private ScenarioGroupService scenarioGroupService;

    @MockitoBean
    private PriceInterpretationService priceInterpretationService;

    @MockitoBean
    private CmeBenchmarkService cmeBenchmarkService;

    @MockitoBean
    // [MCP Snapshot]
    private EventSnapshotService eventSnapshotService;

    @MockitoBean
    // [MCP Feedback]
    private CommonService commonService;

    @MockitoBean
    // [MCP Event Generate]
    private EventGenerationService eventGenerationService;

    @Test
    @DisplayName("MCP API 그룹에 GET 19건 + POST 2건, 필수 문자열 X-User-Id, Basic Auth 보안 요구를 노출한다")
    void exposesTwelveMcpOperationsWithOnlyUserDelegationHeader() throws Exception {
        JsonNode document = apiDocument("MCP API");
        JsonNode paths = document.path("paths");

        assertThat(fieldNames(paths)).containsExactlyInAnyOrderElementsOf(EXPECTED_PATHS);
        paths.fields().forEachRemaining(pathEntry -> {
            assertThat(pathEntry.getKey()).startsWith(MCP_PREFIX);
            // [MCP Feedback][MCP Event Generate] 피드백 등록·이벤트 생성만 POST, 나머지는 GET 만
            String method = POST_PATHS.contains(pathEntry.getKey()) ? "post" : "get";
            assertThat(fieldNames(pathEntry.getValue())).containsExactly(method);

            JsonNode operation = pathEntry.getValue().path(method);
            JsonNode userIdParameter = findParameter(operation, McpUserContextInterceptor.X_USER_ID);
            assertThat(userIdParameter.isMissingNode()).as(pathEntry.getKey()).isFalse();
            assertThat(userIdParameter.path("in").asText()).isEqualTo("header");
            assertThat(userIdParameter.path("required").asBoolean()).isTrue();
            assertThat(userIdParameter.path("description").asText())
                .contains("사용자 로그인 ID")
                .contains("첫 번째 조회 결과");
            assertThat(userIdParameter.path("schema").path("type").asText()).isEqualTo("string");
            assertThat(userIdParameter.path("schema").path("example").asText())
                .isEqualTo("mcp-user@sk.com");
            assertThat(findParameter(operation, "X-Company-No").isMissingNode()).isTrue();
            // [MCP 인증] 인증 없음 → Basic Auth 보안 요구와 403 응답 노출
            assertThat(operation.path("security").get(0).has("McpBasicAuth")).as(pathEntry.getKey()).isTrue();
            assertErrorResponse(operation, "400", "REQ_INVALID_PARAMETER", ResultCode.Error.REQ_INVALID_PARAMETER);
            assertErrorResponse(operation, "401", "AUTH_REQUIRED", ResultCode.Error.AUTH_REQUIRED);
            assertErrorResponse(operation, "403", "AUTH_ACCESS_DENIED", ResultCode.Error.AUTH_ACCESS_DENIED);
        });

        assertThat(document.toString()).doesNotContain("X-Company-No");
        assertThat(document.path("security").get(0).has("McpBasicAuth")).isTrue();
        JsonNode securitySchemes = document.path("components").path("securitySchemes");
        assertThat(securitySchemes.has("BearerAuth")).isFalse();
        assertThat(securitySchemes.path("McpBasicAuth").path("type").asText()).isEqualTo("http");
        assertThat(securitySchemes.path("McpBasicAuth").path("scheme").asText()).isEqualTo("basic");
    }

    @Test
    @DisplayName("CME operation은 SK Gas 전용 서버 경고를 생성 OpenAPI에 노출한다")
    void exposesDedicatedServerWarningForCme() throws Exception {
        JsonNode operation = apiDocument("MCP API")
            .path("paths")
            .path(CME_PATH)
            .path("get");

        assertThat(operation.path("description").asText())
            .contains("SK Gas 전용 서버 전용")
            .contains("일반 서버의 응답은 신뢰할 수 없습니다");
    }

    @Test
    @DisplayName("그래프·가격전망 해석 operation은 빈 결과의 판단 기준(수급모델 실행 상태)을 노출한다")
    void exposesEmptyResultGuidanceForGraphAndInterpretations() throws Exception {
        JsonNode paths = apiDocument("MCP API").path("paths");

        assertThat(paths.path(MCP_PREFIX + "scenario-groups/{scenarioGroupId}/graph").path("get").path("description").asText())
            .contains("executed = false, modelRunning = false")
            .contains("미실행");
        assertThat(paths.path(MCP_PREFIX + "scenario-groups/{scenarioGroupId}/price-interpretations").path("get").path("description").asText())
            .contains("generatable = false, interpretations = []")
            .contains("scenarioSeries[].executed");
    }

    @Test
    // [MCP Snapshot] OpenAPI description = MCP tool 설명이므로 조회 주의사항 노출을 고정
    @DisplayName("이벤트 Snapshot operation은 조회 시 주의사항(갱신 후 상태·changedFields·사본 기준)을 노출한다")
    void exposesSnapshotGuidance() throws Exception {
        JsonNode paths = apiDocument("MCP API").path("paths");
        String snapshots = MCP_PREFIX + "events/{eventId}/snapshots";

        assertThat(operation(paths, snapshots, "getEventSnapshots").path("description").asText())
            .contains("최신 Snapshot 과 현재 Event(이벤트 상세 조회)가 다를 수 있습니다")
            .contains("changedFields")
            .contains("isEventDeleted 는 저장 당시 Event 상태");
        assertThat(operation(paths, snapshots + "/lookup", "lookupEventSnapshot").path("description").asText())
            .contains("versionNo 와 asOf 는 동시에 줄 수 없습니다")
            .contains("found=false");
        assertThat(operation(paths, snapshots + "/{snapshotId}/compare", "compareEventSnapshot").path("description").asText())
            .contains("recordedChangedFields 는 파이프라인이 기록한 값")
            .contains("comparable=false");
        assertThat(operation(paths, snapshots + "/{snapshotId}/records", "getEventSnapshotRecords").path("description").asText())
            .contains("LLM 에 전달된 근거 Record")
            .contains("snapshotCreatedAt");
        assertThat(operation(paths, snapshots + "/{snapshotId}/outlooks", "getEventSnapshotOutlooks").path("description").asText())
            .contains("활성 기관 전망 전체의 사본");
        assertThat(operation(paths, snapshots + "/{snapshotId}", "getEventSnapshot").path("description").asText())
            .contains("RSC-001");
    }

    @Test
    // [MCP Feedback] requestBody 필드 설명·허용값 = MCP tool 인자 설명·선택지
    @DisplayName("사용자 피드백 등록 operation은 내용만 필수이고 유형·대상 종류의 허용값과 선택 기준을 노출한다")
    void exposesUserFeedbackGuidance() throws Exception {
        JsonNode document = apiDocument("MCP API");
        JsonNode operation = document.path("paths").path(USER_FEEDBACK_PATH).path("post");
        assertThat(operation.path("operationId").asText()).isEqualTo("createMcpUserFeedback");
        assertThat(operation.path("description").asText())
            .contains("명시적으로 요청한 경우에만")
            .contains("정확하지 않아도 등록됩니다");

        String schemaRef = operation.path("requestBody").path("content").path("application/json").path("schema").path("$ref").asText();
        JsonNode schema = document.path("components").path("schemas").path(schemaRef.substring(schemaRef.lastIndexOf('/') + 1));
        assertThat(schema.path("required").toString()).isEqualTo("[\"feedbackContent\"]");

        JsonNode properties = schema.path("properties");
        assertThat(properties.path("feedbackTypeCd").path("enum").toString())
            .isEqualTo("[\"agent_output\",\"ux_ui\",\"feature_suggestion\",\"etc\"]");
        assertThat(properties.path("feedbackTypeCd").path("description").asText()).contains("내용·데이터 관련은 대부분 이것");
        assertThat(properties.path("feedbackSectionNm").path("enum").toString())
            .isEqualTo("[\"Event\",\"Scenario Group\",\"Scenario\"]");
        assertThat(properties.path("targetId").path("type").asText()).isEqualTo("integer");
        assertThat(properties.path("targetId").path("description").asText())
            .contains("eventId")
            .contains("scenarioGroupId")
            .contains("scenarioId")
            .contains("추측하지 말고 생략");
    }

    @Test
    // [MCP Event Generate] description·requestBody 설명 = MCP tool 설명·인자 설명
    @DisplayName("이벤트 생성 operation은 명시적 요청 시에만 호출·즉시 저장·중복 생성 주의와 입력 문장 필수를 노출한다")
    void exposesEventGenerationGuidance() throws Exception {
        JsonNode document = apiDocument("MCP API");
        JsonNode operation = document.path("paths").path(EVENT_GENERATION_PATH).path("post");
        assertThat(operation.path("operationId").asText()).isEqualTo("generateEvents");
        assertThat(operation.path("description").asText())
            .contains("명시적으로 요청한 경우에만")
            .contains("중복 생성됩니다")
            .contains("miEventId");

        String schemaRef = operation.path("requestBody").path("content").path("application/json").path("schema").path("$ref").asText();
        JsonNode schema = document.path("components").path("schemas").path(schemaRef.substring(schemaRef.lastIndexOf('/') + 1));
        assertThat(schema.path("required").toString()).isEqualTo("[\"userQuery\"]");
        assertThat(fieldNames(schema.path("properties"))).containsExactlyInAnyOrder("userQuery", "dateFrom", "dateTo");
        assertThat(schema.path("properties").path("dateFrom").path("description").asText()).contains("YYYY-MM-DD");
    }

    @Test
    @DisplayName("이벤트 목록·상세는 게시 완료 조회 범위와 기존 미존재 오류를 설명한다")
    void exposesPublishedOnlyEventQueries() throws Exception {
        JsonNode paths = apiDocument("MCP API").path("paths");
        JsonNode list = paths.path(MCP_PREFIX + "events").path("get");
        JsonNode detail = paths.path(MCP_PREFIX + "events/{eventId}").path("get");

        assertThat(list.path("description").asText())
            .contains("게시 완료(PUBLISHED)", "신호 이벤트", "미게시(DRAFT)", "게시 상태가 없는");
        assertThat(detail.path("description").asText())
            .contains("게시 완료(PUBLISHED)", "RSC-001");
        assertThat(findParameter(list, "publishStatus").isMissingNode()).isTrue();
        assertThat(findParameter(list, "publish_status").isMissingNode()).isTrue();
    }

    @Test
    // [MCP Event Generate] description·requestBody 설명 = MCP tool 설명·인자 설명
    @DisplayName("이벤트 생성 operation은 명시적 요청 시에만 호출·즉시 저장·중복 생성 주의와 입력 문장 필수를 노출한다")
    void exposesEventGenerationGuidance() throws Exception {
        JsonNode document = apiDocument("MCP API");
        JsonNode operation = document.path("paths").path(EVENT_GENERATION_PATH).path("post");
        assertThat(operation.path("operationId").asText()).isEqualTo("generateEvents");
        assertThat(operation.path("description").asText())
            .contains("명시적으로 요청한 경우에만")
            .contains("중복 생성됩니다")
            .contains("miEventId");

        String schemaRef = operation.path("requestBody").path("content").path("application/json").path("schema").path("$ref").asText();
        JsonNode schema = document.path("components").path("schemas").path(schemaRef.substring(schemaRef.lastIndexOf('/') + 1));
        assertThat(schema.path("required").toString()).isEqualTo("[\"userQuery\"]");
        assertThat(fieldNames(schema.path("properties"))).containsExactlyInAnyOrder("userQuery", "dateFrom", "dateTo");
        assertThat(schema.path("properties").path("dateFrom").path("description").asText()).contains("YYYY-MM-DD");
    }

    private JsonNode operation(JsonNode paths, String path, String operationId) {
        JsonNode operation = paths.path(path).path("get");
        assertThat(operation.path("operationId").asText()).as(path).isEqualTo(operationId);
        return operation;
    }

    @Test
    @DisplayName("MCP 경로는 ETC와 SK Gas 기존 그룹에 중복되지 않는다")
    void excludesMcpPathsFromExistingGroups() throws Exception {
        assertNoMcpPath(apiDocument("ETC API"));
        assertNoMcpPath(apiDocument("SK Gas API"));
    }

    private JsonNode apiDocument(String group) throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs/{group}", group))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(document);
    }

    private void assertErrorResponse(
        JsonNode operation,
        String httpStatus,
        String exampleName,
        ResultCode.Error expectedError
    ) {
        JsonNode response = operation.path("responses").path(httpStatus);
        assertThat(response.path("description").asText()).contains(expectedError.getCode());

        JsonNode mediaType = response.path("content").path("application/json");
        assertThat(mediaType.path("schema").path("$ref").asText()).endsWith("/BaseResponse");
        JsonNode value = mediaType.path("examples").path(exampleName).path("value");
        assertThat(value.path("status").asBoolean()).isFalse();
        assertThat(value.path("code").asText()).isEqualTo(expectedError.getCode());
        assertThat(value.path("message").asText()).isEqualTo(expectedError.getMessage());
        assertThat(value.path("data").isNull()).isTrue();
    }

    private JsonNode findParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equalsIgnoreCase(parameter.path("name").asText())) {
                return parameter;
            }
        }
        return objectMapper.missingNode();
    }

    private void assertNoMcpPath(JsonNode document) {
        document.path("paths").fieldNames().forEachRemaining(path ->
            assertThat(path).doesNotStartWith("/api/v1/mcp/"));
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class,
        Saml2RelyingPartyAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    @Import({
        SwaggerConfig.class,
        McpEventDedicatedController.class,
        McpScenarioGroupDedicatedController.class,
        // [MCP Snapshot]
        McpEventSnapshotDedicatedController.class,
        // [MCP Feedback]
        McpUserFeedbackDedicatedController.class
    })
    static class TestApplication {
    }
}
