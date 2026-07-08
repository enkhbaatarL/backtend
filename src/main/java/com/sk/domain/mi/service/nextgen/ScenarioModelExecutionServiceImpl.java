package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionFileRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioModelExecutionServiceImpl implements ScenarioModelExecutionService {

    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final List<String> REQUIRED_INPUT_FILE_ROLE_KEYWORDS = List.of("DEMAND", "SUPPLY", "TRADE");
    private static final String REQUIRED_INPUT_FILE_ROLE_MESSAGE =
        "files는 파일명에 DEMAND, SUPPLY, TRADE 키워드를 각각 정확히 1개씩 포함해야 합니다.";
    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";
    private static final String CALLBACK_ACTOR = "SYSTEM";
    private static final String JOB_SUBMIT_FAILURE_REASON = "JOB_SUBMIT_FAILED";
    private static final String CALLBACK_PATH_TEMPLATE = "/api/v1/mi/fundamental-model/runs/%s/callback";
    private static final DateTimeFormatter RUN_ID_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final MiScenarioRepository miScenarioRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileService fileService;
    private final AuthorizationService authorizationService;
    private final KubernetesJobService kubernetesJobService;
    private final TransactionOperations transactionOperations;

    @Value("${kubernetes.callback-base-url:}")
    private String callbackBaseUrl;

    @Override
    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario model execution parameters 미구현");
    }

    @Override
    @Transactional
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files) {
        MiScenario scenario = findScenarioForUpdate(scenarioId);
        rejectDiscardedScenario(scenario);
        validateFiles(files);
        rejectIfActiveExecutionExists(scenarioId);

        Integer actorUserNo = authorizationService.getUserNo();
        String actorLoginId = authorizationService.getSubject();
        String companyCd = authorizationService.getCompanyCdWithPriority();
        Role role = authorizationService.getCurrentRole();

        ScenarioCase savedCase = createRunningScenarioCase(scenario, actorUserNo, actorLoginId);
        List<Long> uploadedFileNos = new ArrayList<>();
        try {
            List<ModelExecutionFileRes> uploadedFiles = uploadCaseFiles(
                companyCd,
                role,
                savedCase.getCaseId(),
                files,
                uploadedFileNos
            );

            LocalDateTime requestedAt = LocalDateTime.now();
            String runId = generateRunId(requestedAt);
            ScenarioCaseRunHistory runHistory = scenarioCaseRunHistoryRepository.saveAndFlush(
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

            FundamentalModelJobArgs jobArgs = buildJobArgs(savedCase.getCaseId(), scenarioId, runId, runSeq);
            submitJobAfterCommit(savedCase.getCaseId(), runId, jobArgs);

            return new ModelExecutionAcceptRes(
                savedCase.getCaseId(),
                scenarioId,
                runId,
                ModelExecutionStatus.RUNNING,
                uploadedFiles,
                savedCase.getCreateDt(),
                executionBeginDt,
                buildStatusEndpoint(scenarioId, runId)
            );
        } catch (RuntimeException e) {
            compensateUploadedFiles(uploadedFileNos);
            throw e;
        }
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

    private MiScenario findScenario(Long scenarioId) {
        return miScenarioRepository.findByScenarioIdAndIsDeletedFalse(scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found. scenarioId: %d".formatted(scenarioId)
            ));
    }

    private MiScenario findScenarioForUpdate(Long scenarioId) {
        return miScenarioRepository.findByScenarioIdForUpdate(scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found. scenarioId: %d".formatted(scenarioId)
            ));
    }

    private void rejectIfActiveExecutionExists(Long scenarioId) {
        boolean exists = scenarioCaseRepository.existsByScenarioIdAndExecTypeAndStatusAndIsDeletedFalse(
            scenarioId,
            FundamentalModelExecType.SCENARIO,
            FundamentalModelCaseStatus.RUNNING
        );
        if (exists) {
            throw activeExecutionConflict(scenarioId);
        }
    }

    private void rejectDiscardedScenario(MiScenario scenario) {
        if (scenario.getStatus() == ScenarioStatus.DISCARDED) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Discarded scenario cannot start a model execution. scenarioId: %d".formatted(scenario.getScenarioId())
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

    private AppHttpStatusException activeExecutionConflict(Long scenarioId) {
        return AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.RSC_ALREADY_EXISTS,
            "해당 scenario에는 이미 RUNNING 상태의 model execution이 존재합니다. scenarioId: %d".formatted(scenarioId)
        );
    }

    private String buildCaseTitle(MiScenario scenario) {
        String title = "Scenario Model Execution - " + scenario.getTitle();
        if (title.length() <= TITLE_MAX_LENGTH) {
            return title;
        }
        return title.substring(0, TITLE_MAX_LENGTH);
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.size() != REQUIRED_FILE_COUNT) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "files는 정확히 3개여야 합니다."
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

    private List<ModelExecutionFileRes> uploadCaseFiles(
        String companyCd,
        Role role,
        Long caseId,
        List<MultipartFile> files,
        List<Long> uploadedFileNos
    ) {
        Integer fileRelationshipObjectDistinguishNo = Math.toIntExact(caseId);
        List<ModelExecutionFileRes> uploadedFiles = new ArrayList<>();

        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            FileUploadRes uploadedFile = fileService.uploadFileToRelationship(
                companyCd,
                role,
                file,
                false,
                fileRelationshipObjectDistinguishNo,
                FILE_RELATION_SCENARIO_CASE
            );
            uploadedFileNos.add(uploadedFile.fileNo());
            uploadedFiles.add(new ModelExecutionFileRes(
                uploadedFile.fileNo(),
                index + 1,
                uploadedFile.fileNm(),
                file.getContentType(),
                uploadedFile.fileSize()
            ));
        }

        return uploadedFiles;
    }

    private void compensateUploadedFiles(List<Long> uploadedFileNos) {
        for (Long fileNo : uploadedFileNos) {
            try {
                fileService.deleteFile(fileNo);
            } catch (RuntimeException compensationException) {
                log.warn("Failed to compensate scenario model execution uploaded file. fileNo={}", fileNo, compensationException);
            }
        }
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
        validateRequiredInputFilePaths(demandExcelPath, supplyExcelPath, tradeExcelPath);

        return FundamentalModelJobArgs.builder()
            .caseId(caseId)
            .scenarioId(scenarioId)
            .runId(runId)
            .runSeq(runSeq)
            .demandExcelPath(demandExcelPath)
            .supplyExcelPath(supplyExcelPath)
            .tradeExcelPath(tradeExcelPath)
            .callbackUrl(buildCallbackUrl(runId))
            .build();
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

    private void validateRequiredInputFilePaths(String demandExcelPath, String supplyExcelPath, String tradeExcelPath) {
        if (isBlank(demandExcelPath) || isBlank(supplyExcelPath) || isBlank(tradeExcelPath)) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "case input files must include DEMAND, SUPPLY, and TRADE storage paths."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void submitJobAfterCommit(Long caseId, String runId, FundamentalModelJobArgs jobArgs) {
        Runnable submitJob = () -> submitJob(caseId, runId, jobArgs);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitJob.run();
                }
            });
            return;
        }
        submitJob.run();
    }

    private void submitJob(Long caseId, String runId, FundamentalModelJobArgs jobArgs) {
        try {
            kubernetesJobService.createFundamentalModelJob(jobArgs)
                .whenComplete((ignored, ex) -> {
                    if (ex != null) {
                        log.warn("Kubernetes scenario model job creation failed: runId={}, caseId={}", runId, caseId, ex);
                        markRunFailed(runId, caseId, rootCauseMessage(ex));
                    }
                });
        } catch (RuntimeException e) {
            log.warn("Kubernetes scenario model job creation failed: runId={}, caseId={}", runId, caseId, e);
            markRunFailed(runId, caseId, rootCauseMessage(e));
        }
    }

    private void markRunFailed(String runId, Long caseId, String errorMessage) {
        transactionOperations.executeWithoutResult((TransactionStatus ignored) -> {
            LocalDateTime failedAt = LocalDateTime.now();
            int updateCount = scenarioCaseRunHistoryRepository.updateRunningToTerminal(
                runId,
                FundamentalModelRunStatus.FAILED,
                FundamentalModelRunStatus.RUNNING,
                failedAt,
                JOB_SUBMIT_FAILURE_REASON,
                truncateErrorMessage(errorMessage),
                failedAt,
                CALLBACK_ACTOR
            );
            if (updateCount > 0) {
                scenarioCaseRepository.updateLatestRunStatus(
                    runId,
                    FundamentalModelCaseStatus.FAILED,
                    failedAt,
                    CALLBACK_ACTOR
                );
            } else {
                log.info("Skip marking non-running scenario model run as failed. runId={}, caseId={}", runId, caseId);
            }
        });
    }

    private String rootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= 2000) {
            return errorMessage;
        }
        return errorMessage.substring(0, 2000);
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

    private String buildStatusEndpoint(Long scenarioId, String runId) {
        return "/api/v1/mi/next-gen/scenarios/%d/model-executions/%s".formatted(scenarioId, runId);
    }
}
