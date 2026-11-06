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
import com.sk.domain.mi.service.nextgen.EventVerificationAccessService;
import com.sk.domain.mi.service.nextgen.EventVerificationUpdateService;
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
import java.util.List;
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
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/** MVC + permission aspect + real outbound HTTP; no DB writes or live LLM calls. */
class EventVerificationUpdateTest {
    private static final String ENDPOINT = "/api/v1/mi/next-gen/events/5/verification-updates";
    private static final String VERIFICATION_ID = "12345678-1234-4234-8234-123456789abc";
    private static final String REQUEST = "{\"verificationId\":\"" + VERIFICATION_ID + "\"}";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> sentBody = new AtomicReference<>();
    private final AtomicReference<String> sentMethod = new AtomicReference<>();
    private final AtomicReference<String> sentAuthorization = new AtomicReference<>();
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
        when(repository.findPublishedIdsForVerification(anyCollection())).thenReturn(List.of(5L));
        authorization = mock(AuthorizationService.class);
        when(authorization.getCurrentRole()).thenReturn(Role.USER);
        when(authorization.getCompanyCd()).thenReturn(PermissionCondition.AllowedCompany.SKGAS.getCode());
        when(authorization.getCurrentJwt()).thenReturn(Jwt.withTokenValue("verified-token").header("alg", "RS256")
            .subject("10603").claim("userNo", 123).build());
        agentBody = response("updated").toString();
        agent = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        agent.createContext("/gateway/scenario/events/5/verification-updates", exchange -> {
            calls.incrementAndGet();
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sentMethod.set(exchange.getRequestMethod());
            sentAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
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
        var api = new EventVerificationClientConfig().eventVerificationUpdateAgentApi(httpClient,
            "http://127.0.0.1:" + agent.getAddress().getPort() + "/gateway/scenario/", Duration.ofSeconds(1), timeout);
        var service = new EventVerificationUpdateService(api, new EventVerificationAccessService(repository),
            authorization, validation.getValidator(), mapper);
        var proxy = new AspectJProxyFactory(new EventVerificationUpdateController(service));
        proxy.addAspect(new PermissionAspect(new PermissionStrategyFactory(authorization),
            mock(GoodsNestedEntityResolver.class), mock(PermissionMessageFormatter.class)));
        return MockMvcBuilders.standaloneSetup((EventVerificationUpdateController) proxy.getProxy())
            .setControllerAdvice(new EventVerificationUpdateExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        agent.stop(0);
        httpClient.close();
        validation.close();
    }

    private String withVerificationId(String json) throws Exception {
        try {
            var body = mapper.readTree(json);
            if (body instanceof ObjectNode object && !object.has("verificationId")) {
                object.put("verificationId", VERIFICATION_ID);
            }
            return body.toString();
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return json;
        }
    }

    private ObjectNode verification(String status) {
        var item = mapper.createObjectNode().put("miEventId", 5).put("title", "Event 제목")
            .put("sourceUpdatedAt", "2026-11-06T09:00:00.123456+09:00").put("status", status)
            .put("reasonCode", switch (status) { case "pass" -> "verified"; case "fail" -> "contradicted"; default -> "no_evidence"; })
            .put("review", "검증 내용").put("verificationId", VERIFICATION_ID)
            .put("expiresAt", "2026-11-06T01:30:00.123456Z");
        item.putArray("links").add("https://example.com/news/123");
        var checks = item.putArray("checks");
        if (!"warning".equals(status)) {
            for (String aspect : List.of("entity", "location", "time", "quantity", "freshness", "attribution")) {
                checks.addObject().put("aspect", aspect).put("verdict", "fail".equals(status) ? "contradicted" : "matched")
                    .put("eventClaim", "주장").put("finding", "근거")
                    .putNull("cautionReason").putNull("qualificationQuote")
                    .putArray("sourceUrls").add("https://example.com/news/123");
            }
        }
        return item;
    }

    private ObjectNode response(String updateStatus) {
        var body = mapper.createObjectNode().put("miEventId", 5).put("checkedAt", "2026-11-06T01:00:00Z")
            .put("verificationId", VERIFICATION_ID).put("updateStatus", updateStatus).put("message", "처리 결과")
            .put("updatedAt", "2026-11-06T10:00:00.123456+09:00");
        body.set("verification", verification(switch (updateStatus) { case "updated" -> "fail"; case "unchanged" -> "pass"; default -> "warning"; }));
        body.putNull("postVerification");
        body.putNull("miEventSnapshotId");
        body.putNull("snapshotVersionNo");
        body.putNull("previousMiEventSnapshotId");
        body.putArray("changedFields");
        body.putArray("changes");
        if ("updated".equals(updateStatus)) {
            body.set("postVerification", verification("pass").putNull("verificationId").putNull("expiresAt"));
            body.put("miEventSnapshotId", 91).put("snapshotVersionNo", 1);
            body.withArray("changedFields").add("key_bullets_json");
            var change = body.withArray("changes").addObject().put("field", "key_bullets_json").put("reason", "공식 발표 반영");
            change.putArray("before").add("기존 내용");
            change.putArray("after").add("수정 내용");
            change.putArray("sourceUrls").add("https://example.com/news/123");
        }
        return body;
    }

    @ParameterizedTest
    @ValueSource(strings = {"updated", "unchanged", "skipped"})
    void preservesAllOutcomesSnapshotDiffAndTimestamps(String updateStatus) throws Exception {
        agentBody = response(updateStatus).toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer UI-only-token").content(withVerificationId("{\"expectedUpdatedAt\":\"2026-11-06T09:00:00.123456+09:00\"}")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.updateStatus").value(updateStatus)).andReturn();
        assertThat(mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data"))
            .isEqualTo(mapper.readTree(agentBody));
        assertThat(mapper.readTree(sentBody.get())).isEqualTo(mapper.readTree(withVerificationId(
            "{\"actorUsrLognId\":\"10603\",\"expectedUpdatedAt\":\"2026-11-06T09:00:00.123456+09:00\"}")));
        assertThat(sentMethod.get()).isEqualTo("POST");
        assertThat(sentAuthorization.get()).isNull();
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"expectedUpdatedAt\":null}", "{\"expectedUpdatedAt\":\"2026-11-06T09:00:00.123456\"}",
        "{\"expectedUpdatedAt\":\"2026-11-06T00:00:00.123456Z\"}"})
    void optionalTimestampIsNotConverted(String request) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(withVerificationId(request))).andExpect(status().isOk());
        assertThat(mapper.readTree(sentBody.get()).path("expectedUpdatedAt"))
            .isEqualTo(mapper.readTree(request).has("expectedUpdatedAt") ? mapper.readTree(request).get("expectedUpdatedAt") : mapper.nullNode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{", "{\"actorUsrLognId\":\"attacker\"}", "{\"changes\":[]}",
        "{\"verification\":{\"status\":\"fail\"}}", "{\"expectedUpdatedAt\":1}", "{\"expectedUpdatedAt\":true}",
        "{\"expectedUpdatedAt\":\"\"}", "{\"expectedUpdatedAt\":\"2026-11-06\"}", "{\"expectedUpdatedAt\":\"2026-02-30T09:00:00\"}"})
    void rejectsUntrustedActorContentAndMalformedRequests(String request) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(withVerificationId(request))).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "5.0", "abc", "9223372036854775808"})
    void rejectsInvalidPathBeforeCallingAgent(String id) throws Exception {
        mvc.perform(post("/api/v1/mi/next-gen/events/" + id + "/verification-updates")
            .contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void missingOrUnpublishedEventIsRejectedBeforeAgent() throws Exception {
        when(repository.findPublishedIdsForVerification(anyCollection())).thenReturn(List.of());
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isNotFound());
        assertThat(calls.get()).isZero();
    }

    @Test
    void anotherCompanyCannotModifyEvent() throws Exception {
        when(authorization.getCompanyCd()).thenReturn("other-company");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void adminMustHaveThirdPartyAccessToModify() throws Exception {
        when(authorization.getCurrentRole()).thenReturn(Role.ADMIN);
        when(authorization.isThirdPartyAccessAllowed()).thenReturn(false);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
        when(authorization.isThirdPartyAccessAllowed()).thenReturn(true);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isOk());
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void securityChainRequiresAuthentication() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(EventVerificationSecurityTest.SecurityOnlyConfig.class);
            context.refresh();
            var service = mock(EventVerificationUpdateService.class);
            var secured = MockMvcBuilders.standaloneSetup(new EventVerificationUpdateController(service))
                .addFilters(context.getBean(FilterChainProxy.class)).build();
            secured.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    void invalidAuthenticatedSubjectNeverReachesAgent(String actor) throws Exception {
        when(authorization.getCurrentJwt()).thenReturn(Jwt.withTokenValue("token").header("alg", "RS256")
            .claim("sub", actor).claim("userNo", 123).build());
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 409, 422, 500, 502, 503, 504})
    void upstreamErrorsDoNotRetryOrLeakInternalDetails(int upstreamStatus) throws Exception {
        agentStatus = upstreamStatus;
        agentBody = upstreamStatus == 422 ? "{\"detail\":[{\"msg\":\"sensitive-internal-detail\"}]}"
            : "{\"status\":\"error\",\"message\":\"sensitive-internal-detail\",\"data\":{\"error_code\":-203,\"error_name\":\"EVENT_UPDATE_STALE\"}}";
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().is(upstreamStatus)).andExpect(jsonPath("$.status").value(false)).andReturn();
        if (upstreamStatus == 409) assertThat(mapper.readTree(result.getResponse().getContentAsString()).get("code").asText()).isEqualTo("BIZ-007");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("sensitive-internal-detail");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void clientCredentialsSubjectCannotBeUsedAsAnAuditUser() throws Exception {
        when(authorization.getCurrentJwt()).thenReturn(Jwt.withTokenValue("service-token").header("alg", "RS256")
            .subject("company-client").claim("authMethod", "client_credentials").build());
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @Test
    void schemaFailureHasDistinctErrorCode() throws Exception {
        agentStatus = 500;
        agentBody = "{\"status\":\"error\",\"data\":{\"error_name\":\"EVENT_UPDATE_SCHEMA_ERROR\"}}";
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("API-005"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"expectedUpdatedAt\":null}", "{\"verificationId\":null}",
        "{\"verificationId\":63}", "{\"verificationId\":true}", "{\"verificationId\":[]}",
        "{\"verificationId\":\"\"}", "{\"verificationId\":\"not-a-uuid\"}", "{\"verificationId\":\"1-1-1-1-1\"}"})
    void missingOrMalformedVerificationIdNeverTriggersSearchOrUpdate(String body) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(repository);
        assertThat(calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "different", "nestedDifferent", "missingExpiry"})
    void responseMustReferToRequestedVerification(String defect) throws Exception {
        var body = response("skipped");
        switch (defect) {
            case "missing" -> body.remove("verificationId");
            case "different" -> body.put("verificationId", "00000000-0000-0000-0000-000000000001");
            case "nestedDifferent" -> ((ObjectNode) body.get("verification")).put("verificationId", "00000000-0000-0000-0000-000000000001");
            case "missingExpiry" -> ((ObjectNode) body.get("verification")).remove("expiresAt");
        }
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isBadGateway());
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void expiredVerificationIsDistinctFromEventChangeAndDoesNotSearchAgain() throws Exception {
        agentStatus = 409;
        agentBody = "{\"status\":\"error\",\"data\":{\"error_code\":-205,\"error_name\":\"EVENT_VERIFICATION_EXPIRED\"}}";
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BIZ-008"))
            .andExpect(jsonPath("$.data.error_name").value("EVENT_VERIFICATION_EXPIRED"))
            .andExpect(jsonPath("$.data.verification_id").value(VERIFICATION_ID));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void repeatedVerificationPreservesAppliedSnapshotAndDoesNotReportFreshUpdate() throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.updateStatus").value("updated"));
        String originalRequest = sentBody.get();
        agentStatus = 409;
        agentBody = mapper.createObjectNode().put("status", "error").set("data", mapper.createObjectNode()
            .put("error_code", -206).put("error_name", "EVENT_VERIFICATION_ALREADY_APPLIED")
            .put("verification_id", VERIFICATION_ID).put("mi_event_snapshot_id", 91)
            .put("internal_secret", "sensitive-internal-detail")).toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(false))
            .andExpect(jsonPath("$.code").value("BIZ-009"))
            .andExpect(jsonPath("$.data.error_code").value(-206))
            .andExpect(jsonPath("$.data.error_name").value("EVENT_VERIFICATION_ALREADY_APPLIED"))
            .andExpect(jsonPath("$.data.verification_id").value(VERIFICATION_ID))
            .andExpect(jsonPath("$.data.mi_event_snapshot_id").value(91)).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("sensitive-internal-detail");
        assertThat(sentBody.get()).isEqualTo(originalRequest);
        assertThat(calls.get()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingSnapshot", "wrongId", "unknownConflict"})
    void invalidConflictCannotPointToAnotherSavedResult(String defect) throws Exception {
        agentStatus = 409;
        var data = mapper.createObjectNode().put("error_name", "EVENT_VERIFICATION_ALREADY_APPLIED")
            .put("verification_id", VERIFICATION_ID).put("mi_event_snapshot_id", 91);
        switch (defect) {
            case "missingSnapshot" -> data.remove("mi_event_snapshot_id");
            case "wrongId" -> data.put("verification_id", "00000000-0000-0000-0000-000000000001");
            case "unknownConflict" -> data.put("error_name", "UNKNOWN_CONFLICT");
        }
        agentBody = mapper.createObjectNode().set("data", data).toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isBadGateway());
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void invalidStoredVerificationHasDistinctError() throws Exception {
        agentStatus = 500;
        agentBody = "{\"data\":{\"error_name\":\"EVENT_VERIFICATION_INVALID\"}}";
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("API-006"));
    }

    @Test
    void timeoutDoesNotRetryAndInstructsCheckingSavedState() throws Exception {
        delayMillis = 350;
        mvc(Duration.ofMillis(50)).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("API-002"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Snapshot")));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void recheckedNoopRemainsUnchanged() throws Exception {
        var body = response("unchanged");
        body.set("verification", verification("fail"));
        body.set("postVerification", verification("pass").putNull("verificationId").putNull("expiresAt"));
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.updateStatus").value("unchanged"))
            .andExpect(jsonPath("$.data.miEventSnapshotId").isEmpty());
    }

    @Test
    void failedReverificationRemainsSkipped() throws Exception {
        var body = response("skipped");
        body.set("verification", verification("fail"));
        body.set("postVerification", verification("warning"));
        agentBody = body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.updateStatus").value("skipped"))
            .andExpect(jsonPath("$.data.miEventSnapshotId").isEmpty());
    }

    private ObjectNode qualifiedVerification(boolean hasMatched) {
        var item = verification("pass").put("status", hasMatched ? "pass" : "warning")
            .put("reasonCode", hasMatched ? "no_contradiction" : "review_required");
        if (!hasMatched) {
            for (var check : item.withArray("checks")) ((ObjectNode) check).put("verdict", "not_applicable");
        }
        ((ObjectNode) item.withArray("checks").get(4)).put("verdict", "caution")
            .put("cautionReason", "as_of_limitation").put("finding", "기준 날짜 이후 상태는 확인하지 못했습니다.")
            .put("qualificationQuote", "2026-11-06 기준이며 이후 변동은 확인하지 못했습니다.");
        return item;
    }

    private void assertExactUpdateResponse(ObjectNode body) throws Exception {
        agentBody = body.toString();
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isOk()).andReturn();
        assertThat(mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data"))
            .isEqualTo(body);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void publicPassDoesNotTurnSkippedIntoSaved() throws Exception {
        var body = response("skipped");
        body.set("verification", qualifiedVerification(true));
        body.put("message", "명확한 불일치는 없으나 수정안 생성 조건을 충족하지 않아 생략했습니다.");
        assertExactUpdateResponse(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"source_conflict", "insufficient_evidence", "other"})
    void skippedCandidatePublicPassDoesNotReplaceCurrentEventVerification(String cautionReason) throws Exception {
        var body = response("skipped");
        body.set("verification", verification("fail"));
        var candidate = qualifiedVerification(true);
        ((ObjectNode) candidate.withArray("checks").get(4)).put("cautionReason", cautionReason)
            .putNull("qualificationQuote");
        body.set("postVerification", candidate);
        body.put("message", "수정안의 저장 조건을 충족하지 못했습니다.");
        assertExactUpdateResponse(body);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void committedConditionalUpdatePreservesPublicGradeAndRemainingCaution(boolean hasMatched) throws Exception {
        var body = response("updated");
        var original = verification("pass").put("status", "fail").put("reasonCode", "outdated");
        for (var check : original.withArray("checks")) ((ObjectNode) check).put("verdict", "not_applicable");
        ((ObjectNode) original.withArray("checks").get(4)).put("verdict", "outdated");
        body.set("verification", original);
        body.set("postVerification", qualifiedVerification(hasMatched));
        body.put("message", "기준 날짜와 미확인 범위를 명시하여 저장했습니다.");
        assertExactUpdateResponse(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "json", "wrongId", "missingVerification", "wrongPostId", "missingSnapshot", "missingRecheck", "unknownStatus", "wrongField", "differentChanges", "emptyChanges", "skippedWithChanges", "sameValue"})
    void inconsistentSuccessIsNeverReportedAsSaved(String defect) throws Exception {
        var body = response("updated");
        switch (defect) {
            case "wrongId" -> body.put("miEventId", 99);
            case "missingVerification" -> body.remove("verification");
            case "wrongPostId" -> ((ObjectNode) body.get("postVerification")).put("miEventId", 99);
            case "missingSnapshot" -> body.putNull("miEventSnapshotId");
            case "missingRecheck" -> body.putNull("postVerification");
            case "unknownStatus" -> body.put("updateStatus", "success");
            case "wrongField" -> body.withArray("changedFields").add("unauthorized_field");
            case "differentChanges" -> ((ObjectNode) body.withArray("changes").get(0)).put("field", "search_text");
            case "emptyChanges" -> { body.withArray("changedFields").removeAll(); body.withArray("changes").removeAll(); }
            case "skippedWithChanges" -> body.put("updateStatus", "skipped");
            case "sameValue" -> { var change = (ObjectNode) body.withArray("changes").get(0); change.set("after", change.get("before")); }
            default -> { }
        }
        agentBody = "empty".equals(defect) ? "" : "json".equals(defect) ? "{" : body.toString();
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Snapshot")));
        assertThat(calls.get()).isEqualTo(1);
    }
}
