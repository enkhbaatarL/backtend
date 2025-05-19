package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MessageBundle(

    @Schema(description = "내용(MarkDown Format)", example = "")
    String content,

    @Schema(description = "시계열 데이터", example = "")
    Series series

) {

    public static MessageBundle of(ModelAgentRes.Output.Message message) {
        return MessageBundle.builder()
            .content(message.content().text())
            .series(Series.of(message.content()))
            .build();
    }

}
