package com.sk.domain.mi.service.nextgen.execution;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioExecutionRegistrar {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";
    private static final String CALLBACK_PATH_TEMPLATE = "/api/v1/mi/fundamental-model/runs/%s/callback";
    private static final DateTimeFormatter RUN_ID_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final FileService fileService;
    private final AuthorizationService authorizationService;
    private final ScenarioModelJobDispatcher jobDispatcher;
    private final PriceInterpretationStore priceInterpretationStore;

    @Value("${kubernetes.callback-base-url:}")
    private String callbackBaseUrl;

    @Transactional(propagation = Propagation.MANDATORY)
    public ModelExecutionAcceptRes registerStaged(
        MiScenario scenario,
        StagedScenarioExecutionFiles stagedFiles,
        Long execPrepId
    ) {
        Long scenarioId = scenario.getScenarioId();
        Integer actorUserNo = authorizationService.getUserNo();
        String actorLoginId = authorizationService.getSubject();

        priceInterpretationStore.softDeleteForModelExecution(scenarioId);
        ScenarioCase savedCase = createRunningScenarioCase(scenario, actorUserNo, actorLoginId);
        fileService.attachStagedFilesToRelationship(
            stagedFiles.fileNos(),
            Math.toIntExact(savedCase.getCaseId()),
            FILE_RELATION_SCENARIO_CASE,
            DateUtil.endYmd()
        );

        LocalDateTime requestedAt = LocalDateTime.now();
        String runId = generateRunId(requestedAt);
        scenarioCaseRunHistoryRepository.saveAndFlush(
            ScenarioCaseRunHistory.createRunning(savedCase.getCaseId(), runId, actorLoginId)
        );
        Long runSeq = scenarioCaseRunHistoryRepository.findRunSeqByRunId(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "scenario model execution run_seq not found after insert. runId: %s".formatted(runId)
            ));
        LocalDateTime executionBeginDt = scenarioCaseRunHistoryRepository.findExecutionBeginDtByRunId(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "scenario model execution begin time not found after insert. runId: %s".formatted(runId)
            ));

        savedCase.attachLatestRun(runId, actorLoginId);
        scenarioCaseRepository.flush();
        attachPreparation(execPrepId, savedCase.getCaseId(), actorLoginId);

        FundamentalModelJobArgs jobArgs = buildJobArgs(savedCase.getCaseId(), scenarioId, runId, runSeq);
        jobDispatcher.dispatchAfterCommit(savedCase.getCaseId(), runId, jobArgs, execPrepId);

        return new ModelExecutionAcceptRes(
            savedCase.getCaseId(),
            scenarioId,
            runId,
            ModelExecutionStatus.RUNNING,
            stagedFiles.toResponseFiles(),
            savedCase.getCreateDt(),
            executionBeginDt,
            buildStatusEndpoint(scenarioId, runId)
        );
    }

    private void attachPreparation(Long execPrepId, Long caseId, String actorLoginId) {
        if (execPrepId == null) {
            return;
        }
        int attached = modelExecutionPreparationRepository.attachCaseAndMarkExecuting(
            execPrepId,
            caseId,
            ModelExecutionPreparationStatus.PREPARING_EXECUTION,
            ModelExecutionPreparationStatus.EXECUTING,
            actorLoginId,
            LocalDateTime.now()
        );
        if (attached != 1) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "model execution preparation cannot attach case from current state. execPrepId: %d"
                    .formatted(execPrepId)
            );
        }
    }

    private ScenarioCase createRunningScenarioCase(MiScenario scenario, Integer actorUserNo, String actorLoginId) {
        try {
            return scenarioCaseRepository.saveAndFlush(ScenarioCase.createScenarioRunning(
                actorUserNo,
                scenario.getScenarioId(),
                buildCaseTitle(scenario),
                "Next Gen scenario model execution",
                actorLoginId
            ));
        } catch (DataIntegrityViolationException e) {
            log.info("Active scenario execution unique guard rejected submit. scenarioId={}", scenario.getScenarioId(), e);
            throw activeExecutionConflict(scenario.getScenarioId());
        }
    }

    private String buildCaseTitle(MiScenario scenario) {
        String title = "Scenario Model Execution - " + scenario.getTitle();
        if (title.length() <= TITLE_MAX_LENGTH) {
            return title;
        }
        return title.substring(0, TITLE_MAX_LENGTH);
    }

    private String generateRunId(LocalDateTime requestedAt) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return RUN_ID_TIMESTAMP_FORMATTER.format(requestedAt) + "_" + suffix;
    }

    private FundamentalModelJobArgs buildJobArgs(Long caseId, Long scenarioId, String runId, Long runSeq) {
        List<UploadedFile> inputFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );
        String demandExcelPath = findInputFileStoragePath(inputFiles, "DEMAND");
        String supplyExcelPath = findInputFileStoragePath(inputFiles, "SUPPLY");
        String tradeExcelPath = findInputFileStoragePath(inputFiles, "TRADE");
        String outputExcelPath = findInputFileStoragePath(inputFiles, "OUTPUT");
        validateRequiredInputFilePaths(demandExcelPath, supplyExcelPath, tradeExcelPath, outputExcelPath);

        return FundamentalModelJobArgs.builder()
            .caseId(caseId)
            .scenarioId(scenarioId)
            .runId(runId)
            .runSeq(runSeq)
            .demandExcelPath(demandExcelPath)
            .supplyExcelPath(supplyExcelPath)
            .tradeExcelPath(tradeExcelPath)
            .outputExcelPath(outputExcelPath)
            .callbackUrl(buildCallbackUrl(runId))
            .build();
    }

    private String findInputFileStoragePath(List<UploadedFile> inputFiles, String roleKeyword) {
        return inputFiles.stream()
            .filter(file -> Optional.ofNullable(file.getOriginalFileNm())
                .orElse("")
                .toUpperCase(Locale.ROOT)
                .contains(roleKeyword))
            .map(UploadedFile::getStoragePathNm)
            .filter(path -> path != null && !path.isBlank())
            .findFirst()
            .orElse(null);
    }

    private void validateRequiredInputFilePaths(
        String demandExcelPath,
        String supplyExcelPath,
        String tradeExcelPath,
        String outputExcelPath
    ) {
        if (isBlank(demandExcelPath)
            || isBlank(supplyExcelPath)
            || isBlank(tradeExcelPath)
            || isBlank(outputExcelPath)) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "case input files must include DEMAND, SUPPLY, TRADE, and OUTPUT storage paths."
            );
        }
    }

    private String buildCallbackUrl(String runId) {
        return normalizeCallbackBaseUrl() + CALLBACK_PATH_TEMPLATE.formatted(runId);
    }

    private String normalizeCallbackBaseUrl() {
        if (callbackBaseUrl == null || callbackBaseUrl.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "kubernetes.callback-base-url is required.");
        }
        String normalized = callbackBaseUrl.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AppHttpStatusException activeExecutionConflict(Long scenarioId) {
        return AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.RSC_ALREADY_EXISTS,
            "해당 scenario에는 이미 RUNNING 상태의 model execution이 존재합니다. scenarioId: %d".formatted(scenarioId)
        );
    }

    private String buildStatusEndpoint(Long scenarioId, String runId) {
        return "/api/v1/mi/next-gen/scenarios/%d/model-executions/%s".formatted(scenarioId, runId);
    }
}
