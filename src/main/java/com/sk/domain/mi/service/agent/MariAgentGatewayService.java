package com.sk.domain.mi.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.stream.StreamEventHandler;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.request.ModelAgentHeaders;
import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
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
    private final MariAgentResParser mariAgentResParser;
    private final Map<String, StreamEventHandler> streamEventHandlerNameMap;
    private final Map<String, StreamEventHandler> streamEventHandlerMap = new HashMap<>();
    private final GoodsRepository goodsRepository;
    @Value("${api.model-agent.app-id}")
    private String appId;
    // Agent Stream
    private WebClient webClient;
    @Value("${api.model-agent.url}") private String agentUrl;
    @Value("${api.model-agent.api-key}") private String agentApiKey;
    @Value("${api.model-agent.connection-timeout:1s}") private Duration agentConnectTimeout;
    @Value("${api.model-agent.read-timeout:10s}") private Duration agentReadTimeout;

    @PostConstruct
    public void init(){
        this.webClient = WebClient.builder()
            .baseUrl(agentUrl)
            .build();

        for(StreamEventHandler streamEventHandler : streamEventHandlerNameMap.values()){
            this.streamEventHandlerMap.put(streamEventHandler.getEventDataType(), streamEventHandler);
        }
    }

    /**
     * Model-Agent API를 호출하여 질의 결과를 가져옵니다.
     *
     * @return Optional<AnswerRes>
     */
    @Transactional
    protected Optional<AnswerRes> executeModelAgent(QnaReq.Insert talkReq, ChatbotTalkQna qna, List<ChatbotTalkQna> histories) {

        try {

            Integer companyNo = authorizationService.getCompanyNoWithPriority();
            Integer userNo = authorizationService.getUserNo();

//            CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
//                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));

            List<Integer> goodsNos = goodsRepository.findGoodsNosWithPermission(companyNo, userNo);
            Map<String, Object> headers = ModelAgentHeaders.of(companyNo, userNo, qna.getChatbotTalk().getChatbotTalkUuid(), qna.getChatbotTalkQnaNo()).toMap();
            ModelAgentReq body = ModelAgentReq.of(talkReq, histories, companyNo, authorizationService.getUserNo(), goodsNos);

            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(appId, headers, body);

            // 테스트용 더미 데이터
//            ModelAgentRes modelAgentRes = JsonUtil.fromJson(DummyResponses.random(), ModelAgentRes.class);

            List<Related> related = mariAgentResParser.prepareAnswerDataResources(modelAgentRes.output());

            AnswerRes answerRes = AnswerRes.of(modelAgentRes.output().messages(), related);
            qna.updateCompleteResult(modelAgentRes.output(), answerRes);

            TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(authorizationService.getCompanyNoWithPriority(), qna.getInputTokenCnt() + qna.getOutputTokenCnt());
            log.info("[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}", authorizationService.getCompanyNoWithPriority(), quotaResult.delta(), quotaResult.usage(), quotaResult.allowed());

            return Optional.ofNullable(answerRes);

        } catch (ResourceAccessException e) {

            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);

            if (root instanceof SocketTimeoutException err) {
                log.error("[Model-Agent API call read-timeout] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), err.getMessage(), err);
                qna.updateFailResult(AnswerStatusCd.FAIL_TIMED_OUT);
            } else {
                log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
                qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
            qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
            return Optional.empty();
        }

    }

    @Transactional
    protected Flux<AnswerRes> executeModelAgentStream(QnaReq.Insert talkReq, ChatbotTalkQna qna, List<ChatbotTalkQna> histories) {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();
        Integer userNo = authorizationService.getUserNo();
        List<Integer> goodsNos = goodsRepository.findGoodsNosWithPermission(companyNo, userNo);

        HttpHeaders headers = ModelAgentHeaders.of(companyNo, userNo, qna.getChatbotTalk().getChatbotTalkUuid(), qna.getChatbotTalkQnaNo()).toHeaders();
        ModelAgentReq body = ModelAgentReq.of(talkReq, histories, companyNo, authorizationService.getUserNo(), goodsNos);

        return Flux.create(sink -> {
            // Agent에 스트림 요청
            webClient.post()
                .uri("/" + appId +"/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders->{
                    httpHeaders.addAll(headers);
                    httpHeaders.add("Authorization", "Bearer " + agentApiKey);
                })
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .timeout(Duration.ofSeconds(agentReadTimeout.getSeconds()))
                .subscribe(
                    event -> {
                        if(event == null) return;
                        log.debug("Agent 응답 Event Type: {}", event.event());
                        log.debug("Agent 응답 Event ID: {}", event.id());
                        log.debug("Agent 응답 Event Data: {}", event.data());
                        String chunk = event.data();
                        if (chunk != null && !chunk.trim().isEmpty()) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(chunk);
                                String eventDataName = jsonNode.fieldNames().next();
                                StreamEventHandler streamEventHandler = streamEventHandlerMap.get(eventDataName);
                                if(streamEventHandler != null){
                                    sink.next(streamEventHandler.createAnswerRes(qna, companyNo, jsonNode.get(eventDataName)));
                                }else{
                                    log.debug("Agent Event 처리 Skip : {}", eventDataName);
                                }
                            } catch (JsonProcessingException e) {
                                log.error("[Model-Agent API call read-timeout 1] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
                                qna.updateFailResult(AnswerStatusCd.FAIL_PARSE_MESSAGE);
                                sink.error(e);
                            }
                        }
                    },
                    error -> {
                        log.error("Agent 스트림 처리 중 오류 발생", error);
                        if (error instanceof SocketTimeoutException err) {
                            log.error("[Model-Agent API call read-timeout] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), err.getMessage(), err);
                            qna.updateFailResult(AnswerStatusCd.FAIL_TIMED_OUT);
                        } else if(error instanceof ResourceAccessException err) {
                            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), err.getMessage(), err);
                            qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
                        } else if(error instanceof JsonProcessingException err ){
                            log.error("[Model-Agent API call read-timeout] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), err.getMessage(), err);
                            qna.updateFailResult(AnswerStatusCd.FAIL_PARSE_MESSAGE);
                        } else {
                            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), error.getMessage(), error);
                            qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
                        }
                        sink.error(error);
                    },
                    () -> {
                        log.info("Agent 스트림 완료");
                        sink.complete();
                    }
                );
        });
    }


}
