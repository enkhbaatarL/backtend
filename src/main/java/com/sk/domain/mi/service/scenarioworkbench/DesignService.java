package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.domain.mi.dto.request.scenarioworkbench.DesignEventListReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenStatusRes;
import java.util.List;

public interface DesignService {

    DesignEventListRes getDesignEvents(DesignEventListReq request);

    ScenarioGenAcceptRes generateScenarios(ScenarioGenReq request);

    void handleCallback(ScenarioGenCallbackReq request);

    List<GeneratedScenarioRes> getGeneratedScenarios(Long requestId);

    List<ScenarioGenStatusRes> getGenerateStatuses(List<Long> requestIds);

    void reset();

}
