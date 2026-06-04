package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.OverviewEventListReq;
import com.sk.domain.mi.dto.response.nextgen.OverviewEventListRes;
import com.sk.domain.mi.dto.response.nextgen.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.nextgen.OverviewScenarioListRes;
import com.sk.domain.shared.enums.nextgen.IndexName;
import java.util.List;

public interface OverviewService {

    OverviewEventListRes getOverviewEvents(OverviewEventListReq request);

    OverviewScenarioListRes getOverviewScenarios(OverviewEventListReq request);

    OverviewPriceForecastRes getOverviewPriceForecasts(IndexName indexName, Long eventId, List<Long> scenarioIds);

}
