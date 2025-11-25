package com.sk.domain.mi.dto.response.agent.talk;


import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AnswerRes(

        @Schema(description = "메시지")
        Object message,

        @Schema(description = "스트림 메타데이터 (스트리밍 응답일 경우)")
        StreamMetadata metadata

) {

    public static AnswerRes of(ModelAgentRes.Output message) {
        return AnswerRes.builder()
                .message(message)
                .metadata(null)  // 비스트리밍은 metadata 없음
                .build();
    }

    // ===== 기존 정적 팩토리 메서드 (비스트리밍) =====

    public static AnswerRes of(ModelAgentStreamBody streamBody) {
        return AnswerRes.builder()
            .message(streamBody.getValueAsString())
            .metadata(StreamMetadata.builder()
                .state(streamBody.state())
                .data(streamBody.data())
                .node(streamBody.node())
                .streaming(isStreamingType(streamBody.data()))
                .build())
            .build();
    }

    public static AnswerRes of(ModelAgentStreamBody streamBody, StreamRes streamRes) {
        return AnswerRes.builder()
            .message(streamRes)
            .metadata(StreamMetadata.builder()
                .state(streamBody.state())
                .data(streamBody.data())
                .node(streamBody.node())
                .streaming(isStreamingType(streamBody.data()))
                .build())
            .build();
    }

    public static AnswerRes of(ModelAgentStreamBody streamBody, StreamRes streamRes,  String data) {
        return AnswerRes.builder()
            .message(streamRes)
            .metadata(StreamMetadata.builder()
                .state(streamBody.state())
                .data(data)
                .node(streamBody.node())
                .streaming(isStreamingType(streamBody.data()))
                .build())
            .build();
    }

    // ===== 스트리밍용 정적 팩토리 메서드 (수정) =====

    /**
     * 스트리밍 타입인지 확인 (analysis_chunk만 true)
     */
    private static boolean isStreamingType(String type) {
        return "chunk".equals(type);
    }

    /**
     * 스트림 이벤트의 메타데이터
     */
    @Builder
    public record StreamMetadata(

            @Schema(description = "이벤트 상태", example = "start", allowableValues = {"start", "progress", "error", "complete"})
            String state,

            @Schema(description = "데이터 타입", example = "message", allowableValues = {"message", "series", "table", "chunk"})
            String data,

            @Schema(description = "노드명", example = "PreparePromptNode")
            String node,

            @Schema(description = "스트리밍 여부 - true면 프론트에서 누적 처리 필요")
            Boolean streaming
    ) {
        public boolean isComplete() {
            return "complete".equals(state);
        }

    }

}
