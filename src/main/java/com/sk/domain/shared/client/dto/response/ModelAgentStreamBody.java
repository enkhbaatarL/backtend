package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

/**
 * Agent 스트림 이벤트의 data 부분을 나타내는 DTO
 *
 * 구조 예시:
 * event: data
 * data: {
 *   "node": "DataAnalysisNode",
 *   "type": "analysis_chunk",
 *   "value": "_specific",
 *   "message": "...",
 *   "charts": [...],
 *   ...
 * }
 */
@Builder
public record ModelAgentStreamBody(
        /**
         * 현재 실행 중인 노드
         * 예: "PreparePromptNode", "DataAnalysisNode", "PlannerNode"
         */
        String node,

        /**
         * 이벤트 타입
         * - "info": 진행 상태 정보
         * - "analysis_chunk": LLM 생성 청크 (스트리밍)
         * - "analysis_complete": LLM 생성 완료
         */
        String type,

        /**
         * 이벤트의 주요 값
         * - info 타입: 진행 상태 메시지 (예: "분석에 사용할 프롬프트 리소스를 준비중입니다...")
         * - analysis_chunk: LLM 생성 텍스트 조각
         * - analysis_complete: 전체 완성된 텍스트
         */
        String value,

        // ===== 기존 필드 =====
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
        List<ModelAgentRes.Output.Message.Content.Source> webSources,

        /**
         * analysis_complete 시 포함될 수 있는 토큰 사용량 정보
         */
        ModelAgentRes.Output.Message.Usage usage

) {
    /**
     * Type enum으로 변환
     */
    public Type getTypeEnum() {
        return Type.fromValue(type);
    }

    /**
     * Node enum으로 변환
     */
    public Node getNodeEnum() {
        return Node.fromValue(node);
    }

    /**
     * info 타입 이벤트인지 확인
     */
    public boolean isInfoType() {
        return Type.INFO.value.equals(type);
    }

    /**
     * analysis_chunk 타입 이벤트인지 확인
     */
    public boolean isAnalysisChunkType() {
        return Type.ANALYSIS_CHUNK.value.equals(type);
    }

    /**
     * analysis_complete 타입 이벤트인지 확인
     */
    public boolean isAnalysisCompleteType() {
        return Type.ANALYSIS_COMPLETE.value.equals(type);
    }

    /**
     * 표시할 텍스트를 반환
     * - value가 있으면 value
     * - 없으면 message
     */
    public String getDisplayText() {
        return value != null && !value.isEmpty() ? value : message;
    }

    /**
     * 이벤트 타입 열거형
     */
    public enum Type {
        INFO("info"),
        ANALYSIS_CHUNK("analysis_chunk"),
        ANALYSIS_COMPLETE("analysis_complete");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type fromValue(String value) {
            if (value == null) return null;
            for (Type type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 노드 타입 열거형
     */
    public enum Node {
        PREPARE_PROMPT_NODE("PreparePromptNode"),
        REWRITE_QUERY_NODE("RewriteQueryNode"),
        PLANNER_NODE("PlannerNode"),
        DATA_COLLECT_NODE("DataCollectNode"),
        DATA_ANALYSIS_NODE("DataAnalysisNode"),
        WEB_SEARCH_NODE("WebSearchNode"),
        RDB_SEARCH_NODE("RdbSearchNode"),
        VECTOR_SEARCH_NODE("VectorSearchNode"),
        TIMESERIES_TREND_NODE("TimeseriesTrendNode"),
        EVENT_CAUSALITY_NODE("EventCausalityNode"),
        EFFECT_SIZE_NODE("EffectSizeNode");

        private final String value;

        Node(String value) {
            this.value = value;
        }

        public static Node fromValue(String value) {
            if (value == null) return null;
            for (Node node : values()) {
                if (node.value.equals(value)) {
                    return node;
                }
            }
            return null;
        }

        public String getValue() {
            return value;
        }
    }

    public record PlanStep(
            String tool,
            String description
    ) {
    }
}
