package com.sk.domain.mi.service.nextgen.sse.registry;

import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSseEvent;
import org.springframework.stereotype.Component;

/**
 * 가격전망 해석 SSE 연결 레지스트리
 */
@Component
public class PriceInterpretationSseRegistry extends AbstractSseRegistry<PriceInterpretationSseEvent> {

    @Override
    protected String eventName(PriceInterpretationSseEvent event) {
        return event.type().getCode();
    }

    @Override
    protected String logTag() {
        return "[PriceInterpretation SSE]";
    }

}
