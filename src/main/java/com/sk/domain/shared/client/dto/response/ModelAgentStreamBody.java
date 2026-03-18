package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent 스트림 이벤트의 data 부분을 나타내는 DTO
 *
 * 구조 예시:
 * event: data
 * data: {
 *   "state": "start | progress | error | complete",
 *   "data": "message | series | table | chunk | vector_data",
 *   "node": "PreparePromptNode | DataAnalysisNode | VectorSearchNode | ...",
 *   "value": "string or dict depending on data type"
 * }
 */
@Slf4j
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelAgentStreamBody(
        /*
         * 이벤트 상태
         * - "start": 시작
         * - "progress": 진행 중
         * - "error": 오류 발생
         * - "complete": 완료
         */
        String state,

        /*
         * 데이터 타입
         * - "message": 일반 메시지
         * - "series": 시계열 데이터
         * - "table": 테이블 데이터
         * - "chunk": 스트리밍 청크
         * - "vector_data": 벡터 검색 데이터
         */
        String data,

        /*
         * 현재 실행 중인 노드
         * 예: "PreparePromptNode", "RewriteQueryNode", "PlannerNode",
         *     "DataCollectNode", "DataAnalysisNode", "RdbSearchNode",
         *     "WebSearchNode", "VectorSearchNode", "TimeseriesTrendNode"
         */
        String node,

        /*
         * 이벤트의 값
         * - data가 "message" 또는 "chunk"인 경우: String
         * - data가 "series", "table", "vector_data"인 경우: Map/Object 형태
         * <p>
         * Object 타입으로 받아 런타임에 타입 체크 후 처리
         */
        Object value

) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * State enum으로 변환
     */
    public State getStateEnum() {
        return State.fromValue(state);
    }

    /**
     * Data type enum으로 변환
     */
    public DataType getDataTypeEnum() {
        return DataType.fromValue(data);
    }

    /**
     * Node enum으로 변환
     */
    public Node getNodeEnum() {
        return Node.fromValue(node);
    }

    /**
     * value가 String인 경우 반환
     */
    public String getValueAsString() {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }

    /**
     * value가 Map인 경우 반환
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getValueAsMap() {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    /**
     * value를 특정 타입으로 변환
     */
    public <T> T getValueAs(Class<T> clazz) {
        try {
            if (value == null) {
                return null;
            }
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            // Map이나 List 형태인 경우 ObjectMapper를 사용해 변환
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.error("Failed to convert value to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 시작 상태인지 확인
     */
    public boolean isStartState() {
        return State.START.value.equals(state);
    }

    /**
     * 진행 중 상태인지 확인
     */
    public boolean isProgressState() {
        return State.PROGRESS.value.equals(state);
    }

    /**
     * 오류 상태인지 확인
     */
    public boolean isErrorState() {
        return State.ERROR.value.equals(state);
    }

    /**
     * 완료 상태인지 확인
     */
    public boolean isCompleteState() {
        return State.COMPLETE.value.equals(state);
    }

    /**
     * 메시지 데이터 타입인지 확인
     */
    public boolean isMessageData() {
        return DataType.MESSAGE.value.equals(data);
    }

    /**
     * 청크 데이터 타입인지 확인 (스트리밍)
     */
    public boolean isChunkData() {
        return DataType.CHUNK.value.equals(data);
    }

    /**
     * 시리즈 데이터 타입인지 확인
     */
    public boolean isSeriesData() {
        return DataType.SERIES.value.equals(data);
    }

    /**
     * 테이블 데이터 타입인지 확인
     */
    public boolean isTableData() {
        return DataType.TABLE.value.equals(data);
    }

    /**
     * 벡터 데이터 타입인지 확인
     */
    public boolean isVectorData() {
        return DataType.VECTOR_DATA.value.equals(data);
    }

    /**
     * 벡터 데이터 타입인지 확인
     */
    public boolean isRdbData() {
        return DataType.RDB_DATA.value.equals(data);
    }

    /**
     * 스트리밍 처리가 필요한지 확인
     * chunk 데이터이면서 progress 상태일 때 true
     */
    public boolean isStreamingRequired() {
        return isChunkData() && isProgressState();
    }

    /**
     * 표시할 텍스트를 반환
     */
    public String getDisplayText() {
        if (isMessageData() || isChunkData()) {
            return getValueAsString();
        }
        return null;
    }

    /**
     * 이벤트 상태 열거형
     */
    @Getter
    public enum State {
        START("start"),
        PROGRESS("progress"),
        ERROR("error"),
        COMPLETE("complete");

        private final String value;

        State(String value) {
            this.value = value;
        }

        public static State fromValue(String value) {
            if (value == null) return null;
            for (State state : values()) {
                if (state.value.equals(value)) {
                    return state;
                }
            }
            return null;
        }

    }

    /**
     * 데이터 타입 열거형
     */
    @Getter
    public enum DataType {
        MESSAGE("message"),
        SERIES("series"),
        TABLE("table"),
        CHUNK("chunk"),
        VECTOR_DATA("vector_data"),
        RDB_DATA("rdb_data");

        private final String value;

        DataType(String value) {
            this.value = value;
        }

        public static DataType fromValue(String value) {
            if (value == null) return null;
            for (DataType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }

    }

    /**
     * 노드 타입 열거형
     */
    @Getter
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

    }
}
