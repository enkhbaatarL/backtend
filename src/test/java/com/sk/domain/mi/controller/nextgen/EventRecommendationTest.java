package com.sk.domain.mi.controller.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.aop.PermissionAspect;
import com.sk.common.handler.GlobalExceptionHandler;
import com.sk.domain.mi.service.nextgen.EventRecommendationService;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.shared.client.config.RestClientHttpApiConfig;
import com.sk.domain.shared.service.permissions.GoodsNestedEntityResolver;
import com.sk.domain.shared.service.permissions.PermissionMessageFormatter;
import com.sk.domain.shared.service.permissions.PermissionStrategyFactory;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real HTTP client + MVC contract tests, without DB, Redis or LLM dependencies. */
class EventRecommendationTest {
    private static final String ENDPOINT = "/api/v1/mi/next-gen/events/recommendations";
    private HttpServer agent;
    private ValidatorFactory validation;
    private MockMvc mvc;
    private PermissionStrategyFactory permissions;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> sentBody = new AtomicReference<>();
    private volatile int agentStatus = 200;
    private volatile String agentBody = "{\"items\":[],\"selectionReason\":\"No candidates\"}";
    private volatile long delayMillis;

    @BeforeEach
    void setUp() throws Exception {
        validation = Validation.buildDefaultValidatorFactory();
        permissions = mock(PermissionStrategyFactory.class);
        when(permissions.canAccessApi(any(PermissionRule[].class))).thenReturn(true);
        agent = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        agent.createContext("/events/recommendations", exchange -> {
            calls.incrementAndGet();
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).startsWith("application/json");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
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
        mvc = mvc(Duration.ofSeconds(2));
    }

    private MockMvc mvc(Duration readTimeout) {
        var api = new RestClientHttpApiConfig().eventRecommendationAgentApi(
            "http://127.0.0.1:" + agent.getAddress().getPort(), Duration.ofSeconds(1), readTimeout);
        var service = new EventRecommendationService(api, validation.getValidator());
        var controller = new AspectJProxyFactory(new EventController(mock(EventService.class), service));
        controller.addAspect(new PermissionAspect(permissions, mock(GoodsNestedEntityResolver.class),
            mock(PermissionMessageFormatter.class)));
        return MockMvcBuilders.standaloneSetup((EventController) controller.getProxy())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        agent.stop(0);
        validation.close();
    }

    @Test
    void forwardsAllSelectedIdsAndReturnsRecommendationsInData() throws Exception {
        agentBody = """
            {"items":[{"miEventId":47,"title":"LNG 공급 차질","reason":"수급 영향"},
                      {"miEventId":48,"title":"수요 변화","reason":"수요 영향"}],
             "selectionReason":"함께 분석"}
            """;
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[12,5]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].miEventId").value(47))
            .andExpect(jsonPath("$.data.items[0].title").value("LNG 공급 차질"))
            .andExpect(jsonPath("$.data.items[0].reason").value("수급 영향"))
            .andExpect(jsonPath("$.data.selectionReason").value("함께 분석"));
        assertThat(new ObjectMapper().readTree(sentBody.get()))
            .isEqualTo(new ObjectMapper().readTree("{\"miEventIds\":[12,5]}"));
        assertThat(calls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[5]", "[1,2,3,4,5]"})
    void emptyRecommendationsAreSuccessful(String ids) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":" + ids + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
            .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"miEventIds\":null}", "{\"miEventIds\":[]}", "{\"miEventIds\":[1,2,3,4,5,6]}",
        "{\"miEventIds\":[5,5]}", "{\"miEventIds\":[0]}", "{\"miEventIds\":[-1]}",
        "{\"miEventIds\":[null]}", "{\"miEventIds\":[1.5]}", "{\"miEventIds\":[1.0]}",
        "{\"miEventIds\":[\"5\"]}", "{\"miEventIds\":[true]}", "{\"miEventIds\":5}",
        "{\"miEventIds\":[9223372036854775808]}", "{", "null"
    })
    void rejectsInvalidInputBeforeCallingAgent(String body) throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(false));
        assertThat(calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 422, 500, 504})
    void preservesDocumentedAgentErrorStatusWithoutReturningEmptyItems(int upstreamStatus) throws Exception {
        agentStatus = upstreamStatus;
        agentBody = "{\"detail\":\"internal-sensitive-detail\"}";
        var result = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().is(upstreamStatus))
            .andExpect(jsonPath("$.status").value(false))
            .andExpect(jsonPath("$.data").isEmpty()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("internal-sensitive-detail");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "not-json", "{}", "{\"items\":null,\"selectionReason\":\"x\"}",
        "{\"items\":[null],\"selectionReason\":\"x\"}",
        "{\"items\":[{\"miEventId\":6}],\"selectionReason\":\"x\"}",
        "{\"items\":[{\"miEventId\":5,\"title\":\"x\",\"reason\":\"x\"}],\"selectionReason\":\"x\"}",
        "{\"items\":[{\"miEventId\":6,\"title\":\"x\",\"reason\":\"x\"},{\"miEventId\":6,\"title\":\"x\",\"reason\":\"x\"}],\"selectionReason\":\"x\"}",
        "{\"items\":[{\"miEventId\":6},{\"miEventId\":7},{\"miEventId\":8}],\"selectionReason\":\"x\"}"
    })
    void invalidSuccessfulAgentResponseIsAnError(String body) throws Exception {
        agentBody = body;
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("API-004"))
            .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deniedPermissionDoesNotCallAgent() throws Exception {
        when(permissions.canAccessApi(any(PermissionRule[].class))).thenReturn(false);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(false));
        assertThat(calls.get()).isZero();
    }

    @Test
    void unavailableAgentIsBadGateway() throws Exception {
        agent.stop(0);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"miEventIds\":[5]}"))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("API-003"));
    }

    @Test
    void configuredReadTimeoutIsEnforced() throws Exception {
        delayMillis = 500;
        mvc(Duration.ofMillis(100)).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content("{\"miEventIds\":[5]}"))
            .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("API-002"));
    }
}
