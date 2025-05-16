package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.agent.ChatRoomReq;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.ChatRoomRes;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
     * 특정 사용자의 채팅룸 목록을 최신순으로 조회합니다.
     *
     * @param user 사용자 객체
     * @return 채팅룸 목록
     */
    public List<ChatRoomRes> getChatRooms(User user) {
        List<ChatbotTalk> chatbotTalks = chatbotTalkRepository.findByUserAndIsDeletedFalseOrderByFinalTalkDtDesc(user);
        return ChatRoomRes.ofList(chatbotTalks);
    }

    /**
     * 주어진 ID에 해당하는 채팅룸 정보를 업데이트합니다.
     *
     * @param chatRoomId  업데이트할 채팅룸 ID
     * @param chatRoomReq 업데이트 요청 객체
     * @param user        요청한 사용자 객체
     * @return 업데이트된 대화 응답 객체
     */
    @Transactional
    public ChatRoomRes patchChatRoom(UUID chatRoomId, ChatRoomReq chatRoomReq, User user) {

        ChatbotTalk talk = chatbotTalkRepository.findByChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [talk(chat-room)]. chatRoomId: %s".formatted(chatRoomId)));

        validateTalkOwnership(talk, user, chatRoomId.toString());

        talk.update(chatRoomReq);

        return ChatRoomRes.of(talk);
    }

    /**
     * 주어진 ID에 해당하는 채팅룸을 소프트 삭제 처리합니다.
     *
     * @param chatRoomId 삭제할 채팅룸 ID
     * @param user       요청한 사용자 객체
     * @return 삭제된 대화 응답 객체
     */
    @Transactional
    public ChatRoomRes deleteChatRoom(UUID chatRoomId, User user) {

        ChatbotTalk talk = chatbotTalkRepository.findByChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [talk(chat-room)]. chatRoomId: %s".formatted(chatRoomId.toString())));

        validateTalkOwnership(talk, user, chatRoomId.toString());

        talk.softDelete();

        return ChatRoomRes.of(talk);
    }

    /**
     * 사용자의 채팅룸 Q&A 목록을 페이지네이션으로 조회합니다.
     *
     * @param page       페이지 번호
     * @param size       페이지 당 항목 수
     * @param chatRoomId 채팅룸 ID
     * @param user       사용자 객체
     * @return 페이지네이션 처리된 Q&A 응답
     */
    public PaginationRes<QnaRes> getChatRoomQnaList(int page, int size, UUID chatRoomId, User user) {
        return PaginationRes.toRes(chatbotTalkQnaRepository.findQnaByConditions(page, size, chatRoomId.toString(), user));
    }

    /**
     * 주어진 채팅룸 ID와 사용자가 입력한 질문을 기반으로 Q&A를 생성합니다.
     *
     * @param chatRoomId 채팅룸 ID
     * @param talkReq    사용자가 입력한 질문 객체
     * @param user       요청한 사용자 객체
     * @return 생성된 Q&A 응답 객체
     */
    @Transactional
    public QnaRes postChatRoomQna(UUID chatRoomId, QnaReq.Insert talkReq, User user) {

        ChatbotTalk talk = chatbotTalkRepository
            .findByChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .map(existingTalk -> {
                validateTalkOwnership(existingTalk, user, chatRoomId.toString());
                return existingTalk;
            })
            .orElseGet(() -> chatbotTalkRepository.save(ChatbotTalk.of(chatRoomId.toString(), talkReq, user)));

        ChatbotTalkQna qna = chatbotTalkQnaRepository.save(ChatbotTalkQna.of(talkReq, talk));

        // 모델 에이전트 질의
        Optional<ModelAgentRes> modelAgentResOpt = executeModelAgent(qna);

        return modelAgentResOpt
            .map(modelAgentRes -> QnaRes.of(qna, modelAgentRes))
            .orElse(QnaRes.of(qna));
    }

    /**
     * 주어진 ID와 Q&A 번호를 사용하여 Q&A의 평가 정보를 업데이트합니다.
     *
     * @param chatRoomId       채팅룸 ID
     * @param qnaNo            Q&A 번호
     * @param qnaAssessmentReq 업데이트할 평가 요청 객체
     * @return 업데이트된 Q&A 응답 객체
     */
    @Transactional
    public QnaRes patchQnaAssessment(UUID chatRoomId, Integer qnaNo, QnaAssessmentReq qnaAssessmentReq) {

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, chatRoomId.toString())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [qna]. chatRoomId: %s, qnaNo: %s".formatted(chatRoomId.toString(), qnaNo)));

        qna.update(qnaAssessmentReq);

        return QnaRes.of(qna);
    }

    /**
     * 주어진 ID와 Q&A 번호를 사용하여 Q&A 재질의를 요청합니다.
     *
     * @param chatRoomId 채팅룸 ID
     * @param qnaNo      Q&A 번호
     * @param user       요청한 사용자 객체
     * @return 재질의된 Q&A 응답 객체
     */
    @Transactional
    public QnaRes postQnaRetry(UUID chatRoomId, Integer qnaNo, User user) {

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, chatRoomId.toString())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [qna]. chatRoomId: %s, qnaNo: %s".formatted(chatRoomId, qnaNo)));

        validateTalkOwnership(qna.getChatbotTalk(), user, chatRoomId.toString());

        // 모델 에이전트 질의
        Optional<ModelAgentRes> modelAgentResOpt = executeModelAgent(qna);

        return modelAgentResOpt
            .map(modelAgentRes -> QnaRes.of(qna, modelAgentRes))
            .orElse(QnaRes.of(qna));
    }

    /**
     * 주어진 채팅룸와 사용자 정보를 기반으로 대화 소유권을 검증합니다.
     *
     * @param talk                검증할 채팅룸 객체
     * @param currentUser         현재 사용자 객체
     * @param requestedChatRoomId 요청된 채팅룸 ID
     */
    private void validateTalkOwnership(ChatbotTalk talk, User currentUser, String requestedChatRoomId) {
        if (!Objects.equals(currentUser.getUserNo(), talk.getUser().getUserNo())) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "You can only control talks(chatRooms) you started. (chatRoomId: %s)".formatted(requestedChatRoomId));
        }
    }

    /**
     * Model-Agent API를 호출하여 질의 결과를 가져옵니다.
     *
     * @param qna qna 객체
     * @return
     */
    private Optional<ModelAgentRes> executeModelAgent(ChatbotTalkQna qna) {
        try {
//            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(qna.getUserQuestionCntt());

            String response = """
                {
                    "output": {
                        "messages": [
                            {
                                "content": {
                                    "text": "# 안녕하세요, Mari 에이전트입니다!\\n\\n오늘의 주요 정보를 요약해 드립니다.\\n\\n## 🌟 주요 업데이트\\n\\n- **새로운 기능**: '주요 질의문' 기능이 추가되었습니다. 이제 분과별로 자주 묻는 질문을 빠르게 확인할 수 있습니다.\\n- **성능 개선**: 채팅 응답 속도가 평균 15% 향상되었습니다.\\n\\n## 📊 최근 데이터 동향 (예시)\\n\\n아래 차트는 최근 2주간의 가상 지표 변화를 보여줍니다. 실제 데이터는 API를 통해 확인해주세요.\\n\\n*   항목 A: 꾸준한 상승세\\n*   항목 B: 변동성 증가\\n\\n더 자세한 내용은 [공식 문서](https://www.example.com/docs)를 참조하시거나, `도움말`이라고 입력하여 문의해주세요.\\n\\n```python\\n# 이것은 예시 코드 블록입니다.\\ndef greet():\\n    print(\\"Hello, Mari!\\")\\n```\\n\\n감사합니다!",
                                    "charts":[
                                        {"date": "2024-07-24", "price": 152.34},
                                        {"date": "2024-07-25", "price": 155.12},
                                        {"date": "2024-07-26", "price": 153.98},
                                        {"date": "2024-07-27", "price": 157.05},
                                        {"date": "2024-07-28", "price": 156.88},
                                        {"date": "2024-07-29", "price": 160.42},
                                        {"date": "2024-07-30", "price": 159.76},
                                        {"date": "2024-07-31", "price": 162.15},
                                        {"date": "2024-08-01", "price": 161.50},
                                        {"date": "2024-08-02", "price": 163.88},
                                        {"date": "2024-08-03", "price": 165.02},
                                        {"date": "2024-08-04", "price": 164.30},
                                        {"date": "2024-08-05", "price": 166.77},
                                        {"date": "2024-08-06", "price": 168.10}
                                    ]
                                },
                                "additional_kwargs": {
                                    "refusal": null
                                },
                                "response_metadata": {
                                    "token_usage": {
                                        "completion_tokens": 81,
                                        "prompt_tokens": 48,
                                        "total_tokens": 129,
                                        "completion_tokens_details": {
                                            "accepted_prediction_tokens": 0,
                                            "audio_tokens": 0,
                                            "reasoning_tokens": 0,
                                            "rejected_prediction_tokens": 0
                                        },
                                        "prompt_tokens_details": {
                                            "audio_tokens": 0,
                                            "cached_tokens": 0
                                        }
                                    },
                                    "model_name": "gpt-4o-2024-08-06",
                                    "system_fingerprint": "fp_ee1d74bde0",
                                    "finish_reason": "stop",
                                    "logprobs": null
                                },
                                "type": "ai",
                                "name": null,
                                "id": "run-14f316b3-4071-4cf3-8a6a-1021deb62985-0",
                                "example": false,
                                "tool_calls": [],
                                "invalid_tool_calls": [],
                                "usage_metadata": {
                                    "input_tokens": 48,
                                    "output_tokens": 81,
                                    "total_tokens": 129,
                                    "input_token_details": {
                                        "audio": 0,
                                        "cache_read": 0
                                    },
                                    "output_token_details": {
                                        "audio": 0,
                                        "reasoning": 0
                                    }
                                }
                            }
                        ]
                    },
                    "metadata": {
                        "run_id": "f82e37a3-5ce7-46c2-b436-894ed7803848",
                        "feedback_tokens": []
                    }
                }
                """;

            ModelAgentRes modelAgentRes = JsonUtil.fromJson(response, ModelAgentRes.class);

            qna.updateCompleteResult(modelAgentRes);
            return Optional.of(modelAgentRes);
        } catch (Exception e) {
            qna.updateFailResult();
            return Optional.empty();
        }
    }

}
