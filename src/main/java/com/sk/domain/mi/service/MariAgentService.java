package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.TalkRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotMajorQuestion;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tcm.ChatbotMajorQuestionRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MariAgentService {

    private final ChatbotTalkRepository chatbotTalkRepository;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final ChatbotMajorQuestionRepository chatbotMajorQuestionRepository;

    public List<MajorQuestionRes> getMajorQuestions(String subcommitteeCd) {
        List<ChatbotMajorQuestion> majorQuestions = chatbotMajorQuestionRepository.findByCompanySubcommitteeSeparateCdAndIsDeletedFalseOrderByMajorQuestionRnk(subcommitteeCd);
        return MajorQuestionRes.ofList(majorQuestions);
    }

    public List<TalkRes> getTalks(User user) {
        List<ChatbotTalk> chatbotTalks = chatbotTalkRepository.findByUserAndIsDeletedFalseOrderByCreateDtDesc(user);
        return TalkRes.ofList(chatbotTalks);
    }

    public PaginationRes<QnaRes> getTalkQnaList(int page, int size, String talkId, User user) {
        return PaginationRes.toRes(chatbotTalkQnaRepository.findQnaByConditions(page, size, talkId, user));
    }

    @Transactional
    public QnaRes patchQnaAssessment(String talkId, Integer qnaNo, QnaAssessmentReq qnaAssessmentReq) {

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, talkId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [qna]. talkId: %s, qnaNo: %s".formatted(talkId, qnaNo)));

        qna.update(qnaAssessmentReq);

        return QnaRes.of(qna);
    }

}
