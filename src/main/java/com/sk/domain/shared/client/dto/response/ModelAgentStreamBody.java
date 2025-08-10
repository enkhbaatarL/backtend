package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ModelAgentStreamBody(
        String message,
        String rewrite,
        @JsonProperty("used_data")
        List<String> usedData,
        List<ModelAgentRes.Output.Message.Content.Chart> charts,
        @JsonProperty("analysis_type")
        String analysisType,
        @JsonProperty("plan_steps")
        List<PlanStep> planSteps,
        @JsonProperty("related_data")
        List<Integer> relatedData,
        @JsonProperty("vector_metadata")
        List<ModelAgentRes.Output.Message.Content.VectorSource> vectorSources,
        @JsonProperty("web_sources")
        List<ModelAgentRes.Output.Message.Content.Source> webSources

) {
    public record PlanStep(
      String tool
    ){
    }

}

