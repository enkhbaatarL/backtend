package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModelAgentReq(

    Input input

) {

    @Builder
    public record Input(

        String message,

        @JsonProperty("web_search")
        Boolean shouldWebSearch,

        @JsonProperty("history_id")
        String uuid

    ) {

        public static Input of(QnaReq.Insert talkReq, String uuid) {
            return Input.builder()
                .message(talkReq.question())
                .shouldWebSearch(talkReq.shouldWebSearch())
                .uuid(uuid)
                .build();
        }

    }

    public static ModelAgentReq of(QnaReq.Insert talkReq, String uuid) {
        return ModelAgentReq.builder()
            .input(Input.of(talkReq, uuid))
            .build();
    }

}
