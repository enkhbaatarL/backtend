package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.TalkReq;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.TalkRes;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.ModelAgentRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotMajorQuestion;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tcm.ChatbotMajorQuestionRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkRepository;
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

    private final ModelAgentApi modelAgentApi;

    private final ChatbotTalkRepository chatbotTalkRepository;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final ChatbotMajorQuestionRepository chatbotMajorQuestionRepository;

    /**
     * 주어진 분과 코드로 필터링된 주요 질의문 목록을 조회합니다.
     *
     * @param subcommitteeCd 분과 코드
     * @return 주요 질의문 목록
     */
    public List<MajorQuestionRes> getMajorQuestions(String subcommitteeCd) {
        List<ChatbotMajorQuestion> majorQuestions = chatbotMajorQuestionRepository.findByCompanySubcommitteeSeparateCdAndIsDeletedFalseOrderByMajorQuestionRnk(subcommitteeCd);
        return MajorQuestionRes.ofList(majorQuestions);
    }

    /**
     * 특정 사용자의 챗봇 대화 목록을 최신순으로 조회합니다.
     *
     * @param user 사용자 객체
     * @return 대화 목록
     */
    public List<TalkRes> getTalks(User user) {
        List<ChatbotTalk> chatbotTalks = chatbotTalkRepository.findByUserAndIsDeletedFalseOrderByCreateDtDesc(user);
        return TalkRes.ofList(chatbotTalks);
    }

    /**
     * 사용자의 챗봇 대화 Q&A 목록을 페이지네이션으로 조회합니다.
     *
     * @param page   페이지 번호
     * @param size   페이지 당 항목 수
     * @param talkId 챗봇 대화 ID
     * @param user   사용자 객체
     * @return 페이지네이션 처리된 Q&A 응답
     */
    public PaginationRes<QnaRes> getTalkQnaList(int page, int size, String talkId, User user) {
        return PaginationRes.toRes(chatbotTalkQnaRepository.findQnaByConditions(page, size, talkId, user));
    }

    /**
     * 챗봇 Q&A의 평가 정보를 업데이트합니다.
     *
     * @param talkId           챗봇 대화 ID
     * @param qnaNo            Q&A 번호
     * @param qnaAssessmentReq 업데이트할 평가 요청 객체
     * @return 업데이트된 Q&A 응답 객체
     */
    @Transactional
    public QnaRes patchQnaAssessment(String talkId, Integer qnaNo, QnaAssessmentReq qnaAssessmentReq) {

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, talkId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [qna]. talkId: %s, qnaNo: %s".formatted(talkId, qnaNo)));

        qna.update(qnaAssessmentReq);

        return QnaRes.of(qna);
    }

    @Transactional
    public Object postQna(TalkReq talkReq, User user) {

        ChatbotTalk talk = chatbotTalkRepository
            .findByChatbotTalkUuidAndIsDeletedFalse(talkReq.talkId().toString())
            .orElseGet(() -> chatbotTalkRepository.save(ChatbotTalk.of(talkReq, user)));

        ChatbotTalkQna qna = chatbotTalkQnaRepository.save(ChatbotTalkQna.of(talkReq, talk));

        // 모델 에이전트 질의
        executeModelAgent(qna);

        return null;
    }

    @Transactional
    public Object postQnaRetry(String talkId, Integer qnaNo, User user) {

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, talkId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [qna]. talkId: %s, qnaNo: %s".formatted(talkId, qnaNo)));

        // 모델 에이전트 질의
        executeModelAgent(qna);

        return null;
    }

    private void executeModelAgent(ChatbotTalkQna qna) {
        try {
            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(qna.getUserQuestionCntt());
            qna.updateResult(modelAgentRes);
        } catch (Exception e) {
            qna.updateResult();
        }
    }

}
