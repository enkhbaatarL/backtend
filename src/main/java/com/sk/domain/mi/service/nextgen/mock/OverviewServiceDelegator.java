package com.sk.domain.mi.service.nextgen.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.OverviewEventListReq;
import com.sk.domain.mi.dto.response.nextgen.OverviewEventListRes;
import com.sk.domain.mi.dto.response.nextgen.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.nextgen.OverviewScenarioListRes;
import com.sk.domain.mi.service.nextgen.OverviewService;
import com.sk.domain.mi.service.nextgen.OverviewServiceImpl;
import com.sk.domain.shared.enums.nextgen.IndexName;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class OverviewServiceDelegator implements OverviewService {

    private final OverviewServiceImpl realService;
    private final OverviewMockService mockService;
    private final NextGenMockProperties mockProps;

    @Override
    public OverviewEventListRes getOverviewEvents(OverviewEventListReq request) {
        return mockProps.getOverview().getApi().isGetEvents()
            ? mockService.getOverviewEvents(request)
            : realService.getOverviewEvents(request);
    }

    @Override
    public OverviewScenarioListRes getOverviewScenarios(OverviewEventListReq request) {
        return mockProps.getOverview().getApi().isGetScenarios()
            ? mockService.getOverviewScenarios(request)
            : realService.getOverviewScenarios(request);
    }

    @Override
    public OverviewPriceForecastRes getOverviewPriceForecasts(IndexName indexName, Long eventId, List<Long> scenarioIds) {
        return mockProps.getOverview().getApi().isGetPriceForecasts()
            ? mockService.getOverviewPriceForecasts(indexName, eventId, scenarioIds)
            : realService.getOverviewPriceForecasts(indexName, eventId, scenarioIds);
    }

}
