package com.sk.domain.mi.dto.response.agent.talk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.sk.domain.shared.client.dto.ModelAgentRes;

@Builder
public record Series(

        @Schema(description = "순서") Integer order,

        @Schema(description = "범례") String legend,

        @Schema(description = "timestamps, x축") List<String> timestamps,

        @Schema(description = "값 리스트") List<BigDecimal> values

) {

    public static Series of(ModelAgentRes.Message.Content.Chart chart) {

        List<String> timestamps = chart.date().stream().map(s -> s.replaceAll("-", "")).toList();
        List<BigDecimal> values = chart.value().stream().map(BigDecimal::valueOf).toList();

        return Series.builder()
                .order(chart.order())
                .legend(chart.legend())
                .timestamps(timestamps)
                .values(values)
                .build();

    }

    public static List<Series> ofList(List<ModelAgentRes.Message.Content.Chart> charts) {
        if (charts == null)
            return Collections.emptyList();
        return charts.stream()
                .map(Series::of)
                .toList();
    }

}
