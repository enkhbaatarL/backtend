package com.sk.domain.shared.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ModelAgentRes(

        List<Message> messages,
        Usage usage

) {

    public record Message(
            Content content,
            String type,
            String id) {

        public record Content(
                String text,
                List<Chart> charts,
                List<Source> sources,

                @JsonProperty("vector_sources") List<VectorSource> vectorSources,

                @JsonProperty("related_data") List<Integer> relatedData) {

            public record Chart(
                    int order,
                    String legend,
                    List<String> date,
                    List<Double> value) {
            }

            public record Source(
                    String url,
                    @JsonProperty("source_nm") String sourceNm) {
            }

            /**
             * 새로 추가된 벡터 소스 정보를 담습니다.
             */
            public record VectorSource(
                    String source,
                    String title) {
            }
        }
    }

    public record Usage(

            @JsonProperty("completion_tokens") int completionTokens,

            @JsonProperty("prompt_tokens") int promptTokens,

            @JsonProperty("total_tokens") int totalTokens,

            @JsonProperty("completion_tokens_details") CompletionTokensDetails completionTokensDetails,

            @JsonProperty("prompt_tokens_details") PromptTokensDetails promptTokensDetails

    ) {
        public record CompletionTokensDetails(

                @JsonProperty("accepted_prediction_tokens") int acceptedPredictionTokens,

                @JsonProperty("audio_tokens") int audioTokens,

                @JsonProperty("reasoning_tokens") int reasoningTokens,

                @JsonProperty("rejected_prediction_tokens") int rejectedPredictionTokens

        ) {
        }

        public record PromptTokensDetails(

                @JsonProperty("audio_tokens") int audioTokens,

                @JsonProperty("cached_tokens") int cachedTokens

        ) {
        }

    }

}