package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanStreamEventHandler implements StreamEventHandler{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Map<String, String> EXPLAIN_MAP;

    static {
        EXPLAIN_MAP = new HashMap<>();
        EXPLAIN_MAP.put("general_request", "일반 질의의 답을 찾고 있습니다.");
        EXPLAIN_MAP.put("single_metric", "단일 지표 분석을 위해 데이터를 찾고 있습니다.");
        EXPLAIN_MAP.put("timeseries_trend", "시계열 추세를 분석하기 위해 데이터를 찾고 있습니다.");
        EXPLAIN_MAP.put("item_comparison", "데이터를 비교 분석하기 위해 정보를 검색 하고 있습니다.");
        EXPLAIN_MAP.put("similarity_pattern", "유사 패턴을 분석하기 위해 데이터를 찾고 있습니다.");
        EXPLAIN_MAP.put("market_impact", "산업 영향을 분석하기 위해 데이터를 찾고 있습니다.");
        EXPLAIN_MAP.put("event_impact", "이벤트의 영향을 분석하기 위해 데이터를 찾고 있습니다.");
    }

    private final MariAgentResParser mariAgentResParser;

    @Override
    public String getEventDataType() {
        return "plan";
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, JsonNode jsonNode) throws JsonProcessingException {

        ModelAgentStreamBody modelAgentStreamBody = OBJECT_MAPPER.treeToValue(jsonNode, ModelAgentStreamBody.class);
        List<Related> relatedList = mariAgentResParser.prepareAnswerDataResources(modelAgentStreamBody);

        String text = modelAgentStreamBody.analysisType();
        return AnswerRes.of(text, modelAgentStreamBody, relatedList);
    }
}
