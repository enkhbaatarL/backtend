package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import lombok.Builder;

import java.util.List;

@Builder
public record AnswerRes(

        List<MessageBundle> bundles

) {

    public static AnswerRes of(List<ModelAgentRes.Output.Message> messages, List<Related> related) {
        return AnswerRes.builder()
                .bundles(messages
                        .stream()
                        .map(message -> MessageBundle.of(message, related))
                        .toList())
                .build();
    }

    public static AnswerRes of(String text, ModelAgentStreamBody modelAgentStreamBody, List<Related> related) {
        return AnswerRes.builder()
            .bundles(List.of(MessageBundle.of(text, modelAgentStreamBody, related)))
            .build();
    }






}
