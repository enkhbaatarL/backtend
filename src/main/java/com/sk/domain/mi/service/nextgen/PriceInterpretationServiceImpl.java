package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationEditReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationGenerateRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationGenerationLockStore;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.shared.client.PriceInterpretationAgentApi;
import com.sk.domain.shared.client.dto.request.PriceInterpretationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * 시나리오별 가격전망 해석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceInterpretationServiceImpl implements PriceInterpretationService {

    private static final String ACTION_GENERATE = "generatePriceInterpretation";

    private final PriceInterpretationStore store;
    private final PriceInterpretationGenerationLockStore generationLockStore;
    private final PriceInterpretationAgentApi priceInterpretationAgentApi;
    private final AuthorizationService authorizationService;

    /**
     * [시나리오 가격전망 해석 조회]
     */
    @Override
    public PriceInterpretationSectionRes getInterpretations(Long scenarioGroupId) {
        return store.readPriceInterpretations(scenarioGroupId);
    }

    /**
     * [시나리오 가격전망 해석 생성]
     * 대상 시나리오를 generating으로 생성한 뒤 Agent에 해석 산출을 요청한다.
     * Agent 접수 실패 시 대상 시나리오를 failed로 되돌려(재시도 가능) 버튼이 다시 활성화되도록 한다.
     */
    @Override
    public PriceInterpretationGenerateRes generateInterpretation(Long scenarioGroupId) {

        Long userId = authorizationService.getUserNo().longValue();

        String lockToken = generationLockStore.tryAcquire(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.BIZ_PRICE_INTERP_OCCUPIED, "Price interpretation generation is already in progress. (scenarioGroupId=%d)".formatted(scenarioGroupId)));

        try {

            List<Long> targets = store.beginGeneration(scenarioGroupId);
            // API 호출
            dispatchToAgent(scenarioGroupId, userId, targets);
            return PriceInterpretationGenerateRes.of(scenarioGroupId, targets);

        } finally {
            // lock 해제
            generationLockStore.release(scenarioGroupId, lockToken);
        }

    }

    /**
     * Agent 호출.
     * 이 시점엔 대상 시나리오의 GENERATING이 이미 커밋된 상태 -> 접수가 확정적으로 실패하면 markFailed로 되돌림
     */
    private void dispatchToAgent(Long scenarioGroupId, Long userId, List<Long> targets) {

        try {
            ScenarioDesignAgentRes res = callAgent(() -> priceInterpretationAgentApi.generate(scenarioGroupId, PriceInterpretationAgentReq.of(scenarioGroupId, userId, targets)), ACTION_GENERATE);

            if (!res.isSuccess()) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "agent response: %s".formatted(JsonUtil.toJson(res.data())));
            }

        } catch (RuntimeException e) {
            store.markFailed(targets, e.getMessage()); // 접수 실패 -> failed 업데이트(재시도 가능하도록)
            throw e;
        }

    }

    @Override
    public PriceInterpretationRes editInterpretation(Long scenarioGroupId, Long scenarioId, PriceInterpretationEditReq request) {
        return PriceInterpretationRes.of(store.edit(scenarioGroupId, scenarioId, request.content()));
    }

    @Override
    public PriceInterpretationRes resetInterpretation(Long scenarioGroupId, Long scenarioId) {
        return PriceInterpretationRes.of(store.reset(scenarioGroupId, scenarioId));
    }

    /**
     * Agent 호출을 감싸 통신 예외(호스트 다운/타임아웃/역직렬화 실패)를 통제된 외부 API 에러(500)로 변환한다.
     * HTTP 오류 status는 잡지 않는다 — 공통 RestClient 빌더가 이미 ApiErrorException(status + body 원문)으로
     * 던지고 전역 핸들러가 500으로 응답하므로, 여기서 잡아도 변환할 것이 없다 (원문 로그도 빌더가 남긴다).
     */
    private <T> T callAgent(Supplier<T> call, String action) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[PriceInterp Agent] {} 통신 오류: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(code, e);
        } catch (RestClientException e) {
            log.error("[PriceInterp Agent] {} 호출 실패: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        }
    }

}
