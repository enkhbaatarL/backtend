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
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tcm.ChatbotMajorQuestionRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
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

    private final AuthorizationService authorizationService;

    private final ModelAgentApi modelAgentApi;

    private final ChatbotTalkRepository chatbotTalkRepository;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final ChatbotMajorQuestionRepository chatbotMajorQuestionRepository;

    /**
     * 주어진 분과 코드로 필터링된 주요 질의문 목록을 조회합니다.
     *
     * @return 주요 질의문 목록
     */
    public List<MajorQuestionRes> getMajorQuestions() {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();
        CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));

        List<ChatbotMajorQuestion> majorQuestions = chatbotMajorQuestionRepository.findByMarketIndexAnalysisTargetCdAndIsDeletedFalseOrderByMajorQuestionRnk(company.getMarketIndexAnalysisTargetCd());
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

        return QnaRes.ofCompact(qna);
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
                          "text": "## 📈 2024년 5월 주요 제품 판매 실적 분석\\n\\n안녕하세요! 2024년 5월 주요 제품들의 판매 실적을 분석한 결과입니다.\\n\\n아래 표는 각 제품별 판매량 및 전월 대비 증감률을 나타냅니다.\\n\\n| 제품명         | 카테고리 | 판매량 (개) | 전월 대비 증감률 (%) | 비고     |\\n|----------------|----------|-------------|--------------------|----------|\\n| 스마트워치 X   | 웨어러블 | 1,250       | +15.2              | 신모델 출시 |\\n| AI 스피커 Mini | 스마트홈 | 880         | +8.5               | 프로모션 진행 |\\n| 무선 이어폰 Z  | 오디오   | 2,130       | -2.1               | 경쟁 심화  |\\n| 4K 모니터 Pro  | 디스플레이| 670         | +22.0              | 수요 증가  |\\n\\n전반적으로 스마트워치 X와 4K 모니터 Pro의 판매 성장이 두드러지며, 무선 이어폰 Z는 경쟁 심화로 소폭 감소했습니다. AI 스피커 Mini는 프로모션 효과로 꾸준한 성장을 보이고 있습니다.\\n\\n다음은 '스마트워치 X'의 일별 판매량 추이 그래프입니다.",
                          "charts": [
                            {
                              "order": 1,
                              "legend": "스마트워치 X 일별 판매량 (5월)",
                              "date": [
                                "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
                                "2024-05-06", "2024-05-07", "2024-05-08", "2024-05-09", "2024-05-10",
                                "2024-05-11", "2024-05-12", "2024-05-13", "2024-05-14", "2024-05-15",
                                "2024-05-16", "2024-05-17", "2024-05-18", "2024-05-19", "2024-05-20",
                                "2024-05-21", "2024-05-22", "2024-05-23", "2024-05-24", "2024-05-25",
                                "2024-05-26", "2024-05-27", "2024-05-28", "2024-05-29", "2024-05-30",
                                "2024-05-31"
                              ],
                              "value": [
                                30.0, 32.0, 35.0, 33.0, 38.0, 40.0, 42.0, 45.0, 43.0, 48.0,
                                50.0, 55.0, 52.0, 49.0, 45.0, 48.0, 53.0, 58.0, 60.0, 62.0,
                                59.0, 55.0, 50.0, 48.0, 52.0, 57.0, 61.0, 65.0, 68.0, 70.0,
                                66.0
                              ]
                            }
                          ]
                        },
                        "additional_kwargs": {
                          "refusal": null
                        },
                        "response_metadata": {
                          "token_usage": {
                            "completion_tokens": 350,
                            "prompt_tokens": 120,
                            "total_tokens": 470,
                            "completion_tokens_details": {
                              "accepted_prediction_tokens": 340,
                              "audio_tokens": 0,
                              "reasoning_tokens": 10,
                              "rejected_prediction_tokens": 0
                            },
                            "prompt_tokens_details": {
                              "audio_tokens": 0,
                              "cached_tokens": 50
                            }
                          },
                          "model_name": "claude-3-opus-20240229",
                          "system_fingerprint": "fp_abc123",
                          "finish_reason": "stop_sequence",
                          "logprobs": null
                        },
                        "type": "ai_response",
                        "name": "ProductAnalysisBot",
                        "id": "msg_01HXZYJ8K9N7M5G4F3E2D1C0B",
                        "example": false,
                        "tool_calls": [],
                        "invalid_tool_calls": [],
                        "usage_metadata": {
                          "input_tokens": 120,
                          "output_tokens": 350,
                          "total_tokens": 470,
                          "input_token_details": {
                            "audio": 0,
                            "cache_read": 50
                          },
                          "output_token_details": {
                            "audio": 0,
                            "reasoning": 10
                          }
                        }
                      },
                      {
                        "content": {
                          "text": "## 📊 웹사이트 트래픽 분석 (2024년 5월)\\n\\n2024년 5월 한 달간 저희 웹사이트의 트래픽 변화를 요약해 드립니다.\\n\\n주요 유입 경로별 세션 수는 다음과 같습니다.\\n\\n| 유입 경로     | 세션 수   | 전월 대비 (%) | 주요 키워드 (상위 2개) |\\n|---------------|-----------|---------------|------------------------|\\n| 직접 유입     | 15,200    | +5.8          | -                      |\\n| 자연 검색(SEO)| 28,500    | +12.3         | '스마트워치 추천', 'AI 스피커 가격' |\\n| 소셜 미디어   | 8,700     | -3.1          | (캠페인 종료)          |\\n| 추천(Referral)| 4,100     | +7.5          | 'IT뉴스사이트', '기술블로그' |\\n\\n자연 검색을 통한 유입이 크게 증가했으며, 소셜 미디어는 특정 캠페인 종료로 인해 감소했습니다. 전반적인 트래픽은 증가 추세입니다.\\n\\n아래는 일별 총 방문자 수 변화를 나타내는 차트입니다.",
                          "charts": [
                            {
                              "order": 1,
                              "legend": "웹사이트 일별 총 방문자 수 (5월)",
                              "date": [
                                "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
                                "2024-05-06", "2024-05-07", "2024-05-08", "2024-05-09", "2024-05-10",
                                "2024-05-11", "2024-05-12", "2024-05-13", "2024-05-14", "2024-05-15",
                                "2024-05-16", "2024-05-17", "2024-05-18", "2024-05-19", "2024-05-20",
                                "2024-05-21", "2024-05-22", "2024-05-23", "2024-05-24", "2024-05-25",
                                "2024-05-26", "2024-05-27", "2024-05-28", "2024-05-29", "2024-05-30",
                                "2024-05-31"
                              ],
                              "value": [
                                1800.0, 1850.0, 1900.0, 1880.0, 1950.0, 2000.0, 2050.0, 2100.0, 2080.0, 2150.0,
                                2200.0, 2250.0, 2220.0, 2180.0, 2100.0, 2150.0, 2230.0, 2300.0, 2350.0, 2380.0,
                                2330.0, 2280.0, 2200.0, 2180.0, 2250.0, 2320.0, 2380.0, 2450.0, 2500.0, 2550.0,
                                2510.0
                              ]
                            }
                          ]
                        },
                        "additional_kwargs": {
                          "refusal": null
                        },
                        "response_metadata": {
                          "token_usage": {
                            "completion_tokens": 400,
                            "prompt_tokens": 100,
                            "total_tokens": 500,
                            "completion_tokens_details": {
                              "accepted_prediction_tokens": 390,
                              "audio_tokens": 0,
                              "reasoning_tokens": 10,
                              "rejected_prediction_tokens": 0
                            },
                            "prompt_tokens_details": {
                              "audio_tokens": 0,
                              "cached_tokens": 30
                            }
                          },
                          "model_name": "claude-3-sonnet-20240307",
                          "system_fingerprint": "fp_def456",
                          "finish_reason": "max_tokens",
                          "logprobs": null
                        },
                        "type": "ai_response",
                        "name": "TrafficAnalysisBot",
                        "id": "msg_01HXZYK2R7S6P5H4G3F2E1D0C",
                        "example": false,
                        "tool_calls": [],
                        "invalid_tool_calls": [],
                        "usage_metadata": {
                          "input_tokens": 100,
                          "output_tokens": 400,
                          "total_tokens": 500,
                          "input_token_details": {
                            "audio": 0,
                            "cache_read": 30
                          },
                          "output_token_details": {
                            "audio": 0,
                            "reasoning": 10
                          }
                        }
                      }
                    ]
                  },
                  "metadata": {
                    "run_id": "run_xyz789abc123",
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
