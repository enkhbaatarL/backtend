package com.sk.domain.mi.service.scenarioworkbench;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.scenarioworkbench.OverviewEventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewScenarioListRes;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OverviewServiceImpl implements OverviewService {

    @Override
    public OverviewEventListRes getOverviewEvents(OverviewEventListReq request) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Overview events DB 연동 미구현");
    }

    @Override
    public OverviewScenarioListRes getOverviewScenarios(OverviewEventListReq request) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Overview scenarios DB 연동 미구현");
    }

    @Override
    public OverviewPriceForecastRes getOverviewPriceForecasts(IndexName indexName, Long eventId, List<Long> scenarioIds) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Overview price forecasts DB 연동 미구현");
    }

}
