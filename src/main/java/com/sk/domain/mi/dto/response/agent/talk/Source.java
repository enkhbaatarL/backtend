package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record Source(

    @Schema(description = "출처 경로")
    String url,

    @Schema(description = "출처 명")
    String sourceNm

) {

    public static Source of(ModelAgentRes.Output.Content source) {
        return Source.builder()
            .sourceNm("test")
            .build();
    }

}
