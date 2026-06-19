package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ScenarioModelExecutionService {

    ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId);

    ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files);

    ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String runId);
}
