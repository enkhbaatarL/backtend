package com.sk.domain.mi.service.nextgen.sse.registry;

import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalSseEvent;
import org.springframework.stereotype.Component;

/**
 * 시나리오 파라미터 비교 팝업용 그룹 단위 스트림.
 * 시나리오 단위 스트림과 같은 이벤트명을 쓰므로 FE는 한 규칙으로 처리하고, payload의 scenarioId로 행을 갱신한다.
 */
@Component
public class GroupModelParamProposalSseRegistry extends AbstractSseRegistry<ModelParamProposalSseEvent> {

    @Override
    protected String eventName(ModelParamProposalSseEvent event) {
        return "model-parameter-proposal";
    }

    @Override
    protected String logTag() {
        return "[GroupModelParamProposal SSE]";
    }
}
