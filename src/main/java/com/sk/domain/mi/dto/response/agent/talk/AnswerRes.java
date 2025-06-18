package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import lombok.Builder;

import java.util.List;

@Builder
public record AnswerRes(

        List<MessageBundle> bundles

) {

    public static AnswerRes of(ModelAgentRes modelAgentRes, List<Related> related) {
        return AnswerRes.builder()
                .bundles(modelAgentRes.output().messages()
                        .stream()
                        .map(message -> MessageBundle.of(message, related))
                        .toList())
                .build();
    }

}
