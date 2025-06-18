package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Builder
public record Series(

    @Schema(description = "순서")
    Integer order,

    @Schema(description = "타입", example = "ACTUAL", allowableValues = {"ACTUAL", "FORECAST"})
    Type type,

    @Schema(description = "범례")
    String legend,

    @Schema(description = "timestamps, x축")
    List<String> timestamps,

    @Schema(description = "값 리스트")
    List<BigDecimal> values

) {

    @Getter
    @RequiredArgsConstructor
    public enum Type {

        ACTUAL("actual"),
        FORECAST("predict");

        private final String modelRes;

        public static Type typeOf(String modelRes) {
            if (modelRes == null || modelRes.isEmpty()) {
                return ACTUAL;
            }
            return Arrays.stream(values())
                .filter(s -> s.modelRes.equalsIgnoreCase(modelRes))
                .findFirst()
                .orElse(ACTUAL);
        }

    }

    public static Series of(ModelAgentRes.Output.Message.Content.Chart chart) {

        List<String> timestamps = chart.date().stream().map(s -> s.replaceAll("-", "")).toList();
        List<BigDecimal> values = chart.value().stream().map(BigDecimal::valueOf).toList();

        return Series.builder()
            .order(chart.order())
            .type(Type.typeOf(chart.type()))
            .legend(chart.legend())
            .timestamps(timestamps)
            .values(values)
            .build();

    }

    public static List<Series> ofList(List<ModelAgentRes.Output.Message.Content.Chart> charts) {

        if (charts == null)
            return Collections.emptyList();

        return charts.stream()
            .map(Series::of)
            .toList();

    }

}
