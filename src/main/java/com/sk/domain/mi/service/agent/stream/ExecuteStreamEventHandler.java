package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, Integer companyNo,
        ModelAgentStreamBody streamBody) {

        log.debug("ExecuteStreamEventHandler - state: {} data:{} node: {}, value: {}",
            streamBody.state(), streamBody.data(), streamBody.node(),
            streamBody.value());

        return AnswerRes.of(streamBody);
    }
}
