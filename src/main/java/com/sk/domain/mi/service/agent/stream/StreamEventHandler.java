package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;

public interface StreamEventHandler {
    String getEventDataType();
    AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, JsonNode jsonNode) throws JsonProcessingException;
}
