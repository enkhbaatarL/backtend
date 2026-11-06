package com.sk.domain.mi.controller.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.common.aop.PermissionAspect;
import com.sk.common.handler.GlobalExceptionHandler;
import com.sk.domain.mi.service.nextgen.EventRecommendationService;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.EventVerificationAccessService;
import com.sk.domain.mi.service.nextgen.EventVerificationService;
import com.sk.domain.shared.client.config.EventVerificationClientConfig;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import com.sk.domain.shared.service.permissions.GoodsNestedEntityResolver;
import com.sk.domain.shared.service.permissions.PermissionMessageFormatter;
import com.sk.domain.shared.service.permissions.PermissionStrategyFactory;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** MVC + real outbound HTTP + real permission rules, without DB/Redis/web-search dependencies. */
class EventVerificationTest {
    private static final String ENDPOINT = "/api/v1/mi/next-gen/events/verifications";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> sentBody = new AtomicReference<>();
    private final AtomicReference<String> sentAuthorization = new AtomicReference<>();
    private final AtomicReference<String> sentMethod = new AtomicReference<>();
    private HttpServer agent;
    private ValidatorFactory validation;
    private CloseableHttpClient httpClient;
    private MiEventRepository repository;
    private AuthorizationService authorization;
    private MockMvc mvc;
    private volatile int agentStatus = 200;
    private volatile String agentBody;
    private volatile long delayMillis;

