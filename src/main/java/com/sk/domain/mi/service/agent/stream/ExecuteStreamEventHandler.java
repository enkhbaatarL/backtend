package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody) {

        long start = System.currentTimeMillis();

        StreamRes streamRes = StreamRes.of(qna, streamBody);
        long streamResTime = System.currentTimeMillis() - start;

        AnswerRes answerRes = AnswerRes.of(streamBody, streamRes);
        long totalTime = System.currentTimeMillis() - start;

        if (totalTime > 50) {
            log.warn("[PERF] ExecuteHandler slow - StreamRes.of: {}ms, AnswerRes.of: {}ms, total: {}ms",
                streamResTime, (totalTime - streamResTime), totalTime);
        }

        return answerRes;
    }
}
