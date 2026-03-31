package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecuteStreamEventHandler implements StreamEventHandler {

    @Override
    public String getEventDataType() {
        return "execute";
    }

    @Override
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody) {

        StreamRes streamRes = StreamRes.of(qna, streamBody);
        return AnswerRes.of(streamBody, streamRes);
    }
}
