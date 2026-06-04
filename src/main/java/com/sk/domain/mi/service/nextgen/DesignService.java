package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.DesignEventListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenReq;
import com.sk.domain.mi.dto.response.nextgen.DesignEventListRes;
import com.sk.domain.mi.dto.response.nextgen.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenStatusRes;
import java.util.List;

public interface DesignService {

    DesignEventListRes getDesignEvents(DesignEventListReq request);

    ScenarioGenAcceptRes generateScenarios(ScenarioGenReq request);

    void handleCallback(ScenarioGenCallbackReq request);

    List<GeneratedScenarioRes> getGeneratedScenarios(Long requestId);

    List<ScenarioGenStatusRes> getGenerateStatuses(List<Long> requestIds);

    void reset();

}
