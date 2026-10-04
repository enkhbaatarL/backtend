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
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
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
        CME_PATH);

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

    @Test
    @DisplayName("MCP API 그룹에 GET 13건과 필수 문자열 X-User-Id만 노출한다")
    void exposesTwelveMcpOperationsWithOnlyUserDelegationHeader() throws Exception {
        JsonNode document = apiDocument("MCP API");
        JsonNode paths = document.path("paths");

        assertThat(fieldNames(paths)).containsExactlyInAnyOrderElementsOf(EXPECTED_PATHS);
        paths.fields().forEachRemaining(pathEntry -> {
            assertThat(pathEntry.getKey()).startsWith(MCP_PREFIX);
            assertThat(fieldNames(pathEntry.getValue())).containsExactly("get");

            JsonNode operation = pathEntry.getValue().path("get");
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
            assertThat(operation.path("security").isMissingNode()
                || operation.path("security").isEmpty()).isTrue();
            assertErrorResponse(operation, "400", "REQ_INVALID_PARAMETER", ResultCode.Error.REQ_INVALID_PARAMETER);
            assertErrorResponse(operation, "401", "AUTH_REQUIRED", ResultCode.Error.AUTH_REQUIRED);
        });

        assertThat(document.toString()).doesNotContain("X-Company-No");
        assertThat(document.path("security").isMissingNode() || document.path("security").isEmpty()).isTrue();
        assertThat(document.path("components").path("securitySchemes").has("BearerAuth")).isFalse();
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
        McpScenarioGroupDedicatedController.class
    })
    static class TestApplication {
    }
}
