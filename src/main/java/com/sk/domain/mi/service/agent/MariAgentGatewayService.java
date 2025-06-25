package com.sk.domain.mi.service.agent;

import com.sk.domain.admin.service.TokenQuotaGuard;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.shared.client.ModelAgentApi;
import com.sk.domain.shared.client.dto.request.ModelAgentHeaders;
import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
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
public class MariAgentGatewayService {

    @Value("${api.model-agent.app-id}")
    private String appId;

    private final AuthorizationService authorizationService;

    private final ModelAgentApi modelAgentApi;
    private final TokenQuotaGuard tokenQuotaGuard;

    private final CustomerCompanyRepository customerCompanyRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    /**
     * Model-Agent API를 호출하여 질의 결과를 가져옵니다.
     *
     * @return Optional<AnswerRes>
     */
    @Transactional
    protected Optional<AnswerRes> executeModelAgent(QnaReq.Insert talkReq, ChatbotTalkQna qna, List<ChatbotTalkQna> histories) {

        try {

            Integer companyNo = authorizationService.getCompanyNoWithPriority();
            Integer userNo = authorizationService.getUserNo();

//            CustomerCompany company = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
//                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo)));

            Map<String, Object> headers = ModelAgentHeaders.of(companyNo, userNo, qna.getChatbotTalk().getChatbotTalkUuid(), qna.getChatbotTalkQnaNo()).toMap();
            ModelAgentReq body = ModelAgentReq.of(talkReq, histories, companyNo, authorizationService.getUserNo());

            ModelAgentRes modelAgentRes = modelAgentApi.postMessage(appId, headers, body);

            // 테스트용 더미 데이터
//            ModelAgentRes modelAgentRes = JsonUtil.fromJson(DummyResponses.random(), ModelAgentRes.class);

            List<Related> related = prepareAnswerDataResources(modelAgentRes);

            AnswerRes answerRes = AnswerRes.of(modelAgentRes, related);
            qna.updateCompleteResult(modelAgentRes, answerRes);

            TokenQuotaGuard.QuotaResult quotaResult = tokenQuotaGuard.addAndCheck(authorizationService.getCompanyNoWithPriority(), qna.getInputTokenCnt() + qna.getOutputTokenCnt());
            log.info("[Model-Agent API call token usage] company: {}, used: {}, total: {}, next-request-allowed: {}", authorizationService.getCompanyNoWithPriority(), quotaResult.delta(), quotaResult.usage(), quotaResult.allowed());

            return Optional.ofNullable(answerRes);

        } catch (ResourceAccessException e) {

            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);

            if (root instanceof SocketTimeoutException err) {
                log.error("[Model-Agent API call read-timeout] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), err.getMessage(), err);
                qna.updateFailResult(AnswerStatusCd.FAIL_TIMED_OUT);
            } else {
                log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
                qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("[Model-Agent API call failed] for qnaNo: {}, chatRoomId: {}. Error: {}", qna.getChatbotTalkQnaNo(), qna.getChatbotTalk().getChatbotTalkUuid(), e.getMessage(), e);
            qna.updateFailResult(AnswerStatusCd.FAIL_AGENT_COMMUNICATION);
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
