package com.sk.domain.mi.dto.request.nextgen.priceinterp;

import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import java.util.List;

/**
 * [Agent 콜백] 가격전망 해석 생성 완료 본문 (Agent → B.E).
 */
public record PriceInterpretationGenerationCallbackReq(

    AgentCallbackResult result,
    String reason,
    List<Item> interpretations

) {

    public boolean isSuccess() {
        return result != null && result.isSuccess();
    }

    public record Item(
        Long scenarioId,
        String content
    ) {}

}
