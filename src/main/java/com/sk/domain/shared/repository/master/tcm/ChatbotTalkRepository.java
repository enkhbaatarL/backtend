package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tmd.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatbotTalkRepository extends JpaRepository<ChatbotTalk, String> {

    List<ChatbotTalk> findByUserAndIsDeletedFalseOrderByCreateDtDesc(User user);

    Optional<ChatbotTalk> findByChatbotTalkUuidAndIsDeletedFalse(String uuid);

}
