package com.sk.domain.mi.dto.response.agent.talk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;

@Builder
public record Source(

        @Schema(description = "출처 경로") String url,

        @Schema(description = "출처 명") String sourceNm

) {

    public static Source of(ModelAgentRes.Output.Message.Content.Source source) {
        return Source.builder()
                .url(source.url())
                .sourceNm(source.sourceNm())
                .build();
    }

    public static List<Source> ofList(List<ModelAgentRes.Output.Message.Content.Source> sources) {
        return sources != null ? sources.stream().map(Source::of).toList() : List.of();
    }

}
