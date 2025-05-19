package com.sk.domain.mi.dto.response.agent;

import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record ChatRoomRes(

    @Schema(description = "채팅룸 ID", example = "132f1ff8-7265-48b8-8106-3ff52d025097")
    String chatRoomId,

    @Schema(description = "채팅룸 제목", example = "삼성전자 주가는 왜 이렇게 끝도 없이 내려갈까요?")
    String title,

    @Schema(description = "채팅시작 일자", example = "20250101")
    String chatBeginDt,

    @Schema(description = "최종채팅 일자", example = "20250102")
    String finalChatDt

) {

    public static ChatRoomRes of(ChatbotTalk entity) {
        return ChatRoomRes.builder()
            .chatRoomId(entity.getChatbotTalkUuid())
            .title(entity.getTalkTitleNm())
            .chatBeginDt(entity.getTalkBeginDt())
            .finalChatDt(entity.getFinalTalkDt())
            .build();
    }

    public static List<ChatRoomRes> ofList(List<ChatbotTalk> entities) {
        return entities.stream().map(ChatRoomRes::of).collect(Collectors.toList());
    }

}
