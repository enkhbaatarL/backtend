package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.domain.mi.dto.request.scenarioworkbench.OverviewEventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewScenarioListRes;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import java.util.List;

public interface OverviewService {

    OverviewEventListRes getOverviewEvents(OverviewEventListReq request);

    OverviewScenarioListRes getOverviewScenarios(OverviewEventListReq request);

    OverviewPriceForecastRes getOverviewPriceForecasts(IndexName indexName, Long eventId, List<Long> scenarioIds);

}
