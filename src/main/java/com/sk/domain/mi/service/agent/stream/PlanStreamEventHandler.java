package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * info 타입 이벤트를 처리하는 Handler
 * 각 Node의 진행 상태를 프론트엔드로 전달합니다.
 *
 * 예시 이벤트:
 * {
 *   "node": "PreparePromptNode",
 *   "type": "info",
 *   "value": "분석에 사용할 프롬프트 리소스를 준비중입니다... 🛠️"
 * }
 *
 * 처리 전략:
 * 1. text 필드: node 값 전달 (프론트엔드에서 i18n 키로 사용)
 * 2. streamBody: 전체 데이터 전달 (value 포함)
 *
 * 프론트엔드 처리:
 * - text(node)를 i18n 키로 사용하여 다국어 메시지 표시
 * - 실패 시 streamBody.value를 fallback으로 사용 가능
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanStreamEventHandler implements StreamEventHandler {

    private final MariAgentResParser mariAgentResParser;

    @Override
    public String getEventDataType() {
        return "plan";
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo, ModelAgentStreamBody streamBody) {

        log.debug("PlanStreamEventHandler - state: {}, node: {}, data: {}, value: {}",
            streamBody.state(),
            streamBody.node(),
            streamBody.data(),
            streamBody.value());


        return AnswerRes.of(streamBody);
    }
}
