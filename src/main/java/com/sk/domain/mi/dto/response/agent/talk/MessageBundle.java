package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.common.model.TableRes;
import com.sk.common.utils.MarkdownUtil;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record MessageBundle(

        @Schema(description = "내용(MarkDown Format)") String content,

        @Schema(description = "시계열(차트) 데이터") List<Series> series,

        @Schema(description = "테이블 데이터") List<TableRes> tables,

        @Schema(description = "출처") List<Source> sources,

        @Schema(description = "연관 데이터") List<Related> relatedData

) {

    public static MessageBundle of(ModelAgentRes.Message message, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        String updatedMarkdown = MarkdownUtil.insertTableIds(message.content().text());
        return MessageBundle.builder()
                .content(updatedMarkdown)
                .series(Series.ofList(message.content().charts()))
                .tables(MarkdownUtil.parseTablesWithIds(updatedMarkdown))
                .sources(Source.ofList(message.content().sources()))
                .relatedData(Related.ofList(message.content().relatedData(), gdsMap))
                .build();
    }

}
