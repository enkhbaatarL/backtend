package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.ChatbotMajorQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotMajorQuestionRepository extends JpaRepository<ChatbotMajorQuestion, String> {

    List<ChatbotMajorQuestion> findByCompanySubcommitteeSeparateCdAndIsDeletedFalseOrderByMajorQuestionRnk(String subcommitteeCd);

}
