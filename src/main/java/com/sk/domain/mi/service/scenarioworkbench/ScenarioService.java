package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioListRes;

public interface ScenarioService {

    ScenarioListRes getScenarios(ScenarioListReq request);

    ScenarioDetailRes getScenarioDetail(Long scenarioId);

}
