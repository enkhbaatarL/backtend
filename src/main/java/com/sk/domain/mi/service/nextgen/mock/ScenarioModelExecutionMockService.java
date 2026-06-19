package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionFileRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterTableRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioModelExecutionMockService {

    private static final int REQUIRED_FILE_COUNT = 3;
    private static final String MOCK_DISPATCH_FAILURE_MESSAGE = "Mock external API dispatch failed.";
    private static final String TIMEOUT_ERROR_MESSAGE = "Model execution timed out.";

    private final ObjectMapper objectMapper;
    private final NextGenMockProperties mockProperties;
    private final ScenarioGroupMockService scenarioGroupMockService;
    private final ConcurrentMap<String, StoredExecution> executionStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong caseIdSequence = new AtomicLong(100);
    private final AtomicLong fileNoSequence = new AtomicLong(1000);
    private Map<String, List<ModelExecutionParameterTableRes>> parameterTablesByScenario;

    @PostConstruct
    void init() {
        try {
            loadModelExecutionParameters();
            log.info("[Mock] Scenario model execution mock data loaded: parameterScenarios={}",
                parameterTablesByScenario.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load scenario model execution mock data", e);
            parameterTablesByScenario = Collections.emptyMap();
        }
    }

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
    }

    public void reset() {
        executionStore.clear();
        log.info("[Mock] Model execution mock data reset to initial state");
    }

    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        String scenarioName = getScenarioName(scenarioId);
        return new ModelExecutionParameterRes(
            scenarioId,
            scenarioName,
            getMockTables(scenarioId)
        );
    }

    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files) {
        getScenarioName(scenarioId);
        validateFiles(files);
        rejectIfActiveExecutionExists(scenarioId);

        LocalDateTime requestedAt = LocalDateTime.now();
        Long caseId = caseIdSequence.incrementAndGet();
        String runId = generateRunId(requestedAt);
        List<ModelExecutionFileRes> uploadedFiles = mockUploadFiles(files);

        StoredExecution runningExecution = new StoredExecution(
            caseId,
            runId,
            scenarioId,
            ModelExecutionStatus.RUNNING,
            requestedAt,
            requestedAt,
            null,
            null,
            null,
            uploadedFiles
        );
        executionStore.put(runId, runningExecution);
        simulateExternalDispatch(runningExecution);

        return new ModelExecutionAcceptRes(
            caseId,
            scenarioId,
            runId,
            ModelExecutionStatus.RUNNING,
            uploadedFiles,
            requestedAt,
            runningExecution.startedAt(),
            buildStatusEndpoint(scenarioId, runId)
        );
    }

    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String runId) {
        return getExecution(scenarioId, runId).toResponse();
    }

    private StoredExecution getExecution(Long scenarioId, String runId) {
        StoredExecution execution = executionStore.get(runId);
        if (execution == null || !execution.scenarioId().equals(scenarioId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Model execution을 찾을 수 없습니다.");
        }
        return applyTimeoutIfNeeded(execution);
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
    }

    private List<ModelExecutionFileRes> mockUploadFiles(List<MultipartFile> files) {
        List<ModelExecutionFileRes> uploadedFiles = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            uploadedFiles.add(new ModelExecutionFileRes(
                fileNoSequence.incrementAndGet(),
                index + 1,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
            ));
        }
        return uploadedFiles;
    }

    private void rejectIfActiveExecutionExists(Long scenarioId) {
        boolean hasActiveExecution = executionStore.values().stream()
            .filter(execution -> execution.scenarioId().equals(scenarioId))
            .map(this::applyTimeoutIfNeeded)
            .anyMatch(execution -> execution.status() == ModelExecutionStatus.RUNNING);

        if (hasActiveExecution) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "해당 scenario에는 이미 진행 중인 model execution이 존재합니다."
            );
        }
    }

    private void simulateExternalDispatch(StoredExecution pendingExecution) {
        boolean dispatchSuccess = mockProperties.getModelExecutions().getWorkflow().isDispatchSuccess();
        if (!dispatchSuccess) {
            StoredExecution failedExecution = new StoredExecution(
                pendingExecution.caseId(),
                pendingExecution.runId(),
                pendingExecution.scenarioId(),
                ModelExecutionStatus.FAILED,
                pendingExecution.requestedAt(),
                pendingExecution.startedAt(),
                LocalDateTime.now(),
                "JOB_SUBMIT_FAILED",
                MOCK_DISPATCH_FAILURE_MESSAGE,
                pendingExecution.files()
            );
            executionStore.put(pendingExecution.runId(), failedExecution);
            return;
        }

        scenarioGroupMockService.markModelRunning(pendingExecution.scenarioId()); // 그래프 mock 실행중 표기 연동
        scheduleAutoComplete(pendingExecution);
    }

    private void scheduleAutoComplete(StoredExecution runningExecution) {
        long delaySeconds = mockProperties.getModelExecutions().getWorkflow().getAutoCompleteSeconds();
        scheduler.schedule(() -> {
            StoredExecution current = executionStore.get(runningExecution.runId());
            if (current != null && current.status() == ModelExecutionStatus.RUNNING) {
                StoredExecution completed = new StoredExecution(
                    current.caseId(),
                    current.runId(),
                    current.scenarioId(),
                    ModelExecutionStatus.COMPLETED,
                    current.requestedAt(),
                    current.startedAt(),
                    LocalDateTime.now(),
                    null,
                    null,
                    current.files()
                );
                executionStore.put(current.runId(), completed);
                scenarioGroupMockService.markModelIdle(current.scenarioId());
                log.info("[Mock] Auto-completed model execution: runId={}, scenarioId={}",
                    current.runId(), current.scenarioId());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private StoredExecution applyTimeoutIfNeeded(StoredExecution execution) {
        if (execution.status() != ModelExecutionStatus.RUNNING || execution.startedAt() == null) {
            return execution;
        }

        Duration runningTimeout = Duration.ofMinutes(
            mockProperties.getModelExecutions().getWorkflow().getRunningTimeoutMinutes()
        );
        if (!execution.startedAt().isBefore(LocalDateTime.now().minus(runningTimeout))) {
            return execution;
        }

        StoredExecution timedOutExecution = new StoredExecution(
            execution.caseId(),
            execution.runId(),
            execution.scenarioId(),
            ModelExecutionStatus.FAILED,
            execution.requestedAt(),
            execution.startedAt(),
            LocalDateTime.now(),
            "TIMEOUT",
            TIMEOUT_ERROR_MESSAGE,
            execution.files()
        );
        executionStore.put(execution.runId(), timedOutExecution);
        scenarioGroupMockService.markModelIdle(execution.scenarioId());
        return timedOutExecution;
    }

    private String getScenarioName(Long scenarioId) {
        return scenarioGroupMockService.findScenarioName(scenarioId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario를 찾을 수 없습니다."));
    }

    private void loadModelExecutionParameters() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/model-execution-parameters.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode parametersNode = root.get("parameterTablesByScenario");
            parameterTablesByScenario = objectMapper.readValue(
                parametersNode.traverse(objectMapper),
                new TypeReference<>() {}
            );
        }
    }

    private List<ModelExecutionParameterTableRes> getMockTables(Long scenarioId) {
        List<ModelExecutionParameterTableRes> scenarioTables = parameterTablesByScenario.get(String.valueOf(scenarioId));
        if (scenarioTables != null) {
            return scenarioTables;
        }

        List<ModelExecutionParameterTableRes> defaultTables = parameterTablesByScenario.get("default");
        if (defaultTables != null) {
            return defaultTables;
        }

        throw AppErrorException.of(
            ResultCode.Error.RSC_NOT_FOUND,
            "Scenario model execution parameter mock data를 찾을 수 없습니다."
        );
    }

    private String generateRunId(LocalDateTime requestedAt) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return requestedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "_" + suffix;
    }

    private String buildStatusEndpoint(Long scenarioId, String runId) {
        return "/api/v1/mi/next-gen/scenarios/" + scenarioId + "/model-executions/" + runId;
    }

    private record StoredExecution(
        Long caseId,
        String runId,
        Long scenarioId,
        ModelExecutionStatus status,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String failureReason,
        String errorMessage,
        List<ModelExecutionFileRes> files
    ) {

        private ModelExecutionStatusRes toResponse() {
            return new ModelExecutionStatusRes(
                caseId,
                scenarioId,
                runId,
                status,
                files,
                requestedAt,
                startedAt,
                finishedAt,
                failureReason,
                errorMessage
            );
        }
    }
}
