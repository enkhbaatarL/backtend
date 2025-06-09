package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
public record Source(

    @Schema(description = "출처 경로")
    String url,

    @Schema(description = "출처 명")
    String sourceNm

) {

    public static Source of(ModelAgentRes.Message.Content.Source source) {
        return Source.builder()
            .url(source.url())
            .sourceNm(source.sourceNm())
            .build();
    }

    public static List<Source> ofList(List<ModelAgentRes.Message.Content.Source> sources) {
        if (sources == null) return Collections.emptyList();
        return sources.stream()
            .map(Source::of)
            .toList();
    }

}
