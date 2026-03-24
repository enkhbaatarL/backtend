package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.common.model.TableRes;
import com.sk.common.utils.MarkdownUtil;
import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record Answer(
    @Schema(description = "내용(MarkDown Format)")
    String content,

    @Schema(description = "시계열(차트) 데이터")
    List<Series> series,

    @Schema(description = "테이블 데이터")
    List<TableRes> tables,

    @Schema(description = "출처")
    List<Source> sources,

    @Schema(description = "연관 데이터")
    List<Related> relatedData
) {
    public static Answer of(ModelAgentRes.Output message) {
        return Answer.builder()
            .content(message.content().text())
            .build();
    }

    public static Answer of(ModelAgentStreamBody modelAgentStreamBody){
        String updatedMarkdown = MarkdownUtil.insertTableIds(modelAgentStreamBody.getValueAsString());
        return Answer.builder()
                .content(updatedMarkdown)
                .tables(MarkdownUtil.parseTablesWithIds(updatedMarkdown))
                .build();
    }

    @SuppressWarnings("unchecked")
    public static Answer of(AnswerRes answerRes){
        StreamRes streamRes = (StreamRes) answerRes.message();
        if (streamRes.data() instanceof Map<?, ?> rawMapData) {
            Map<String, Object> mapData = (Map<String, Object>) rawMapData;
            return Answer.builder()
                .content((String) mapData.get("content"))
                .series((List<Series>) mapData.getOrDefault("series", Collections.emptyList()))
                .tables((List<TableRes>) mapData.getOrDefault("tables", Collections.emptyList()))
                .sources((List<Source>) mapData.getOrDefault("sources", Collections.emptyList()))
                .relatedData((List<Related>) mapData.getOrDefault("relatedData", Collections.emptyList()))
                .build();
        }

        return Answer.builder()
            .content(String.valueOf(streamRes.data()))
            .build();
    }

    public static Answer of(String message) {
        return Answer.builder().content(message).build();
    }
}
