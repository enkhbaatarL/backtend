package com.sk.domain.mi.dto.response.agent;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record StreamRes(

    @Schema(description = "질의응답 번호", example = "1004")
    Integer qnaNo,

    @Schema(description = "data", example = "")
    Object data

) {

    @QueryProjection
    public StreamRes(ChatbotTalkQna entity) {
        this(
            entity.getChatbotTalkQnaNo(),
            AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            )
        );
    }

    @Builder
    public StreamRes(Integer qnaNo, Object data) {
        this.qnaNo = qnaNo;
        this.data = data;
    }

    public static StreamRes of(ChatbotTalkQna qna) {
        return new StreamRes(qna);
    }

    public static StreamRes ofCompact(ChatbotTalkQna qna) {
        return StreamRes.builder()
            .qnaNo(qna.getChatbotTalkQnaNo())
            .data(AuditInfo.ofSimple(
                qna.getCreateUserId(),
                qna.getCreateDt(),
                qna.getModifyUserId(),
                qna.getModifyDt())
            )
            .build();
    }

    public static StreamRes of(ChatbotTalkQna qna, ModelAgentStreamBody body) {
        return StreamRes.builder()
            .qnaNo(qna.getChatbotTalkQnaNo())
            .data(body.value())
            .build();
    }

}
