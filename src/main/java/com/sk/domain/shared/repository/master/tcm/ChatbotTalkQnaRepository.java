package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.repository.master.tcm.custom.CustomChatbotTalkQnaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatbotTalkQnaRepository extends JpaRepository<ChatbotTalkQna, Integer>, CustomChatbotTalkQnaRepository {

    Optional<ChatbotTalkQna> findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(Integer qnaNo, String talkId);

}
