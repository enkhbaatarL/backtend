package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.request.agent.ChatRoomReq;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.ChatRoomRes;
import com.sk.domain.mi.dto.response.agent.MajorQuestionRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.ChatbotMajorQuestion;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import com.sk.domain.shared.repository.master.tcm.ChatbotMajorQuestionRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkQnaRepository;
import com.sk.domain.shared.repository.master.tcm.ChatbotTalkRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import com.sk.domain.test.DummyResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MariAgentService {

    @Value("${features.validation.model-agent.write-length:-1}")
    private int writeLimit;

    private final AuthorizationService authorizationService;

    private final ModelAgentApi modelAgentApi;
    private final TokenQuotaGuard tokenQuotaGuard;

    private final ProhibitedWordService prohibitedWordService;

    private final ChatbotTalkRepository chatbotTalkRepository;
    private final ChatbotTalkQnaRepository chatbotTalkQnaRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final ChatbotMajorQuestionRepository chatbotMajorQuestionRepository;

    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    /**
     * 주어진 분과 코드로 필터링된 주요 질의문 목록을 조회합니다.
     *
     * @return 주요 질의문 목록
     */
    public List<MajorQuestionRes> getMajorQuestions() {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();
        CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));

        List<ChatbotMajorQuestion> majorQuestions = chatbotMajorQuestionRepository
            .findByMarketIndexAnalysisTargetCdAndIsDeletedFalseOrderByMajorQuestionRnk(company.getMarketIndexAnalysisTargetCd());
        return MajorQuestionRes.ofList(majorQuestions);
    }

    /**
     * 특정 사용자의 채팅룸 목록을 최신순으로 조회합니다.
     *
     * @param user 사용자 객체
     * @return 채팅룸 목록
     */
    public List<ChatRoomRes> getChatRooms(User user) {
        List<ChatbotTalk> chatbotTalks = chatbotTalkRepository.findByUserAndIsDeletedFalseOrderByCreateDtDesc(user);
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

        ChatbotTalk talk = findAndValidateTalkOwnership(chatRoomId, user);

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

        ChatbotTalk talk = findAndValidateTalkOwnership(chatRoomId, user);

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

        AnswerStatusCd validationStatus = validateQuestion(talkReq.question());

        if (validationStatus != null) {
            ChatbotTalkQna qna = chatbotTalkQnaRepository.save(ChatbotTalkQna.ofFail(talkReq, talk, validationStatus));
            return QnaRes.of(qna);
        }

        ChatbotTalkQna qna = chatbotTalkQnaRepository.save(ChatbotTalkQna.of(talkReq, talk));

        // 히스토리 조회
        List<ChatbotTalkQna> histories = chatbotTalkQnaRepository.findByChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .stream()
            .sorted(Comparator.comparing(ChatbotTalkQna::getChatbotTalkQnaNo))
            .toList();

        return executeModelAgent(talkReq, qna, histories)
            .map(answer -> QnaRes.of(qna, answer))
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

        ChatbotTalkQna qna = chatbotTalkQnaRepository
            .findByChatbotTalkQnaNoAndChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(qnaNo, chatRoomId.toString())
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

        ChatbotTalk talk = findAndValidateTalkOwnership(chatRoomId, user);

        ChatbotTalkQna qna = chatbotTalkQnaRepository.findByChatbotTalkQnaNoAndChatbotTalkAndIsDeletedFalse(qnaNo, talk)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [qna]. chatRoomId: %s, qnaNo: %s".formatted(chatRoomId, qnaNo)));

        AnswerStatusCd validationStatus = validateQuestion(qna.getUserQuestionCntt());

        if (validationStatus != null) {
            qna.updateFailResult(validationStatus);
            return QnaRes.of(qna);
        }

        List<ChatbotTalkQna> qnaList = chatbotTalkQnaRepository.findByChatbotTalk_ChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString());

        // 현재 qna 이후의 대화는 모두 삭제
        qnaList.stream()
            .filter(entity -> entity.getChatbotTalkQnaNo() > qnaNo)
            .forEach(BaseEntity::softDelete);

        // 히스토리 조회
        List<ChatbotTalkQna> histories = qnaList.stream()
            .filter(entity -> entity.getChatbotTalkQnaNo() < qnaNo)
            .sorted(Comparator.comparing(ChatbotTalkQna::getChatbotTalkQnaNo))
            .toList();

        return executeModelAgent(QnaReq.Insert.ofDefault(qna.getUserQuestionCntt()), qna, histories)
            .map(answer -> QnaRes.of(qna, answer))
            .orElse(QnaRes.of(qna));
    }

    /**
     * Model-Agent API를 호출하여 질의 결과를 가져옵니다.
     *
     * @return Optional<AnswerRes>
     */
    private Optional<AnswerRes> executeModelAgent(QnaReq.Insert talkReq, ChatbotTalkQna qna, List<ChatbotTalkQna> histories) {

        try {

            Integer companyNo = authorizationService.getCompanyNoWithPriority();
            CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));

            ModelAgentReq req = ModelAgentReq.of(talkReq, histories, company.getCustomerCompanyCd(), authorizationService.getUserNo());
             ModelAgentRes modelAgentRes = modelAgentApi.postMessage(req);

             // 테스트용 더미 데이터
