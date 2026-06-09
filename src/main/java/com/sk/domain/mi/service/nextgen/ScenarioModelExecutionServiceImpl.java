package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.ModelExecutionCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioModelExecutionServiceImpl implements ScenarioModelExecutionService {

    @Override
    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution parameters 미구현");
    }

    @Override
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution submit 미구현");
    }

    @Override
    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String executionId) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution status 미구현");
    }

    @Override
    public ModelExecutionStatusRes updateModelExecutionFromCallback(
        Long scenarioId,
        String executionId,
        ModelExecutionCallbackReq request
    ) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution callback 미구현");
    }
}