    @BeforeEach
    void setUp() throws Exception {
        validation = Validation.buildDefaultValidatorFactory();
        repository = mock(MiEventRepository.class);
        when(repository.findPublishedIdsForVerification(anyCollection())).thenAnswer(invocation ->
            new ArrayList<>(invocation.<Collection<Long>>getArgument(0)));
        authorization = mock(AuthorizationService.class);
        when(authorization.getCurrentRole()).thenReturn(Role.USER);
        when(authorization.getCompanyCd()).thenReturn(PermissionCondition.AllowedCompany.SKGAS.getCode());
        when(authorization.getCurrentJwt()).thenReturn(Jwt.withTokenValue("verified-token").header("alg", "RS256")
            .subject("10603").claim("userNo", 123).build());
        agentBody = response(List.of(5L), List.of("no_evidence")).toString();
        agent = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Prefix and trailing-slash handling must survive the typed HTTP interface.
        agent.createContext("/gateway/scenario/events/verifications", exchange -> {
            calls.incrementAndGet();
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sentAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sentMethod.set(exchange.getRequestMethod());
            try {
                if (delayMillis > 0) Thread.sleep(delayMillis);
                byte[] body = agentBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(agentStatus, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        agent.start();
        httpClient = new EventVerificationClientConfig().eventVerificationHttpClient();
        mvc = mvc(Duration.ofSeconds(2));
    }

    private MockMvc mvc(Duration timeout) {
        var api = new EventVerificationClientConfig().eventVerificationAgentApi(httpClient,
            "http://127.0.0.1:" + agent.getAddress().getPort() + "/gateway/scenario/", Duration.ofSeconds(1), timeout);
        var service = new EventVerificationService(api, new EventVerificationAccessService(repository), validation.getValidator(), authorization);
        var proxy = new AspectJProxyFactory(new EventVerificationController(service));
        proxy.addAspect(new PermissionAspect(new PermissionStrategyFactory(authorization),
            mock(GoodsNestedEntityResolver.class), mock(PermissionMessageFormatter.class)));
        return MockMvcBuilders.standaloneSetup((EventVerificationController) proxy.getProxy())
            .setControllerAdvice(new EventVerificationExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        agent.stop(0);
        httpClient.close();
        validation.close();
    }

    private ObjectNode response(List<Long> ids, List<String> reasons) {
        var root = mapper.createObjectNode().put("checkedAt", "2026-11-04T01:00:00Z");
        var items = root.putArray("items");
        for (int i = 0; i < ids.size(); i++) {
            String reason = reasons.get(i);
            String itemStatus = switch (reason) {
                case "verified", "no_contradiction" -> "pass";
                case "contradicted", "outdated" -> "fail";
                default -> "warning";
            };
            var item = items.addObject().put("miEventId", ids.get(i)).put("title", "Event 제목")
                .put("sourceUpdatedAt", "2026-11-03T09:00:00").put("status", itemStatus)
                .put("reasonCode", reason).put("review", "검증 결과를 확인해 주세요.")
                .put("verificationId", new java.util.UUID(0, ids.get(i)).toString()).put("expiresAt", "2026-11-04T01:30:00.123456Z");
            item.putArray("links");
            var checks = item.putArray("checks");
            if (!Set.of("no_evidence", "search_error", "verification_error").contains(reason)) {
                for (String aspect : List.of("entity", "location", "time", "quantity", "freshness", "attribution")) {
                    var check = checks.addObject().put("aspect", aspect)
                        .put("verdict", "review_required".equals(reason) ? "unverified" : "matched")
                        .put("eventClaim", "Event 내용").put("finding", "검색 근거")
                        .putNull("cautionReason").putNull("qualificationQuote");
                    check.putArray("sourceUrls").add("https://example.com/announcement");
                }
                ((ObjectNode) checks.get(0)).put("verdict", switch (reason) {
                    case "contradicted", "outdated" -> reason;
                    case "review_required" -> "unverified";
                    case "no_contradiction" -> "caution";
                    default -> "matched";
                });
                item.withArray("links").add("https://example.com/announcement");
            }
        }
        return root;
    }

    @Test
    void forwardsAllIdsAndKeepsMixedResultsOrderAndTimestamps() throws Exception {
        agentBody = response(List.of(12L, 5L, 7L, 8L, 9L),
            List.of("verified", "search_error", "contradicted", "outdated", "verification_error")).toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer UI-only-token").content("{\"miEventIds\":[12,5,7,8,9]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
            .andExpect(jsonPath("$.data.items[0].status").value("pass"))
            .andExpect(jsonPath("$.data.items[1].reasonCode").value("search_error"))
            .andExpect(jsonPath("$.data.items[2].status").value("fail"))
            .andExpect(jsonPath("$.data.items[4].checks").isEmpty()).andReturn();
        assertThat(mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data"))
            .isEqualTo(mapper.readTree(agentBody));
        assertThat(mapper.readTree(sentBody.get())).isEqualTo(mapper.readTree("{\"miEventIds\":[12,5,7,8,9],\"actorUsrLognId\":\"10603\"}"));
        assertThat(sentAuthorization.get()).isNull();
        assertThat(sentMethod.get()).isEqualTo("POST");
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"verified", "no_contradiction", "review_required", "no_evidence", "search_error", "verification_error", "contradicted", "outdated"})
    void preservesEveryReasonCodeAsHttp200(String reason) throws Exception {
        agentBody = response(List.of(5L), List.of(reason)).toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].reasonCode").value(reason));
    }

    @Test
    void passesMixedChecksWithoutDroppingCautionsOrQualifications() throws Exception {
        var body = response(List.of(5L), List.of("no_contradiction"));
        var checks = body.withArray("items").get(0).withArray("checks");
        for (int i = 0; i < 3; i++) ((ObjectNode) checks.get(i)).put("verdict", "matched");
        ((ObjectNode) checks.get(3)).put("verdict", "caution").put("cautionReason", "source_conflict")
            .put("finding", "기관별 수치 범위가 다릅니다.");
        ((ObjectNode) checks.get(4)).put("verdict", "caution").put("cautionReason", "as_of_limitation")
            .put("qualificationQuote", "2026-11-06 기준이며 이후 변동은 확인하지 못했습니다.");
        ((ObjectNode) checks.get(5)).put("verdict", "unverified").put("finding", "발표 주체를 확인하지 못했습니다.");
        agentBody = body.toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].status").value("pass"))
            .andExpect(jsonPath("$.data.items[0].reasonCode").value("no_contradiction")).andReturn();
        assertThat(mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data"))
            .isEqualTo(mapper.readTree(agentBody));
    }

    @ParameterizedTest
    @ValueSource(strings = {"warning", "fail"})
    void noContradictionMustUsePublicPassGrade(String grade) throws Exception {
        var body = response(List.of(5L), List.of("no_contradiction"));
        ((ObjectNode) body.withArray("items").get(0)).put("status", grade);
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void olderChecksWithoutOptionalCautionFieldsAreStillAccepted() throws Exception {
        var body = response(List.of(5L), List.of("verified"));
        for (var check : body.withArray("items").get(0).withArray("checks")) {
            ((ObjectNode) check).remove(List.of("cautionReason", "qualificationQuote"));
        }
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].checks[0].cautionReason").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "2026-11-03T09:00:00", "2026-11-03T09:00:00+09:00"})
    void preservesNullableOrZonelessSourceTime(String value) throws Exception {
        var body = response(List.of(5L), List.of("no_evidence"));
        var item = (ObjectNode) body.withArray("items").get(0);
        if (value.equals("null")) item.putNull("sourceUpdatedAt");
        else item.put("sourceUpdatedAt", value);
        agentBody = body.toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isOk()).andReturn();
        assertThat(mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data"))
            .isEqualTo(mapper.readTree(agentBody));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{", "{\"miEventIds\":null}", "{\"miEventIds\":5}",
        "{\"miEventIds\":[]}", "{\"miEventIds\":[1,2,3,4,5,6]}", "{\"miEventIds\":[5,5]}",
        "{\"miEventIds\":[0]}", "{\"miEventIds\":[-5]}", "{\"miEventIds\":[null]}",
        "{\"miEventIds\":[\"5\"]}", "{\"miEventIds\":[1.5]}", "{\"miEventIds\":[1.0]}",
        "{\"miEventIds\":[true]}", "{\"miEventIds\":[9223372036854775808]}",
        "{\"miEventIds\":[5],\"extra\":true}", "{\"mi_event_ids\":[5]}"})
    void rejectsInvalidInputBeforeDbOrAgent(String body) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("REQ-003"));
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void unavailableSelectionRejectsWholeRequestBeforeSearch() throws Exception {
        when(repository.findPublishedIdsForVerification(anyCollection())).thenReturn(List.of(5L));
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5,12]}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.data").isEmpty());
        assertThat(calls.get()).isZero();
    }

    @Test
    void otherCompanyUserIsDeniedBeforeDbOrAgent() throws Exception {
        when(authorization.getCompanyCd()).thenReturn("other-company");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void adminWithoutThirdPartyAccessIsDenied() throws Exception {
        when(authorization.getCurrentRole()).thenReturn(Role.ADMIN);
        when(authorization.isThirdPartyAccessAllowed()).thenReturn(false);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void authorizedAdminCanVerify() throws Exception {
        when(authorization.getCurrentRole()).thenReturn(Role.ADMIN);
        when(authorization.isThirdPartyAccessAllowed()).thenReturn(true);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 422, 500, 502, 503, 504})
    void mapsWholeRequestErrorsWithoutRetryOrLeakingBody(int upstreamStatus) throws Exception {
        agentStatus = upstreamStatus;
        agentBody = upstreamStatus == 422 ? "{\"detail\":[{\"msg\":\"internal-sensitive-detail\"}]}"
            : "{\"status\":false,\"message\":\"internal-sensitive-detail\",\"data\":{\"error_code\":\"X\",\"error_name\":\"X\"}}";
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().is(upstreamStatus)).andExpect(jsonPath("$.status").value(false))
            .andExpect(jsonPath("$.data").isEmpty()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("internal-sensitive-detail");
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "json", "missing", "wrongId", "missingItem", "reversed", "status", "reason",
        "checkedAt", "nonUtc", "review", "checkCount", "duplicateAspect", "verdict", "incompleteChecks"})
    void invalidSuccessIsNotReportedAsAnEventVerdict(String defect) throws Exception {
        var body = response(List.of(5L, 12L), List.of("verified", "no_evidence"));
        var item = (ObjectNode) body.withArray("items").get(0);
        switch (defect) {
            case "wrongId" -> item.put("miEventId", 77L);
            case "missingItem" -> body.withArray("items").remove(1);
            case "reversed" -> { item.put("miEventId", 12L); ((ObjectNode) body.withArray("items").get(1)).put("miEventId", 5L); }
            case "status" -> item.put("status", "warning");
            case "reason" -> item.put("reasonCode", "unknown");
            case "checkedAt" -> body.put("checkedAt", "invalid");
            case "nonUtc" -> body.put("checkedAt", "2026-11-04T09:00:00+09:00");
            case "review" -> item.put("review", "x".repeat(301));
            case "checkCount" -> item.withArray("checks").remove(0);
            case "duplicateAspect" -> ((ObjectNode) item.withArray("checks").get(0)).put("aspect", "location");
            case "verdict" -> ((ObjectNode) item.withArray("checks").get(0)).put("verdict", "unknown");
            case "incompleteChecks" -> { item.put("status", "warning"); item.put("reasonCode", "search_error"); }
            default -> { }
        }
        agentBody = switch (defect) { case "empty" -> ""; case "json" -> "not-json"; case "missing" -> "{}"; default -> body.toString(); };
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5,12]}"))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("API-004"));
    }

    @Test
    void connectionFailureIs502() throws Exception {
        agent.stop(0);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("API-003"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingId", "invalidId", "duplicateIds", "missingExpiry", "invalidExpiry", "beforeCheckedAt"})
    void rejectsUnusableVerificationReferences(String defect) throws Exception {
        var body = response(List.of(5L, 12L), List.of("verified", "no_evidence"));
        var item = (ObjectNode) body.withArray("items").get(0);
        switch (defect) {
            case "missingId" -> item.remove("verificationId");
            case "invalidId" -> item.put("verificationId", "invalid");
            case "duplicateIds" -> item.set("verificationId", body.withArray("items").get(1).get("verificationId"));
            case "missingExpiry" -> item.remove("expiresAt");
            case "invalidExpiry" -> item.put("expiresAt", "invalid");
            case "beforeCheckedAt" -> item.put("expiresAt", "2026-11-04T00:00:00Z");
        }
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5,12]}"))
            .andExpect(status().isBadGateway());
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void verificationActorCannotBeProvidedByClient() throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content("{\"miEventIds\":[5],\"actorUsrLognId\":\"attacker\"}"))
            .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void serviceCredentialCannotCreateUserVerificationHistory() throws Exception {
        when(authorization.getCurrentJwt()).thenReturn(Jwt.withTokenValue("service").header("alg", "RS256")
            .subject("client-id").build());
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void readTimeoutIs504() throws Exception {
        delayMillis = 500;
        mvc(Duration.ofMillis(100)).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("API-002"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void scoped422HandlerDoesNotChangeRecommendationValidation() throws Exception {
        var existingService = mock(EventRecommendationService.class);
        var existingMvc = MockMvcBuilders.standaloneSetup(new EventController(mock(EventService.class), existingService))
            .setControllerAdvice(new EventVerificationExceptionHandler(), new GlobalExceptionHandler()).build();
        existingMvc.perform(post("/api/v1/mi/next-gen/events/recommendations")
                .contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[]}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(existingService);
    }
}
