package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * analysis_complete 타입 이벤트를 처리하는 Handler
 * LLM 생성이 완료되면 최종 결과를 DB에 저장하고 토큰 사용량을 기록합니다.
 *
 * 예시 이벤트:
 * {
 *   "node": "DataAnalysisNode",
 *   "type": "analysis_complete",
 *   "value": "전체 생성된 텍스트...",
 *   "charts": [...],
 *   "related_data": [1, 2, 3],
 *   "usage": {...}
 * }
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateStreamEventHandler implements StreamEventHandler {

    private final MariAgentResParser mariAgentResParser;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final TokenQuotaGuard tokenQuotaGuard;

    @Override
    public String getEventDataType() {
        return "generate";
    }

    @Override
    @Transactional
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, ModelAgentStreamBody streamBody)
            throws JsonProcessingException {

        log.info("GenerateStreamEventHandler - node: {}, type: {}, 완료된 텍스트 길이: {}",
            streamBody.node(), streamBody.type(),
            streamBody.value() != null ? streamBody.value().length() : 0);

        // analysis_complete의 경우 value에 전체 생성된 텍스트가 포함
        String completedText = streamBody.value();

        // ModelAgentStreamBody를 ModelAgentRes.Output 형식으로 변환
        ModelAgentRes.Output output = convertToOutput(streamBody, completedText);

        // 관련 데이터 리소스 준비
        List<Related> relatedList = mariAgentResParser.prepareAnswerDataResources(output);

        // 최종 응답 생성
        AnswerRes answerRes = AnswerRes.of(output.messages(), relatedList);

        // DB에 완료 결과 저장
        qna.updateCompleteResult(output, answerRes);
        chatbotTalkQnaRepository.save(qna);

        // 토큰 사용량 체크 및 로깅
        TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(
            companyNo,
            qna.getInputTokenCnt() + qna.getOutputTokenCnt()
        );
        log.info("[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}",
            companyNo, quotaResult.delta(), quotaResult.usage(), quotaResult.allowed());

        return answerRes;
    }

    /**
     * ModelAgentStreamBody를 ModelAgentRes.Output 형식으로 변환
     *
     * @param streamBody 스트림 데이터
     * @param completedText 완료된 전체 텍스트
     * @return ModelAgentRes.Output 객체
     */
    private ModelAgentRes.Output convertToOutput(ModelAgentStreamBody streamBody, String completedText) {

        // Content 생성
        ModelAgentRes.Output.Message.Content content = new ModelAgentRes.Output.Message.Content(
            completedText,
            streamBody.charts(),
            streamBody.webSources(),
            streamBody.vectorSources(),
            streamBody.relatedData()
        );

        // Message 생성
        ModelAgentRes.Output.Message message = new ModelAgentRes.Output.Message(
            content,
            streamBody.usage(),  // analysis_complete에 usage 정보가 포함될 수 있음
            "assistant",  // 기본 타입
            null  // id는 필요시 생성
        );

        // Output 생성
        return new ModelAgentRes.Output(
            Collections.singletonList(message)
        );
    }
}
