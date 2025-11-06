package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * analysis_chunk 타입 이벤트를 처리하는 Handler
 * LLM에서 생성된 텍스트 청크를 스트리밍으로 전달합니다.
 *
 * 예시 이벤트:
 * {
 *   "node": "DataAnalysisNode",
 *   "type": "analysis_chunk",
 *   "value": "_specific"
 * }
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExecuteStreamEventHandler implements StreamEventHandler {

    private final MariAgentResParser mariAgentResParser;

    @Override
    public String getEventDataType() {
        return "execute";
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, ModelAgentStreamBody streamBody)
            throws JsonProcessingException {
/*

        log.debug("ExecuteStreamEventHandler - node: {}, type: {}, value length: {}",
            streamBody.node(), streamBody.type(),
            streamBody.value() != null ? streamBody.value().length() : 0);
*/

        // LLM에서 생성된 chunk를 그대로 사용
        String text = streamBody.value();

        // 청크 단계에서는 보통 관련 데이터가 없지만, 있다면 처리
        List<Related> relatedList = Collections.emptyList();
        if (streamBody.relatedData() != null && !streamBody.relatedData().isEmpty()) {
            relatedList = mariAgentResParser.prepareRelatedData(streamBody.relatedData());
        }

        return AnswerRes.of(text, streamBody, relatedList);
    }
}
