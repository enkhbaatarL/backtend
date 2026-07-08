package com.sk.domain.mi.service.nextgen.priceinterp;

import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationGenerationCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSseEvent;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSseEvent.Type;
import com.sk.domain.mi.service.nextgen.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 가격전망 해석 Agent 콜백 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceInterpretationCallbackService {

    private final PriceInterpretationStore store;
    private final SseEventPublisher eventPublisher;

    /**
     * 생성 완료 콜백 처리. 성공/실패 모두 DB에 반영한 뒤 신호를 푸시한다.
     * (성공: generating 건 generated 적재 / 실패: 대상 generating 건 failed 업데이트)
     */
    public void handleGeneration(Long scenarioGroupId, PriceInterpretationGenerationCallbackReq request) {

        // 커밋 완료
        store.applyGenerationCallback(scenarioGroupId, request);

        if (request.isSuccess()) {
            eventPublisher.publish(PriceInterpretationSseEvent.of(scenarioGroupId, Type.GENERATION));
        } else {
            eventPublisher.publish(PriceInterpretationSseEvent.failure(scenarioGroupId, Type.GENERATION, request.reason()));
        }
    }

}
