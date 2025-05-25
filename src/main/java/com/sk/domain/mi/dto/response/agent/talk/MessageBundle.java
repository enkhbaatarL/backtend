package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.common.model.TableRes;
import com.sk.common.utils.MarkdownUtil;
import com.sk.domain.shared.client.dto.ModelAgentRes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record MessageBundle(

    @Schema(description = "내용(MarkDown Format)", example = "")
    String content,

    @Schema(description = "시계열 데이터", example = "")
    List<Series> series,

    @Schema(description = "테이블 데이터", example = "")
    List<TableRes> tables

) {

    public static MessageBundle of(ModelAgentRes.Output.Message message) {
        String updatedMarkdown = MarkdownUtil.insertTableIds(message.content().text());
        return MessageBundle.builder()
            .content(updatedMarkdown)
            .series(Series.ofList(message.content().charts()))
            .tables(MarkdownUtil.parseTablesWithIds(updatedMarkdown))
            .build();
    }

}