//            ModelAgentRes modelAgentRes = JsonUtil.fromJson(DummyResponses.random(), ModelAgentRes.class);

            List<Related> related = prepareAnswerDataResources(modelAgentRes);

            AnswerRes answerRes = AnswerRes.of(modelAgentRes, related);
            qna.updateCompleteResult(modelAgentRes, answerRes);

            TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(authorizationService.getCompanyNoWithPriority(), qna.getInputTokenCnt() + qna.getOutputTokenCnt());
            log.info("[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}", authorizationService.getCompanyNoWithPriority(), quotaResult.delta(), quotaResult.usage(), quotaResult.allowed());

            return Optional.ofNullable(answerRes);

        } catch (ResourceAccessException e) {
            log.error("[Model-Agent API call timeout] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
            qna.updateFailResult(AnswerStatusCd.FAIL_TIMED_OUT);
            return Optional.empty();

        } catch (Exception e) {
            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
            qna.updateFailResult();
            return Optional.empty();
        }

    }

    private List<Related> prepareAnswerDataResources(ModelAgentRes modelAgentRes) {

        List<Related> related = new ArrayList<>();

        // 1. 정형데이터 연관데이터 추출
        List<Related> structured = prepareStructuredDateResources(modelAgentRes);
        if (structured != null && !structured.isEmpty()) {
            related.addAll(structured);
        }

        // 2. 비정형데이터 연관데이터 추출
        List<Related> unstructured = prepareUnstructuredDateResources(modelAgentRes);
        if (unstructured != null && !unstructured.isEmpty()) {
            related.addAll(unstructured);
        }

        return related;
    }

    private List<Related> prepareStructuredDateResources(ModelAgentRes modelAgentRes) {

        List<Integer> gdsNos = extractRelatedGdsNos(modelAgentRes);

        Map<Integer, GoodsDataSourceDetail> gdsMap = goodsDataSourceDetailRepository.findByGoodsDataSourceNoInAndIsDeletedFalse(gdsNos)
            .stream()
            .collect(Collectors.toMap(GoodsDataSourceDetail::getGoodsDataSourceNo, Function.identity()));

        return Related.ofStructuredList(gdsNos, gdsMap);
    }

    private List<Related> prepareUnstructuredDateResources(ModelAgentRes modelAgentRes) {

        List<String> distinguishIds = extractDistinguishIds(modelAgentRes);

        List<GoodsData> goodsDataList = goodsDataRepository.findByGoodsDataDistinguishIdInAndIsDeletedFalse(distinguishIds);
        List<Integer> goodsDataNos = goodsDataList.stream().map(GoodsData::getGoodsDataNo).toList();

        Map<Integer, UploadedFile> fileMap = uploadedFileRepository.findByFileRelationshipObjectDistinguishNoInAndIsDeletedFalse(goodsDataNos)
            .stream()
            .collect(Collectors.toMap(UploadedFile::getFileRelationshipObjectDistinguishNo, Function.identity(), (v1, v2) -> v2));

        return Related.ofUnstructuredList(goodsDataList, fileMap);
    }

    /**
     * 채팅방 ID로 ChatbotTalk 엔티티를 조회하고, 현재 사용자가 소유주인지 검증합니다.
     * 엔티티가 없거나 소유주가 아니면 적절한 예외를 발생시킵니다.
     *
     * @param chatRoomId 검증할 채팅방의 UUID
     * @param user       현재 요청 사용자
     * @return 검증이 완료된 ChatbotTalk 엔티티
     * @throws AppErrorException 엔티티를 찾지 못하거나 권한이 없는 경우
     */
    private ChatbotTalk findAndValidateTalkOwnership(UUID chatRoomId, User user) {

        // 1. ID로 엔티티 조회
        ChatbotTalk talk = chatbotTalkRepository.findByChatbotTalkUuidAndIsDeletedFalse(chatRoomId.toString())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [talk(chat-room)]. chatRoomId: %s".formatted(chatRoomId)));

        // 2. 소유권 검증
        validateTalkOwnership(talk, user, chatRoomId.toString());

        return talk;
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
     * 질문에 대한 밸리데이션 체크 (길이, 금칙어)
     * 실패 사유가 있을 경우 AnswerStatusCd 반환, 아니면 null
     */
    private AnswerStatusCd validateQuestion(String question) {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();

        if (writeLimit > 0 && question.length() > writeLimit) {
            return AnswerStatusCd.FAIL_EXCEED_LENGTH;
        }
        if (prohibitedWordService.containsProhibitedWord(question)) {
            return AnswerStatusCd.FAIL_FORBIDDEN_WORDS;
        }
        if (!tokenQuotaGuard.hasCapacity(companyNo)) {
            return AnswerStatusCd.FAIL_EXCEED_TOKEN;
        }

        return null;
    }

    /**
     * 정형데이터 식별자 추출
     */
    private List<Integer> extractRelatedGdsNos(ModelAgentRes modelAgentRes) {
        return Optional.ofNullable(modelAgentRes)
            .map(s -> s.output().messages())
            .orElse(Collections.emptyList()) // null 체크 강화
            .stream()
            .map(ModelAgentRes.Output.Message::content)
            .filter(Objects::nonNull)
            .map(ModelAgentRes.Output.Message.Content::relatedData)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();
    }

    /**
     * 상품 데이터 식별번호 추출
     */
    private List<String> extractDistinguishIds(ModelAgentRes modelAgentRes) {
        return Optional.ofNullable(modelAgentRes)
            .map(s -> s.output().messages())
            .orElse(Collections.emptyList()) // null 체크 강화
            .stream()
            .map(ModelAgentRes.Output.Message::content)
            .filter(Objects::nonNull)
            .map(ModelAgentRes.Output.Message.Content::vectorSources)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(ModelAgentRes.Output.Message.Content.VectorSource::gdsDataDstgId)
            .toList();
    }

}
