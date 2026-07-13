package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.LatestModelExecutionInputFilesRes;
import com.sk.domain.mi.dto.response.nextgen.LatestModelParamProposalParametersRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ScenarioModelExecutionService {

    ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId);

    ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files);

    ModelExecutionAcceptRes submitModelExecution(Long scenarioId, ModelParamProposalExecutionReq request);

    ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String runId);

    LatestModelParamProposalParametersRes getLatestConfirmedParameters(Long scenarioId);

    LatestModelParamProposalParametersRes getLatestProposedParameters(Long scenarioId);

    LatestModelExecutionInputFilesRes getLatestInputFiles(Long scenarioId);
}
