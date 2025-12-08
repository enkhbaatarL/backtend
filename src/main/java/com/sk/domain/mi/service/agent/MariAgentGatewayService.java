package com.sk.domain.mi.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.exception.AgentStreamException;
import com.sk.domain.mi.service.agent.stream.StreamEventHandler;
import com.sk.domain.shared.client.ModelAgentApi;
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
    private final AuthorizationService authorizationService;

    private final ModelAgentApi modelAgentApi;
    private final TokenQuotaGuard tokenQuotaGuard;
    private final Map<String, StreamEventHandler> streamEventHandlerNameMap;
    private final Map<String, StreamEventHandler> streamEventHandlerMap = new HashMap<>();
    private final GoodsRepository goodsRepository;
    String currentRunId;
    @Value("${api.model-agent.app-id}")
    private String appId;
    private WebClient webClient;
    @Value("${api.model-agent.url}")
    private String agentUrl;
    @Value("${api.model-agent.api-key}")
    private String agentApiKey;
    @Value("${api.model-agent.read-timeout:10s}")
    private Duration agentReadTimeout;

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
        List<ChatbotTalkQna> histories) {

        try {

            Integer companyNo = authorizationService.getCompanyNoWithPriority();
            Integer userNo = authorizationService.getUserNo();

            List<Integer> goodsNos = goodsRepository.findGoodsNosWithPermission(companyNo, userNo);
            Map<String, Object> headers = ModelAgentHeaders.of(companyNo, userNo,
                qna.getChatbotTalk().getChatbotTalkUuid(), qna.getChatbotTalkQnaNo()).toMap();
            ModelAgentReq body = ModelAgentReq.of(talkReq, histories, companyNo, userNo, goodsNos);

            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(appId, headers, body);

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
        List<Integer> goodsNos,
        int userNo,
        int companyNo
    ) {

        long streamStartTime = System.currentTimeMillis();

        HttpHeaders headers = ModelAgentHeaders.of(
            companyNo, userNo,
            qna.getChatbotTalk().getChatbotTalkUuid(),
            qna.getChatbotTalkQnaNo()
        ).toHeaders();

        ModelAgentReq body = ModelAgentReq.of(
            talkReq, histories, companyNo, userNo, goodsNos
        );

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
            })
            .concatMap(event -> processServerSentEvent(event, qna))
            .doOnNext(answerRes -> {
            })
            .doOnComplete(() -> {
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

                // Handler 선택
                StreamEventHandler handler = selectHandler(streamBody);

                if (handler != null) {
                    AnswerRes answerRes = handler.createAnswerRes(qna, streamBody);
                    return Flux.just(answerRes);
                } else {
                    log.debug("Agent Event 처리 Skip - Node: {}", streamBody.node());
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
        this.currentRunId = runId;

        log.info("Received metadata event - run_id: {}", runId);
    }

    /**
     * ModelAgentStreamBody의 type에 따라 적절한 Handler를 선택
     */
    private StreamEventHandler selectHandler(ModelAgentStreamBody streamBody) {
        if (streamBody.isStreamingRequired()) {
            return streamEventHandlerMap.get("execute");
        } else if (!streamBody.isCompleteState()) {
            return streamEventHandlerMap.get("plan");
        } else {
            return streamEventHandlerMap.get("generate");
        }
    }

}
