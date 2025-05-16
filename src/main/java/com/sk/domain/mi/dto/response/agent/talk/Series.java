package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record Series(

    @Schema(description = "timestamps, x축")
    List<String> timestamps,

    @Schema(description = "시계열 데이터 명")
    String seriesNm,

    @Schema(description = "값 리스트")
    List<BigDecimal> values

) {

    public static Series of(ModelAgentRes.Output.Message.Content content) {

        List<ModelAgentRes.Output.Message.Content.Chart> charts = content.charts();

        List<String> timestamps = charts.stream().map(s -> s.date().replaceAll("-", "")).toList();
        List<BigDecimal> values = charts.stream().map(s -> BigDecimal.valueOf(s.price())).toList();

        return Series.builder()
            .timestamps(timestamps)
            .seriesNm("미정")
            .values(values)
            .build();
    }

}
