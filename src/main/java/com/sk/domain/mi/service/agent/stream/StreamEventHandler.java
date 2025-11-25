package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;

/**
 * Agent 스트림 이벤트를 처리하는 Handler 인터페이스
 */
public interface StreamEventHandler {
    /**
     * 이 Handler가 처리하는 이벤트 데이터 타입을 반환
     * @return "plan", "execute", "generate" 등
     */
    String getEventDataType();

    /**
     * ModelAgentStreamBody를 처리하여 AnswerRes를 생성
     *
     * @param qna 현재 질의응답 엔티티
     * @param streamBody 스트림 데이터
     * @return 응답 DTO
     * @throws JsonProcessingException JSON 처리 중 오류 발생 시
     */
    AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody)
        throws JsonProcessingException;
}
