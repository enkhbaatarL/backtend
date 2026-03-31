package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record ModelAgentRes(
    Output output,
    Metadata metadata
) {

    public record Output(
        Content content,
        Usage usage,
        String type,
        String id
    ) {

        public record Content(
            String text,
            List<Content.Chart> charts
        ) {

            @Builder
            public record Chart(
                int order,
                String type,
                String legend,
                List<String> date,
                List<Double> value
            ) {

            }
        }

        public record Usage(

            @JsonProperty("completion_tokens")
            int completionTokens,

            @JsonProperty("prompt_tokens")
            int promptTokens,

            @JsonProperty("total_tokens")
            int totalTokens,

            @JsonProperty("completion_tokens_details")
            CompletionTokensDetails completionTokensDetails,

            @JsonProperty("prompt_tokens_details")
            PromptTokensDetails promptTokensDetails

        ) {

            public record CompletionTokensDetails(

                @JsonProperty("accepted_prediction_tokens")
                int acceptedPredictionTokens,

                @JsonProperty("audio_tokens")
                int audioTokens,

                @JsonProperty("reasoning_tokens")
                int reasoningTokens,

                @JsonProperty("rejected_prediction_tokens")
                int rejectedPredictionTokens

            ) {

            }

            public record PromptTokensDetails(

                @JsonProperty("audio_tokens")
                int audioTokens,

                @JsonProperty("cached_tokens")
                int cachedTokens

            ) {

            }

        }

    }

    public record Metadata(
        @JsonProperty("run_id")
        String runId,

        @JsonProperty("feedback_tokens")
        List<Object> feedbackTokens
    ) {


    }

}


