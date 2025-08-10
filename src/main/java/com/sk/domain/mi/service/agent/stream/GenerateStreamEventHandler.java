package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateStreamEventHandler implements StreamEventHandler{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MariAgentResParser mariAgentResParser;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final TokenQuotaGuard tokenQuotaGuard;

    @Override
    public String getEventDataType() {
        return "generate";
    }

    @Override
    @Transactional
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, JsonNode jsonNode) throws JsonProcessingException {

        ModelAgentRes.Output output = OBJECT_MAPPER.treeToValue(jsonNode, ModelAgentRes.Output.class);
        List<Related> relatedList = mariAgentResParser.prepareAnswerDataResources(output);

        AnswerRes answerRes = AnswerRes.of(output.messages(), relatedList);
        qna.updateCompleteResult(output, answerRes);
        chatbotTalkQnaRepository.save(qna);

        TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(companyNo, qna.getInputTokenCnt() + qna.getOutputTokenCnt());
        log.info("[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}", companyNo, quotaResult.delta(), quotaResult.usage(), quotaResult.allowed());

        return answerRes;

    }
}
