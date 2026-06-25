package com.sk.domain.mi.service.agent.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * vector_data 타입 이벤트를 처리하는 Handler
 * Vector search 결과를 파싱하고 goodsDataNo를 조회하여 추가합니다.
 *
 * 예시 이벤트:
 * {
 *   "node": "WebSearchNode",
 *   "data": "rdb_data",
 *   "state": "progress",
 *   "value": {
 *     "qnaNo": 2714,
 *     "data": [
 *       {
 *         "source": "Hong Leong Investment Bank Bhd",
 *         "title": "Traders_Brief_0620",
 *         "gds_no": 70008,
 *         "gds_data_dstg_id": "116049757"
 *       }
 *     ]
 *   }
 * }
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RdbDataStreamEventHandler implements StreamEventHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private List<Map<String, Object>> tryParseRdbData(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        // 1) value가 바로 배열 형태인 경우: [{...}, {...}]
        try {
            return objectMapper.readValue(rawValue, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            // 배열 형태 파싱 실패, wrapper 객체 형태로 재시도
            log.debug("rdb_data 배열 파싱 실패, wrapper 객체 형태로 재시도: {}", e.getMessage());
        }

        // 2) value가 wrapper 객체인 경우: {"data": [{...}]}
        try {
            Map<String, Object> wrapper = objectMapper.readValue(
                rawValue,
                new TypeReference<Map<String, Object>>() {}
            );

            Object data = wrapper.get("data");
            if (!(data instanceof List<?> list)) {
                return null;
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> mapItem) {
                    result.add((Map<String, Object>) mapItem);
                }
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public String getEventDataType() {
        return "rdb_data";
    }

    @Override
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody) {

        log.debug("RdbDataStreamEventHandler - state: {}, data: {}, node: {}",
            streamBody.state(), streamBody.data(), streamBody.node());

        String rawValue = streamBody.value();
        List<Map<String, Object>> dataList = tryParseRdbData(rawValue);

        if (rawValue == null || rawValue.trim().isEmpty()
            || (dataList != null && dataList.isEmpty())) {
            log.debug("No rdb data found for qnaNo: {}", qna.getChatbotTalkQnaNo());
            StreamRes streamRes = StreamRes.of(qna, streamBody);
            return AnswerRes.of(streamBody, streamRes, "rdb_data");
        }

        // StreamRes 생성:
        // - 파싱 성공 시 List<Map> 형태 유지
        // - 파싱 실패 시 rawValue(String) 그대로 전달
        Object streamData = dataList != null ? dataList : rawValue;
        StreamRes streamRes = StreamRes.builder()
            .qnaNo(qna.getChatbotTalkQnaNo())
            .data(streamData)
            .build();

        return AnswerRes.of(streamBody, streamRes, "rdb_data");
    }

}
