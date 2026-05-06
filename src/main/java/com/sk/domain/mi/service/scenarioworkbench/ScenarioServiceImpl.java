package com.sk.domain.mi.service.scenarioworkbench;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioListRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioServiceImpl implements ScenarioService {

    @Override
    public ScenarioListRes getScenarios(ScenarioListReq request) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario list DB 연동 미구현");
    }

    @Override
    public ScenarioDetailRes getScenarioDetail(Long scenarioId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario detail DB 연동 미구현");
    }

}
