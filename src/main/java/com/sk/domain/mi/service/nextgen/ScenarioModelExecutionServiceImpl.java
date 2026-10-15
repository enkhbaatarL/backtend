package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.LatestModelParamProposalParametersRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionFileRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.nextgen.execution.ModelParamExecutionCoordinator;
import com.sk.domain.mi.service.nextgen.execution.ModelParamExecutionCoordinator.ExecutionConfirmationContext;
import com.sk.domain.mi.service.nextgen.execution.ScenarioExecutionFileStager;
import com.sk.domain.mi.service.nextgen.execution.ScenarioExecutionRegistrar;
import com.sk.domain.mi.service.nextgen.execution.StagedScenarioExecutionFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterTreeProjector;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.MergedModelInputFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterTreeProjector.InvalidModelParameterTreeException;
import com.sk.domain.mi.service.nextgen.query.LatestModelParamProposalExecutionContextResolver;
import com.sk.domain.mi.service.nextgen.query.LatestModelParamProposalExecutionContextResolver.LatestModelParamProposalExecutionContext;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.enums.nextgen.ModelParamParameterSource;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioModelExecutionServiceImpl implements ScenarioModelExecutionService {

    private static final int REQUIRED_FILE_COUNT = 4;
    private static final List<String> REQUIRED_INPUT_FILE_ROLE_KEYWORDS = List.of(
        "DEMAND",
        "SUPPLY",
        "TRADE",
        "OUTPUT"
    );
    private static final String REQUIRED_INPUT_FILE_ROLE_MESSAGE =
        "files는 파일명에 DEMAND, SUPPLY, TRADE, OUTPUT 키워드를 각각 정확히 1개씩 포함해야 합니다.";
    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";

    private final MiScenarioRepository miScenarioRepository;
    private final MiScenarioGroupRepository miScenarioGroupRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final LatestModelParamProposalExecutionContextResolver latestModelParamProposalExecutionContextResolver;
    private final ModelParamExecutionCoordinator modelParamExecutionCoordinator;
    private final ScenarioExecutionFileStager scenarioExecutionFileStager;
    private final ScenarioExecutionRegistrar scenarioExecutionRegistrar;
    private final TransactionOperations transactionOperations;
    private final ModelParameterTreeProjector modelParameterTreeProjector;

    @Override
    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution parameters 미구현");
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files) {
        precheckMultipartExecution(scenarioId, files);
        StagedScenarioExecutionFiles stagedFiles = scenarioExecutionFileStager.stageSequentially(files);
        try {
            ModelExecutionAcceptRes response = transactionOperations.execute(status -> {
                MiScenario scenario = findScenarioForUpdate(scenarioId);
                rejectDiscardedScenario(scenario);
                validateFiles(files);
                modelParamExecutionCoordinator.rejectIfActiveExecutionExists(scenarioId);
                return scenarioExecutionRegistrar.registerStaged(scenario, stagedFiles, null);
            });
            if (response == null) {
                throw AppErrorException.of(
                    ResultCode.Error.SYS_INTERNAL_ERROR,
                    "multipart model execution transaction returned no response."
                );
            }
            return response;
        } catch (RuntimeException e) {
            scenarioExecutionFileStager.discard(stagedFiles);
            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelExecutionAcceptRes submitModelExecution(
        Long scenarioId,
        ModelParamProposalExecutionReq request
    ) {
        ExecutionConfirmationContext context = beginJsonExecution(scenarioId, request);
        StagedScenarioExecutionFiles stagedFiles = null;
        MergedModelInputFiles mergedFiles = null;
        try {
            mergedFiles = modelParamExecutionCoordinator.confirmAndMerge(scenarioId, request, context);
            List<MultipartFile> files = mergedFiles.toMultipartFiles();
            validateFiles(files);
            stagedFiles = scenarioExecutionFileStager.stageSequentially(files);
            StagedScenarioExecutionFiles filesToAttach = stagedFiles;
            ModelExecutionAcceptRes response = transactionOperations.execute(status -> {
                MiScenario scenario = findScenarioForUpdate(scenarioId);
                rejectDiscardedScenario(scenario);
                modelParamExecutionCoordinator.rejectIfActiveExecutionExists(scenarioId);
                validateFiles(files);
                return scenarioExecutionRegistrar.registerStaged(scenario, filesToAttach, context.execPrepId());
            });
            if (response == null) {
                throw AppErrorException.of(
                    ResultCode.Error.SYS_INTERNAL_ERROR,
                    "JSON model execution transaction returned no response."
                );
            }
            return response;
        } catch (RuntimeException e) {
            scenarioExecutionFileStager.discard(stagedFiles);
            modelParamExecutionCoordinator.markFailed(context.execPrepId(), e);
            throw e;
        } finally {
            // 병합 산출물은 임시 파일 — staging(스토리지 업로드) 이후에는 성공/실패와 무관하게 삭제한다
            if (mergedFiles != null) {
                mergedFiles.cleanup();
            }
        }
    }

    private void precheckMultipartExecution(Long scenarioId, List<MultipartFile> files) {
        transactionOperations.executeWithoutResult(status -> {
            MiScenario scenario = findScenarioForUpdate(scenarioId);
            rejectDiscardedScenario(scenario);
            validateFiles(files);
            modelParamExecutionCoordinator.rejectIfActiveExecutionExists(scenarioId);
        });
    }

    private ExecutionConfirmationContext beginJsonExecution(
        Long scenarioId,
        ModelParamProposalExecutionReq request
    ) {
        ExecutionConfirmationContext context = transactionOperations.execute(status -> {
            MiScenario scenario = findScenarioForUpdate(scenarioId);
            rejectDiscardedScenario(scenario);
            return modelParamExecutionCoordinator.begin(scenario, request);
        });
        if (context == null) {
            throw AppErrorException.of(
                ResultCode.Error.SYS_INTERNAL_ERROR,
                "JSON model execution preparation transaction returned no context."
            );
        }
        return context;
    }

    @Override
    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String runId) {
        ScenarioCaseRunHistory runHistory = scenarioCaseRunHistoryRepository.findByRunIdAndIsDeletedFalse(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "model execution run not found. runId: %s".formatted(runId)
            ));
        ScenarioCase scenarioCase = scenarioCaseRepository.findByCaseIdAndScenarioIdAndExecTypeAndIsDeletedFalse(
                runHistory.getCaseId(),
                scenarioId,
                FundamentalModelExecType.SCENARIO
            )
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "model execution not found. scenarioId: %d, runId: %s".formatted(scenarioId, runId)
            ));

        return new ModelExecutionStatusRes(
            scenarioCase.getCaseId(),
            scenarioId,
            runId,
            toModelExecutionStatus(runHistory.getStatus()),
            findInputFiles(scenarioCase.getCaseId()),
            scenarioCase.getCreateDt(),
            runHistory.getExecutionBeginDt(),
            runHistory.getExecutionEndDt(),
            runHistory.getFailureReasonCode(),
            runHistory.getErrorMessage()
        );
    }

    @Override
    public LatestModelParamProposalParametersRes getLatestConfirmedParameters(Long scenarioId) {
        return getLatestParameters(scenarioId, ModelParamParameterSource.CONFIRMATION_PARAMETERS);
    }

    @Override
    public LatestModelParamProposalParametersRes getLatestProposedParameters(Long scenarioId) {
        return getLatestParameters(scenarioId, ModelParamParameterSource.PROPOSAL_PARAMETERS);
    }

    private LatestModelParamProposalParametersRes getLatestParameters(
        Long scenarioId,
        ModelParamParameterSource parameterSource
    ) {
        LatestModelParamProposalExecutionContext context = latestModelParamProposalExecutionContextResolver.resolve(scenarioId);
        AgentModelParamProposal proposal = agentModelParamProposalRepository
            .findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(
                context.modelParamProposalId(),
                context.scenarioId()
            )
            .orElseThrow(() -> {
                log.warn(
                    "Latest model parameter proposal execution has no active Agent proposal. scenarioId={}, caseId={}, execPrepId={}, modelParamProposalId={}",
                    context.scenarioId(),
                    context.caseId(),
                    context.execPrepId(),
                    context.modelParamProposalId()
                );
                return AppErrorException.of(
                    ResultCode.Error.RSC_NOT_FOUND,
                    "Agent model parameter proposal not found. scenarioId: %d, modelParamProposalId: %d"
                        .formatted(context.scenarioId(), context.modelParamProposalId())
                );
            });

        String parameterJson = switch (parameterSource) {
            case CONFIRMATION_PARAMETERS -> proposal.getConfirmationParameters();
            case PROPOSAL_PARAMETERS -> proposal.getProposalParameters();
            case NONE -> throw new IllegalArgumentException("parameterSource must select Agent parameters.");
        };
        try {
            return new LatestModelParamProposalParametersRes(
                context.scenarioId(),
                context.caseId(),
                context.execPrepId(),
                context.modelParamProposalId(),
                parameterSource,
                modelParameterTreeProjector.projectAllowingEmpty(parameterJson)
            );
        } catch (InvalidModelParameterTreeException e) {
            throw invalidAgentParameterJson();
        }
    }

    private AppErrorException invalidAgentParameterJson() {
        return AppErrorException.of(
            ResultCode.Error.API_RESPONSE_ERROR,
            "Agent proposal parameter JSON is invalid."
        );
    }

    private MiScenario findScenarioForUpdate(Long scenarioId) {
        return miScenarioRepository.findByScenarioIdForUpdate(scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found. scenarioId: %d".formatted(scenarioId)
            ));
    }

    private void rejectDiscardedScenario(MiScenario scenario) {
        if (scenario.getStatus() == ScenarioStatus.DISCARDED) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Discarded scenario cannot start a model execution. scenarioId: %d".formatted(scenario.getScenarioId())
            );
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.size() != REQUIRED_FILE_COUNT) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "files는 정확히 4개여야 합니다."
            );
        }
        boolean hasEmptyFile = files.stream().anyMatch(file -> file == null || file.isEmpty());
        if (hasEmptyFile) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID, "빈 파일은 업로드할 수 없습니다.");
        }
        validateRequiredInputFileRoles(files);
    }

    private void validateRequiredInputFileRoles(List<MultipartFile> files) {
        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        REQUIRED_INPUT_FILE_ROLE_KEYWORDS.forEach(keyword -> roleCounts.put(keyword, 0));

        for (MultipartFile file : files) {
            String originalFileName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
            String normalizedFileName = originalFileName.toUpperCase(Locale.ROOT);
            List<String> matchedRoles = REQUIRED_INPUT_FILE_ROLE_KEYWORDS.stream()
                .filter(normalizedFileName::contains)
                .toList();

            if (matchedRoles.size() != 1) {
                throw invalidInputFileRolesException();
            }
            String matchedRole = matchedRoles.get(0);
            roleCounts.put(matchedRole, roleCounts.get(matchedRole) + 1);
        }

        boolean hasInvalidRoleCount = roleCounts.values().stream().anyMatch(count -> count != 1);
        if (hasInvalidRoleCount) {
            throw invalidInputFileRolesException();
        }
    }

    private AppErrorException invalidInputFileRolesException() {
        return AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, REQUIRED_INPUT_FILE_ROLE_MESSAGE);
    }

    private List<ModelExecutionFileRes> findInputFiles(Long caseId) {
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );

        List<ModelExecutionFileRes> files = new ArrayList<>();
        for (int index = 0; index < uploadedFiles.size(); index++) {
            UploadedFile uploadedFile = uploadedFiles.get(index);
            files.add(new ModelExecutionFileRes(
                uploadedFile.getFileNo(),
                index + 1,
                uploadedFile.getOriginalFileNm(),
                uploadedFile.getMimeTypeNm(),
                uploadedFile.getFileSize()
            ));
        }
        return files;
    }

    private ModelExecutionStatus toModelExecutionStatus(FundamentalModelRunStatus status) {
        return ModelExecutionStatus.valueOf(status.name());
    }
}
