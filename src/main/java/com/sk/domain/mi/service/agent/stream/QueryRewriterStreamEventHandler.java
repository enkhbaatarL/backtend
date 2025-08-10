package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueryRewriterStreamEventHandler implements StreamEventHandler{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MariAgentResParser mariAgentResParser;

    @Override
    public String getEventDataType() {
        return "query_rewriter";
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, JsonNode jsonNode) throws JsonProcessingException {
        ModelAgentStreamBody modelAgentStreamBody = OBJECT_MAPPER.treeToValue(jsonNode, ModelAgentStreamBody.class);
        List<Related> relatedList = mariAgentResParser.prepareAnswerDataResources(modelAgentStreamBody);

        String text = "질의를 해석하고 있습니다.";
        return AnswerRes.of(text, modelAgentStreamBody, relatedList);
    }
}
