package com.sk.domain.shared.repository.master.tcm.custom;

import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.shared.entity.master.tmd.User;
import org.springframework.data.domain.Page;

public interface CustomChatbotTalkQnaRepository {

    Page<QnaRes> findQnaByConditions(int page, int size, String talkId, User user);

}
