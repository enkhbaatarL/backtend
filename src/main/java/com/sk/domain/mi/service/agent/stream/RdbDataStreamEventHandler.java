package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
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

    @Override
    public String getEventDataType() {
        return "rdb_data";
    }

    @Override
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody) {

        log.debug("RdbDataStreamEventHandler - state: {}, data: {}, node: {}",
            streamBody.state(), streamBody.data(), streamBody.node());

        Object value = streamBody.value();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) value;

        if (dataList == null || dataList.isEmpty()) {
            log.debug("No rdb data found for qnaNo: {}", qna.getChatbotTalkQnaNo());
            StreamRes streamRes = StreamRes.of(qna, streamBody);
            return AnswerRes.of(streamBody, streamRes, "rdb_data");
        }



        // StreamRes 생성 (원본 value 사용)
        StreamRes streamRes = StreamRes.builder()
            .qnaNo(qna.getChatbotTalkQnaNo())
            .data(value)
            .build();

        return AnswerRes.of(streamBody, streamRes, "rdb_data");
    }

}
