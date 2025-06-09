package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ModelAgentRes(
        Output output,
        Metadata metadata) {

    public static ModelAgentRes ofEmpty() {
        return ModelAgentRes.builder()
                .output(null)
                .metadata(null)
                .build();
    }

    public record Output(
            List<Message> messages) {

        public record Message(
                Content content,

                @JsonProperty("additional_kwargs") AdditionalKwargs additionalKwargs,

                @JsonProperty("response_metadata") ResponseMetadata responseMetadata,

                String type,
                String name,
                String id,
                boolean example,

                @JsonProperty("tool_calls") List<Object> toolCalls,

                @JsonProperty("invalid_tool_calls") List<Object> invalidToolCalls,

                @JsonProperty("usage_metadata") UsageMetadata usageMetadata) {

            public record Content(
                    String text,
                    List<Chart> charts,
                    List<Source> sources,
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

            }

            public record AdditionalKwargs(
                    Object refusal) {
            }

            public record ResponseMetadata(
                    @JsonProperty("token_usage") TokenUsage tokenUsage,

                    @JsonProperty("model_name") String modelName,

                    @JsonProperty("system_fingerprint") String systemFingerprint,

                    @JsonProperty("finish_reason") String finishReason,

                    Object logprobs) {

                public record TokenUsage(
                        @JsonProperty("completion_tokens") int completionTokens,

                        @JsonProperty("prompt_tokens") int promptTokens,

                        @JsonProperty("total_tokens") int totalTokens,

                        @JsonProperty("completion_tokens_details") CompletionTokensDetails completionTokensDetails,

                        @JsonProperty("prompt_tokens_details") PromptTokensDetails promptTokensDetails) {

                    public record CompletionTokensDetails(
                            @JsonProperty("accepted_prediction_tokens") int acceptedPredictionTokens,

                            @JsonProperty("audio_tokens") int audioTokens,

                            @JsonProperty("reasoning_tokens") int reasoningTokens,

                            @JsonProperty("rejected_prediction_tokens") int rejectedPredictionTokens) {
                    }

                    public record PromptTokensDetails(
                            @JsonProperty("audio_tokens") int audioTokens,

                            @JsonProperty("cached_tokens") int cachedTokens) {
                    }
                }
            }

            public record UsageMetadata(
                    @JsonProperty("input_tokens") int inputTokens,

                    @JsonProperty("output_tokens") int outputTokens,

                    @JsonProperty("total_tokens") int totalTokens,

                    @JsonProperty("input_token_details") InputTokenDetails inputTokenDetails,

                    @JsonProperty("output_token_details") OutputTokenDetails outputTokenDetails) {

                public record InputTokenDetails(
                        int audio,

                        @JsonProperty("cache_read") int cacheRead) {
                }

                public record OutputTokenDetails(
                        int audio,
                        int reasoning) {
                }
            }
        }
    }

    public record Metadata(
            @JsonProperty("run_id") String runId,

            @JsonProperty("feedback_tokens") List<Object> feedbackTokens) {
    }
}
