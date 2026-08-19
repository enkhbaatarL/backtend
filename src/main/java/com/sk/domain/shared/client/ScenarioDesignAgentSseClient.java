package com.sk.domain.shared.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.utils.JsonUtil;
import com.sk.common.exception.AgentDesignApiException;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 시나리오 설계 Agent SSE 스트림 중계 클라이언트 (B.E -> Agent).
 * 생성/operation 스트림의 개설과 프레임 중계만 담당한다 — REST 호출은 전부 ScenarioDesignAgentApi 를 사용한다.
 * 스트림은 exchange 기반 블로킹 소비라 선언형 인터페이스로 옮길 수 없어 별도 클래스로 유지한다.
 */
@Slf4j
@Component
public class ScenarioDesignAgentSseClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration STREAM_READ_TIMEOUT = Duration.ofSeconds(120);

    private final RestClient restClient;

    public ScenarioDesignAgentSseClient(@Value("${api.scenario-design-agent.url:http://40.82.137.178:8001}") String url) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(STREAM_READ_TIMEOUT);
        this.restClient = RestClient.builder()
            .baseUrl(url)
            .requestFactory(requestFactory)
            .build();
    }

    /**
     * 시나리오 그룹 생성 스트림 개설 및 중계. 호출 스레드에서 블로킹으로 스트림을 소비한다 (전용 executor에서 실행할 것).
     * 응답 status가 확정되면 connectGate를 완료(정상)하거나 예외 완료(JSON 오류)한다.
     */
    public void pipeCreateStream(ScenarioGroupCreateV4AgentReq request, SseEmitter emitter, CompletableFuture<Void> connectGate) {
        restClient.post()
            .uri("/scenario-groups")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .body(request)
            .exchange((clientRequest, clientResponse) -> {
                if (clientResponse.getStatusCode().isError()) {
                    connectGate.completeExceptionally(translateError(clientResponse.getStatusCode().value(), readBody(clientResponse.getBody())));
                    return null;
                }
                connectGate.complete(null);
                pipeSse(clientResponse.getBody(), emitter);
                return null;
            });
    }

    /**
     * operation 스트림 재연결 중계. Last-Event-ID를 전달하면 Agent가 그 다음 sequence부터 replay한다.
     */
    public void pipeOperationStream(Long scenarioGroupId, String operationId, String lastEventId, SseEmitter emitter, CompletableFuture<Void> connectGate) {
        restClient.get()
            .uri("/scenario-groups/{scenarioGroupId}/operations/{operationId}/stream", scenarioGroupId, operationId)
            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
            .headers(headers -> {
                if (lastEventId != null && !lastEventId.isBlank()) {
                    headers.set("Last-Event-ID", lastEventId);
                }
            })
            .exchange((clientRequest, clientResponse) -> {
                if (clientResponse.getStatusCode().isError()) {
                    connectGate.completeExceptionally(translateError(clientResponse.getStatusCode().value(), readBody(clientResponse.getBody())));
                    return null;
                }
                connectGate.complete(null);
                pipeSse(clientResponse.getBody(), emitter);
                return null;
            });
    }

    /**
     * SSE 프레임(id/event/data/comment)을 파싱해 emitter로 그대로 전달한다.
     * FE가 끊기면(send 실패) 중계를 멈추고, 업스트림이 정상 종료(terminal event 후 close)하면 emitter를 완료한다.
     */
    private void pipeSse(InputStream body, SseEmitter emitter) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String id = null;
            String event = null;
            StringBuilder data = null;
            String comment = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (!sendFrame(emitter, id, event, data, comment)) {
                        safeComplete(emitter);
                        return;
                    }
                    id = null;
                    event = null;
                    data = null;
                    comment = null;
                    continue;
                }
                if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                } else if (line.startsWith("event:")) {
                    event = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String value = line.length() > 5 && line.charAt(5) == ' ' ? line.substring(6) : line.substring(5);
                    if (data == null) {
                        data = new StringBuilder(value);
                    } else {
                        data.append('\n').append(value);
                    }
                } else if (line.startsWith(":")) {
                    comment = line.substring(1).trim();
                }
            }
            // 잔여 프레임 (마지막 프레임 뒤 빈 줄 없이 종료된 경우)
            sendFrame(emitter, id, event, data, comment);
            safeComplete(emitter);
        } catch (IOException e) {
            log.warn("[Design V4] SSE 중계 중단 (upstream): {}", e.getMessage());
            safeCompleteWithError(emitter, e);
        }
    }

    /** 프레임 1건 전달. FE 연결이 끊겨 전송이 불가하면 false. */
    private boolean sendFrame(SseEmitter emitter, String id, String event, StringBuilder data, String comment) {
        if (id == null && event == null && data == null && comment == null) {
            return true;
        }
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event();
            if (id != null) {
                builder.id(id);
            }
            if (event != null) {
                builder.name(event);
            }
            if (data != null) {
                builder.data(data.toString());
            }
            if (comment != null) {
                builder.comment(comment);
            }
            emitter.send(builder);
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("[Design V4] FE 연결 종료로 중계 중단: {}", e.getMessage());
            return false;
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Exception e) {
        try {
            emitter.completeWithError(e);
        } catch (Exception ignored) {
            // 이미 완료/타임아웃된 emitter
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 이미 완료/타임아웃된 emitter
        }
    }

    private String readBody(InputStream body) {
        try {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Agent JSON 오류를 전용 예외로 변환한다. HTTP status는 그대로 통과시키고,
     * data 객체는 원문 그대로 보존해 FE가 계약대로 error_name 분기와 기존 리소스 ID 재사용
     * (operationId 재연결 / scenarioGroupId 상세 이동)을 할 수 있게 한다.
     * GlobalExceptionHandler가 BaseResponse로 래핑한다.
     */
    public static AgentDesignApiException translateError(int statusValue, String bodyText) {
        String message = bodyText;
        JsonNode dataNode = null;
        try {
            JsonNode bodyNode = JsonUtil.fromJson(bodyText, JsonNode.class);
            if (bodyNode != null) {
                message = bodyNode.path("message").asText(bodyText);
                dataNode = bodyNode.hasNonNull("data") ? bodyNode.get("data") : null;
                // FastAPI 요청 검증 오류는 공통 body가 아니라 {"detail": [...]} 형식
                if (dataNode == null && bodyNode.has("detail")) {
                    message = bodyNode.get("detail").toString();
                }
            }
        } catch (Exception e) {
            log.debug("[Design V4] agent 오류 본문 파싱 실패: {}", e.getMessage());
        }

        HttpStatus status = HttpStatus.resolve(statusValue);
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        ResultCode.Error code = switch (statusValue) {
            case 409, 403 -> ResultCode.Error.BIZ_DESIGN_OCCUPIED;
            case 400, 422 -> ResultCode.Error.BIZ_RULE_VIOLATION;
            default -> ResultCode.Error.API_RESPONSE_ERROR;
        };

        log.warn("[Design V4] agent 오류: status={}, errorName={}, data={}",
            statusValue, dataNode == null ? null : dataNode.path("error_name").asText(null), dataNode);
        return AgentDesignApiException.of(status, code, message, dataNode);
    }

}
