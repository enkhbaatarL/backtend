package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.common.model.TableRes;
import com.sk.common.utils.MarkdownUtil;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record MessageBundle(

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

    public static MessageBundle of(ModelAgentRes.Output message, List<Related> related) {
        String updatedMarkdown = MarkdownUtil.insertTableIds(message.content().text());
        if(related == null){
            related = new ArrayList<>(0);
        }
        return MessageBundle.builder()
            .content(updatedMarkdown)
            .series(Series.ofList(message.content().charts()))
            .build();
    }

    public static MessageBundle of(String text, ModelAgentStreamBody modelAgentStreamBody, List<Related> related) {
        String updatedMarkdown = MarkdownUtil.insertTableIds(text);
        if(related == null){
            related = new ArrayList<>(0);
        }
        return MessageBundle.builder()
            .content(updatedMarkdown)
            .tables(MarkdownUtil.parseTablesWithIds(updatedMarkdown))
            .relatedData(related)
            .build();
    }

}
