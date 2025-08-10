package com.sk.domain.mi.service.agent;

import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotTalkService {

    private final ChatbotTalkRepository chatbotTalkRepository;

    /**
     * 채팅방을 찾거나, 없으면 새로 생성하여 반환합니다.
     * 이 메소드는 REQUIRES_NEW 전파 속성으로 호출 즉시 별도의 트랜잭션에서 실행되고 커밋됩니다.
     * 이를 통해 동시 요청 시의 경쟁 상태(Race Condition)를 방지합니다.
     *
     * @param chatRoomId 채팅룸 ID
     * @param talkReq    최초 생성 시 필요한 요청 데이터
     * @param user       사용자
     * @return 찾거나 생성된 ChatbotTalk 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatbotTalk findOrCreateTalk(UUID chatRoomId, QnaReq.Insert talkReq, User user) {
        return chatbotTalkRepository
            .findByChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .orElseGet(() -> chatbotTalkRepository.save(ChatbotTalk.of(chatRoomId.toString(), talkReq, user)));
    }

}
