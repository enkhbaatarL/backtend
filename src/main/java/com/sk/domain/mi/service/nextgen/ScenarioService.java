package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ScenarioListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioListRes;

public interface ScenarioService {

    ScenarioListRes getScenarios(ScenarioListReq request);

    ScenarioDetailRes getScenarioDetail(Long scenarioId);

}
