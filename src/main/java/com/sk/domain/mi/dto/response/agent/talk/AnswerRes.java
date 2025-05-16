package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.ModelAgentRes;
import lombok.Builder;

import java.util.List;

@Builder
public record AnswerRes(

    List<MessageBundle> bundles

) {

    public static AnswerRes of(ModelAgentRes modelAgentRes) {
        return AnswerRes.builder()
            .bundles(modelAgentRes.output().messages()
                .stream()
                .map(MessageBundle::of)
                .toList()
            )
            .build();
    }

}
