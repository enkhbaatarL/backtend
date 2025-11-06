package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record AnswerRes(

        @Schema(description = "메시지 번들 리스트")
        List<MessageBundle> bundles,

        @Schema(description = "스트림 메타데이터 (스트리밍 응답일 경우)")
        StreamMetadata metadata

) {

    public static AnswerRes of(List<ModelAgentRes.Output.Message> messages, List<Related> related) {
        return AnswerRes.builder()
                .bundles(messages
                        .stream()
                        .map(message -> MessageBundle.of(message, related))
                        .toList())
                .metadata(null)  // 비스트리밍은 metadata 없음
                .build();
    }

    // ===== 기존 정적 팩토리 메서드 (비스트리밍) =====

    public static AnswerRes of(String text, ModelAgentStreamBody streamBody, List<Related> related) {
        return AnswerRes.builder()
            .bundles(List.of(MessageBundle.of(text, streamBody, related)))
            .metadata(StreamMetadata.builder()
                .type(streamBody.type())
                .node(streamBody.node())
                .streaming(isStreamingType(streamBody.type()))
                .build())
            .build();
    }

    // ===== 스트리밍용 정적 팩토리 메서드 (수정) =====

    /**
     * 스트리밍 타입인지 확인 (analysis_chunk만 true)
     */
    private static boolean isStreamingType(String type) {
        return "analysis_chunk".equals(type);
    }

    /**
     * 스트림 이벤트의 메타데이터
     */
    @Builder
    public record StreamMetadata(

            @Schema(description = "이벤트 타입", example = "info", allowableValues = {"info", "analysis_chunk", "analysis_complete"})
            String type,

            @Schema(description = "노드명", example = "DataAnalysisNode")
            String node,

            @Schema(description = "스트리밍 여부 - true면 프론트에서 누적 처리 필요")
            Boolean streaming
    ) {
        /**
         * info 타입인지 확인
         */
        public boolean isInfo() {
            return "info".equals(type);
        }

        /**
         * analysis_chunk 타입인지 확인 (누적 필요)
         */
        public boolean isChunk() {
            return "analysis_chunk".equals(type);
        }

        /**
         * analysis_complete 타입인지 확인 (누적 종료)
         */
        public boolean isComplete() {
            return "analysis_complete".equals(type);
        }
    }
}
