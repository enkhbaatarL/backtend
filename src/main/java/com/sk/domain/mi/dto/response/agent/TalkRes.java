package com.sk.domain.mi.dto.response.agent;

import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record TalkRes(

    @Schema(description = "챗봇대화 ID", example = "1004")
    String talkId,

    @Schema(description = "제목", example = "삼성전자 주가는 왜 이렇게 끝도 없이 내려갈까요?")
    String title,

    @Schema(description = "대화시작일자", example = "20250101")
    String talkBeginDt,

    @Schema(description = "최종대화일자", example = "20250102")
    String finalTalkDt

) {

    public static TalkRes of(ChatbotTalk entity) {
        return TalkRes.builder()
            .talkId(entity.getChatbotTalkUuid())
            .title(entity.getTalkTitleNm())
            .talkBeginDt(entity.getTalkBeginDt())
            .finalTalkDt(entity.getFinalTalkDt())
            .build();
    }

    public static List<TalkRes> ofList(List<ChatbotTalk> entities) {
        return entities.stream().map(TalkRes::of).collect(Collectors.toList());
    }

}
