package com.sk.domain.mi.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.exception.AgentStreamException;
import com.sk.domain.mi.service.agent.stream.StreamEventHandler;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.request.ContentProviderAccess;
import com.sk.domain.shared.client.dto.request.ModelAgentHeaders;
import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.annotation.PostConstruct;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MariAgentGatewayService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int LOG_PREVIEW_MAX = 400;
    private final AuthorizationService authorizationService;
    private final ModelAgentApi modelAgentApi;
    private final TokenQuotaGuard tokenQuotaGuard;
    private final Map<String, StreamEventHandler> streamEventHandlerNameMap;
    private final Map<String, StreamEventHandler> streamEventHandlerMap = new HashMap<>();
    private final GoodsRepository goodsRepository;
    @Value("${api.model-agent.app-id}")
    private String appId;
    private WebClient webClient;
    @Value("${api.model-agent.url}")
    private String agentUrl;
    @Value("${api.model-agent.api-key}")
    private String agentApiKey;
    @Value("${api.model-agent.read-timeout:10s}")
    private Duration agentReadTimeout;

    private static String previewForLog(String text, int max) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ');
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
            .baseUrl(agentUrl)
            .build();

        for (StreamEventHandler streamEventHandler : streamEventHandlerNameMap.values()) {
            this.streamEventHandlerMap.put(streamEventHandler.getEventDataType(),
                streamEventHandler);
        }
    }

    @Transactional
    protected Optional<AnswerRes> executeModelAgent(QnaReq.Insert talkReq, ChatbotTalkQna qna,
        List<ChatbotTalkQna> histories, Set<ContentProviderAccess> contentProviderAccesses, int userNo,
        int companyNo) {

        try {
            Map<String, Object> headers = ModelAgentHeaders.of(companyNo, userNo,
                qna.getChatbotTalk().getChatbotTalkUuid(), qna.getChatbotTalkQnaNo()).toMap();
            List<ContentProviderAccess> vdbContentProviderAccesses =
                getVdbContentProviderAccesses(companyNo, userNo);

            ModelAgentReq body = ModelAgentReq.of(
                talkReq, histories, companyNo, userNo,
                contentProviderAccesses,
                vdbContentProviderAccesses,
                true, true);

            String question = talkReq.question();
            log.info(
                "[Model-Agent] invoke request qnaNo={} chatRoomId={} companyNo={} userNo={} questionLen={} historiesCount={} preview={}",
                qna.getChatbotTalkQnaNo(),
                qna.getChatbotTalk().getChatbotTalkUuid(),
                companyNo,
                userNo,
                question == null ? 0 : question.length(),
                histories == null ? 0 : histories.size(),
                previewForLog(question, LOG_PREVIEW_MAX));

            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(appId, headers, body);

            log.info(
                "[Model-Agent] invoke response qnaNo={} chatRoomId={} receivedOutput={}",
                qna.getChatbotTalkQnaNo(),
                qna.getChatbotTalk().getChatbotTalkUuid(),
                modelAgentRes != null && modelAgentRes.output() != null);

            // 26.07.29 취약점 진단 수정, '누락된 널 값 검사-102183'
            if (modelAgentRes == null) {
                log.error("[Model-Agent] invoke response is null. qnaNo: {}, chatRoomId: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid());
                qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
                return Optional.empty();
            }

            AnswerRes answerRes = AnswerRes.of(modelAgentRes.output());
            qna.updateCompleteResult(answerRes);

            TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(
                authorizationService.getCompanyNoWithPriority(), 0);
            log.info(
                "[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}",
                authorizationService.getCompanyNoWithPriority(), quotaResult.delta(),
                quotaResult.usage(), quotaResult.allowed());

            return Optional.ofNullable(answerRes);

        } catch (ResourceAccessException e) {

            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);

            if (root instanceof SocketTimeoutException err) {
                log.error(
                    "[Model-Agent API call read-timeout] for qnaNo: {}, chatRoomId: {}. Error: {}",
                    qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(),
                    err.getMessage(), err);
                qna.updateFailResult(AnswerStatusCd.FAIL_TIMED_OUT);
            } else {
                log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}",
                    qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(),
                    e.getMessage(), e);
                qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}",
                qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(),
                e.getMessage(), e);
            qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
            return Optional.empty();
        }

    }

    protected Flux<AnswerRes> executeModelAgentStream(
        QnaReq.Insert talkReq,
        ChatbotTalkQna qna,
        List<ChatbotTalkQna> histories,
        Set<ContentProviderAccess> contentProviderAccesses,
        int userNo,
        int companyNo
    ) {

        long streamStartTime = System.currentTimeMillis();

        HttpHeaders headers = ModelAgentHeaders.of(
            companyNo, userNo,
            qna.getChatbotTalk().getChatbotTalkUuid(),
            qna.getChatbotTalkQnaNo()
        ).toHeaders();

        List<ContentProviderAccess> vdbContentProviderAccesses =
            getVdbContentProviderAccesses(companyNo, userNo);

        ModelAgentReq body = ModelAgentReq.of(
            talkReq, histories, companyNo, userNo,
            contentProviderAccesses,
            vdbContentProviderAccesses,
            true, true
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            log.info(
                "[Model-Agent] stream request qnaNo={} chatRoomId={} companyNo={} userNo={} path=/{}/stream json={}",
                qna.getChatbotTalkQnaNo(),
                qna.getChatbotTalk().getChatbotTalkUuid(),
                companyNo,
                userNo,
                appId,
                bodyJson);
        } catch (JsonProcessingException e) {
            log.warn(
                "[Model-Agent] stream request qnaNo={} chatRoomId={} failed to serialize request body to JSON",
                qna.getChatbotTalkQnaNo(),
                qna.getChatbotTalk().getChatbotTalkUuid(),
                e);
        }

        log.info("[PERF] Stream started for qnaNo: {}", qna.getChatbotTalkQnaNo());

        return webClient.post()
            .uri("/" + appId + "/stream")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(httpHeaders -> {
                httpHeaders.addAll(headers);
                httpHeaders.add("Authorization", "Bearer " + agentApiKey);
            })
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
            })
            .timeout(Duration.ofSeconds(agentReadTimeout.getSeconds()))
            .doOnNext(event -> {
                String data = event.data();
                int len = data == null ? 0 : data.length();
                if (log.isDebugEnabled()) {
                    log.debug(
                        "[Model-Agent] stream SSE raw qnaNo={} eventId={} eventName={} dataLen={} dataPreview={}",
                        qna.getChatbotTalkQnaNo(),
                        event.id(),
                        event.event(),
                        len,
                        previewForLog(data, 300));
                } else {
                    log.trace("[Model-Agent] stream SSE qnaNo={} event={} dataLen={}",
                        qna.getChatbotTalkQnaNo(), event.event(), len);
                }
            })
            .concatMap(event -> processServerSentEvent(event, qna))
            .doOnNext(answerRes -> {
            })
            .doOnComplete(() -> {
                long elapsed = System.currentTimeMillis() - streamStartTime;
                log.info(
                    "[Model-Agent] stream completed qnaNo={} chatRoomId={} elapsedMs={}",
                    qna.getChatbotTalkQnaNo(),
                    qna.getChatbotTalk().getChatbotTalkUuid(),
                    elapsed);
            })
            .doOnError(error -> {
                long totalTime = System.currentTimeMillis() - streamStartTime;
                log.error("[PERF] Stream error after {}ms - qnaNo: {}, chatRoomId: {}",
                    totalTime,
                    qna.getChatbotTalkQnaNo(),
                    qna.getChatbotTalk().getChatbotTalkUuid(),
                    error);
            });
    }

    private Flux<AnswerRes> processServerSentEvent(
        ServerSentEvent<String> event,
        ChatbotTalkQna qna
    ) {
        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102203', 오탐 -> 단락 평가로 data()의 null 검사 후 접근하며, ServerSentEvent는 불변 객체라 재호출 반환값이 동일함.
        if (event == null || event.data() == null || event.data().trim().isEmpty()) {
            return Flux.empty();
        }

        String eventType = event.event();
        String chunk = event.data();

        try {
            // metadata 이벤트 처리
            if ("metadata".equals(eventType)) {
                handleMetadataEvent(chunk);
                return Flux.empty();
            }

            // data 이벤트 처리
            if ("data".equals(eventType)) {
                ModelAgentStreamBody streamBody =
                    objectMapper.readValue(chunk, ModelAgentStreamBody.class);

                if (streamBody.node() == null || streamBody.data() == null) {
                    log.warn("Invalid event data: node={}, data={}",
                        streamBody.node(), streamBody.data());
                    return Flux.empty();
                }

                String value = streamBody.value();
                int valueLen = value == null ? 0 : value.length();
                if (streamBody.isCompleteState()) {
                    log.info(
                        "[Model-Agent] stream agent data (complete) qnaNo={} chatRoomId={} node={} dataType={} valueLen={} preview={}",
                        qna.getChatbotTalkQnaNo(),
                        qna.getChatbotTalk().getChatbotTalkUuid(),
                        streamBody.node(),
                        streamBody.data(),
                        valueLen,
                        previewForLog(value, LOG_PREVIEW_MAX));
                } else {
                    log.debug(
                        "[Model-Agent] stream agent data qnaNo={} state={} node={} dataType={} valueLen={} preview={}",
                        qna.getChatbotTalkQnaNo(),
                        streamBody.state(),
                        streamBody.node(),
                        streamBody.data(),
                        valueLen,
                        previewForLog(value, 200));
                }

                // Handler 선택
                StreamEventHandler handler = selectHandler(streamBody);

                if (handler != null) {
                    AnswerRes answerRes = handler.createAnswerRes(qna, streamBody);
                    return Flux.just(answerRes);
                } else {
                    log.info("Agent Event 처리 Skip - Node: {}", streamBody.node());
                    return Flux.empty();
                }
            }

            return Flux.empty();

        } catch (JsonProcessingException e) {
            log.error("JSON parsing error - qnaNo: {}",
                qna.getChatbotTalkQnaNo(), e);
            return Flux.error(new AgentStreamException(
                "Failed to parse agent response",
                AnswerStatusCd.FAIL_PARSE_MESSAGE,
                e
            ));

        } catch (Exception e) {
            log.error("Event processing error - qnaNo: {}",
                qna.getChatbotTalkQnaNo(), e);
            return Flux.error(new AgentStreamException(
                "Failed to process agent event",
                AnswerStatusCd.FAIL_AGENT_COMMUNICATION,
                e
            ));
        }
    }

    /**
     * metadata 이벤트 처리 data: {"run_id": "..."}
     */
    private void handleMetadataEvent(String chunk) throws JsonProcessingException {
        Map<String, Object> metadata = objectMapper.readValue(chunk, Map.class);
        String runId = (String) metadata.get("run_id");
        log.info("Received metadata event - run_id: {}", runId);
    }

    /**
     * ModelAgentStreamBody의 type에 따라 적절한 Handler를 선택
     */
    private StreamEventHandler selectHandler(ModelAgentStreamBody streamBody) {
        // 1. 스트리밍이 필요한 경우 (chunk + progress)
        if (streamBody.isStreamingRequired()) {
            return streamEventHandlerMap.get("execute");
        }

        // 2. vector_data 타입 처리
        if (streamBody.isVectorData()) {
            return streamEventHandlerMap.get("vector_data");
        }

        // 3. rdb_data 타입 처리
        if (streamBody.isRdbData()) {
            return streamEventHandlerMap.get("rdb_data");
        }


        // 4. 완료되지 않은 상태 (중간 진행 상태)
        if (!streamBody.isCompleteState()) {
            return streamEventHandlerMap.get("plan");
        }

        // 5. 완료 상태 (최종 결과)
        return streamEventHandlerMap.get("generate");
    }

    private List<ContentProviderAccess> getVdbContentProviderAccesses(int companyNo, int userNo) {
        List<Object[]> rows = goodsRepository.findVdbGoodsNosAndRepositoryIdsWithPermission(
            companyNo, userNo);

        return rows.stream()
            .collect(Collectors.groupingBy(
                row -> (String) row[2], // prvd_id
                Collectors.mapping(row -> ((Number) row[0]).intValue(), Collectors.toSet())
            ))
            .entrySet().stream()
            .map(entry -> ContentProviderAccess.of(entry.getKey(), entry.getValue()))
            .toList();
    }

}
