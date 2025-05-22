package com.sk.domain.mi.dto.response.agent;

import com.sk.domain.shared.entity.master.tcm.ChatbotMajorQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record MajorQuestionRes(

    @Schema(description = "주요 질의문 번호", example = "1004")
    Integer majorQuestionNo,

    @Schema(description = "질의문 순번", example = "1")
    Integer questionRank,

    @Schema(description = "주요 질의문", example = "삼성전자 주가는 왜 오를 생각을 안하죠?")
    String majorQuestion,

    @Schema(description = "분과코드", example = "SKT")
    String subcommitteeCd

) {

    public static MajorQuestionRes of(ChatbotMajorQuestion entity) {
        return MajorQuestionRes.builder()
            .majorQuestionNo(entity.getChatbotMajorQuestionNo())
            .questionRank(entity.getMajorQuestionRnk())
            .majorQuestion(entity.getMajorQuestionCntt())
            .subcommitteeCd(entity.getMarketIndexAnalysisTargetCd())
            .build();
    }

    public static List<MajorQuestionRes> ofList(List<ChatbotMajorQuestion> entities) {
        return entities.stream().map(MajorQuestionRes::of).toList();
    }

}
