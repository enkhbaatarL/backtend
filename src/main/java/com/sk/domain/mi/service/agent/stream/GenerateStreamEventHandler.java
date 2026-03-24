package com.sk.domain.mi.service.agent.stream;

import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Series;
import com.sk.domain.mi.dto.response.agent.talk.Source;
import com.sk.domain.mi.service.agent.MariAgentResParser;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.common.model.TableRes;
import com.sk.common.utils.MarkdownUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * analysis_complete 타입 이벤트를 처리하는 Handler
 * LLM 생성이 완료되면 최종 결과를 DB에 저장하고 토큰 사용량을 기록합니다.
 *
 * 예시 이벤트:
 * {
 *   "node": "DataAnalysisNode",
 *   "type": "analysis_complete",
 *   "value": "전체 생성된 텍스트...",
 *   "charts": [...],
 *   "related_data": [1, 2, 3],
 *   "usage": {...}
 * }
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateStreamEventHandler implements StreamEventHandler {

    private final MariAgentResParser mariAgentResParser;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final TokenQuotaGuard tokenQuotaGuard;

    @Override
    public String getEventDataType() {
        return "generate";
    }

    @Override
    @Transactional
    public AnswerRes createAnswerRes(ChatbotTalkQna qna, ModelAgentStreamBody streamBody) {

        log.debug("GenerateStreamEventHandler - state: {} data:{} node: {}, value: {}",
            streamBody.state(), streamBody.data(), streamBody.node(),
            streamBody.value());

        StreamRes streamRes = StreamRes.builder()
            .qnaNo(qna.getChatbotTalkQnaNo())
            .data(convertToClientData(streamBody))
            .build();
        AnswerRes answerRes = AnswerRes.of(streamBody, streamRes, "fulltext");

        // DB에 완료 결과 저장
        qna.updateCompleteResult(answerRes, streamBody);
        chatbotTalkQnaRepository.save(qna);

        return answerRes;
    }

    private Map<String, Object> convertToClientData(ModelAgentStreamBody streamBody) {
        String markdown = streamBody.getValueAsString() == null ? "" : streamBody.getValueAsString();
        String updatedMarkdown = MarkdownUtil.insertTableIds(markdown);

        ModelAgentStreamBody.Segment segment = streamBody.segment();
        List<Series> series = extractSeries(segment);
        List<TableRes> tables = extractTimelineTables(segment, updatedMarkdown);
        List<Source> sources = extractWebSources(segment);
        List<Related> relatedData = mariAgentResParser.prepareAnswerDataResources(streamBody);

        return Map.of(
            "content", updatedMarkdown,
            "series", series,
            "tables", tables,
            "sources", sources,
            "relatedData", relatedData
        );
    }

    private List<Series> extractSeries(ModelAgentStreamBody.Segment segment) {
        if (segment == null || segment.timeseriesInsights() == null) {
            return Collections.emptyList();
        }

        return Series.ofStreamChartList(segment.timeseriesInsights().charts());
    }

    private List<Source> extractWebSources(ModelAgentStreamBody.Segment segment) {
        if (segment == null || segment.sources() == null || segment.sources().web() == null) {
            return Collections.emptyList();
        }
        return segment.sources().web().stream()
            .map(web -> Source.builder()
                .url(web.url())
                .sourceNm(web.sourceNm())
                .build())
            .toList();
    }

    private List<TableRes> extractTimelineTables(ModelAgentStreamBody.Segment segment, String markdown) {
        if (segment == null
            || segment.timeseriesInsights() == null
            || segment.timeseriesInsights().timelineHighlights() == null) {
            return MarkdownUtil.parseTablesWithIds(markdown);
        }

        ModelAgentStreamBody.Segment.TimelineHighlights timeline =
            segment.timeseriesInsights().timelineHighlights();
        if (timeline.columns() == null || timeline.columns().isEmpty()) {
            return MarkdownUtil.parseTablesWithIds(markdown);
        }

        List<TableRes.Column> headers = timeline.columns().stream()
            .map(column -> TableRes.Column.of(column.key(), column.label()))
            .toList();

        List<TableRes.Row> rows = timeline.rows() == null
            ? Collections.emptyList()
            : timeline.rows().stream()
                .map(row -> {
                    Map<String, String> valueMap = new LinkedHashMap<>();
                    valueMap.put("date", row.date());
                    valueMap.put("value", row.value() == null ? "" : String.valueOf(row.value()));
                    valueMap.put("event", row.event());

                    List<String> cells = timeline.columns().stream()
                        .map(column -> valueMap.getOrDefault(column.key(), ""))
                        .toList();
                    return TableRes.Row.of(cells);
                })
                .toList();

        if (rows.isEmpty()) {
            return MarkdownUtil.parseTablesWithIds(markdown);
        }

        return List.of(TableRes.of(null, headers, rows));
    }

    /**
     * ModelAgentStreamBody를 ModelAgentRes.Output 형식으로 변환
     *
     * @param streamBody 스트림 데이터
     * @param completedText 완료된 전체 텍스트
     * @return ModelAgentRes.Output 객체
     */
    private ModelAgentRes.Output convertToOutput(ModelAgentStreamBody streamBody, String completedText) {

        // Content 생성
        ModelAgentRes.Output.Content content = new ModelAgentRes.Output.Content(
            completedText,
            null
        );

        // Message 생성
        ModelAgentRes.Output message = new ModelAgentRes.Output(
            content,
            null,  // analysis_complete에 usage 정보가 포함될 수 있음
            "assistant",  // 기본 타입
            null  // id는 필요시 생성
        );

        // Output 생성
        return new ModelAgentRes.Output(
            message.content(),
            null,
            "assistant",
            null
        );
    }
}
