package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ModelExecutionCallbackReq;
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
        String executionId = generateExecutionId();
        String externalRunId = generateExternalRunId();
        List<ModelExecutionFileRes> uploadedFiles = mockUploadFiles(files);

        StoredExecution pendingExecution = new StoredExecution(
            executionId,
            scenarioId,
            externalRunId,
            ModelExecutionStatus.QUEUED,
            requestedAt,
            null,
            null,
            null,
            uploadedFiles
        );
        executionStore.put(executionId, pendingExecution);
        simulateExternalDispatch(pendingExecution);

        return new ModelExecutionAcceptRes(
            executionId,
            scenarioId,
            externalRunId,
            ModelExecutionStatus.QUEUED,
            uploadedFiles,
            requestedAt,
            buildStatusEndpoint(scenarioId, executionId)
        );
    }

    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String executionId) {
        return getExecution(scenarioId, executionId).toResponse();
    }

    public ModelExecutionStatusRes updateModelExecutionFromCallback(
        Long scenarioId,
        String executionId,
        ModelExecutionCallbackReq request
    ) {
        if (!request.status().isTerminal()) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "콜백 status는 SUCCEEDED 또는 FAILED 여야 합니다."
            );
        }

        StoredExecution current = getExecution(scenarioId, executionId);
        StoredExecution currentWithTimeout = applyTimeoutIfNeeded(current);
        if (currentWithTimeout.status() != ModelExecutionStatus.RUNNING) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "콜백은 RUNNING 상태의 execution에만 허용됩니다."
            );
        }
        if (!currentWithTimeout.externalRunId().equals(request.externalRunId())) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "externalRunId가 execution과 일치하지 않습니다."
            );
        }
        if (request.status() == ModelExecutionStatus.FAILED
            && (request.errorMessage() == null || request.errorMessage().isBlank())) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "FAILED callback에는 errorMessage가 필요합니다."
            );
        }

        LocalDateTime finishedAt = request.finishedAt() != null ? request.finishedAt() : LocalDateTime.now();
        LocalDateTime startedAt = currentWithTimeout.startedAt();
        String errorMessage = request.status() == ModelExecutionStatus.FAILED ? request.errorMessage() : null;

        StoredExecution updated = new StoredExecution(
            currentWithTimeout.executionId(),
            currentWithTimeout.scenarioId(),
            currentWithTimeout.externalRunId(),
            request.status(),
            currentWithTimeout.requestedAt(),
            startedAt,
            finishedAt,
            errorMessage,
            currentWithTimeout.files()
        );
        executionStore.put(executionId, updated);
        return updated.toResponse();
    }

    private StoredExecution getExecution(Long scenarioId, String executionId) {
        StoredExecution execution = executionStore.get(executionId);
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
            .anyMatch(execution -> execution.status() == ModelExecutionStatus.QUEUED
                || execution.status() == ModelExecutionStatus.RUNNING);

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
                pendingExecution.executionId(),
                pendingExecution.scenarioId(),
                pendingExecution.externalRunId(),
                ModelExecutionStatus.FAILED,
                pendingExecution.requestedAt(),
                null,
                LocalDateTime.now(),
                MOCK_DISPATCH_FAILURE_MESSAGE,
                pendingExecution.files()
            );
            executionStore.put(pendingExecution.executionId(), failedExecution);
            return;
        }

        StoredExecution runningExecution = new StoredExecution(
            pendingExecution.executionId(),
            pendingExecution.scenarioId(),
            pendingExecution.externalRunId(),
            ModelExecutionStatus.RUNNING,
            pendingExecution.requestedAt(),
            LocalDateTime.now(),
            null,
            null,
            pendingExecution.files()
        );
        executionStore.put(pendingExecution.executionId(), runningExecution);
        scenarioGroupMockService.markModelRunning(runningExecution.scenarioId()); // 그래프 mock 실행중 표기 연동
        scheduleAutoComplete(runningExecution);
    }

    private void scheduleAutoComplete(StoredExecution runningExecution) {
        long delaySeconds = mockProperties.getModelExecutions().getWorkflow().getAutoCompleteSeconds();
        scheduler.schedule(() -> {
            StoredExecution current = executionStore.get(runningExecution.executionId());
            if (current != null && current.status() == ModelExecutionStatus.RUNNING) {
                StoredExecution completed = new StoredExecution(
                    current.executionId(),
                    current.scenarioId(),
                    current.externalRunId(),
                    ModelExecutionStatus.SUCCEEDED,
                    current.requestedAt(),
                    current.startedAt(),
                    LocalDateTime.now(),
                    null,
                    current.files()
                );
                executionStore.put(current.executionId(), completed);
                scenarioGroupMockService.markModelIdle(current.scenarioId());
                log.info("[Mock] Auto-completed model execution: executionId={}, scenarioId={}",
                    current.executionId(), current.scenarioId());
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
            execution.executionId(),
            execution.scenarioId(),
            execution.externalRunId(),
            ModelExecutionStatus.FAILED,
            execution.requestedAt(),
            execution.startedAt(),
            LocalDateTime.now(),
            TIMEOUT_ERROR_MESSAGE,
            execution.files()
        );
        executionStore.put(execution.executionId(), timedOutExecution);
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

    private String generateExecutionId() {
        return "exec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String generateExternalRunId() {
        return "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String buildStatusEndpoint(Long scenarioId, String executionId) {
        return "/api/v1/mi/next-gen/scenarios/" + scenarioId + "/model-executions/" + executionId;
    }

    private record StoredExecution(
        String executionId,
        Long scenarioId,
        String externalRunId,
        ModelExecutionStatus status,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String errorMessage,
        List<ModelExecutionFileRes> files
    ) {

        private ModelExecutionStatusRes toResponse() {
            return new ModelExecutionStatusRes(
                executionId,
                scenarioId,
                externalRunId,
                status,
                files,
                requestedAt,
                startedAt,
                finishedAt,
                errorMessage
            );
        }
    }
}
