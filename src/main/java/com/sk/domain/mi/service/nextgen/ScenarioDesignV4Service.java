package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartV4Req;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStatusRes;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 멀티 이벤트 시나리오 서비스.
 */
public interface ScenarioDesignV4Service {

    ScenarioDesignStatusRes getDesignStatus(List<Long> miEventIds);

    SseEmitter startDesignStream(ScenarioDesignStartV4Req request);

    ScenarioDesignPresentationRes getPresentation(Long scenarioGroupId, String operationId);

}
