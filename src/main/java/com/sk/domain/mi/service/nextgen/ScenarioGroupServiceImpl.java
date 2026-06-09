package com.sk.domain.mi.service.nextgen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBulkStatusRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import java.io.ByteArrayInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioGroupServiceImpl implements ScenarioGroupService {

    @Override
    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group list DB 연동 미구현");
    }

    @Override
    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group scenarios DB 연동 미구현");
    }

    @Override
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group graph DB 연동 미구현");
    }

    @Override
    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group graph export DB 연동 미구현");
    }

    @Override
    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group detail DB 연동 미구현");
    }

    @Override
    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario detail DB 연동 미구현");
    }

    @Override
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioStatus status) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario status 변경 DB 연동 미구현");
    }

    @Override
    public ScenarioBulkStatusRes bulkUpdateScenarioStatus(Long scenarioGroupId, ScenarioStatus status) {
        // TODO: DB 연동 구현
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario status 일괄 변경 DB 연동 미구현");
    }

}
