package com.sk.domain.mi.service.fundamentalmodel.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFileRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListItemRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunStatusRes;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FundamentalModelCaseMockService {

    private static final String MOCK_DATA_RESOURCE = "mock/fundamental-model/fundamental-model-cases.json";
    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final String MOCK_ASSIGNEE_NAME = "홍길동";
    private static final String DELETE_YN_Y = "Y";
    private static final String DELETE_YN_N = "N";

    private final ObjectMapper objectMapper;
    private final AtomicLong caseIdSequence = new AtomicLong(100);
    private final ConcurrentMap<Long, FundamentalModelCaseMockRecord> caseStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FundamentalModelRunMockRecord> runStore = new ConcurrentHashMap<>();

    public FundamentalModelCaseMockService() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    @Autowired
    public FundamentalModelCaseMockService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        reloadSeedData();
    }

    public synchronized FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request) {
        validateSearchCriteria(request);

        List<FundamentalModelCaseListItemRes> cases = caseStore.values().stream()
            .filter(this::isActive)
            .filter(record -> matchesSearch(record, request))
            .sorted(Comparator.comparing(FundamentalModelCaseMockRecord::createdAt).reversed())
            .map(this::toListItemResponse)
            .toList();

        return new FundamentalModelCaseListRes(cases.size(), cases);
    }

    public synchronized FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    ) {
        String normalizedTitle = validateAndNormalizeTitle(title);
        validateFiles(files);

        Long caseId = caseIdSequence.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        List<FundamentalModelFileMockRecord> fileRecords = toFileRecords(caseId, files);
        FundamentalModelCaseMockRecord record = new FundamentalModelCaseMockRecord(
            caseId,
            normalizedTitle,
            description,
            FundamentalModelCaseStatus.PENDING,
            MOCK_ASSIGNEE_NAME,
            fileRecords,
            null,
            DELETE_YN_N,
            now,
            now
        );
        caseStore.put(caseId, record);

        return new FundamentalModelCaseCreateRes(
            record.caseId(),
            record.title(),
            record.description(),
            record.status(),
            fileRecords.stream().map(this::toFileResponse).toList(),
            record.createdAt(),
            buildStatusEndpoint(record.caseId())
        );
    }

    public synchronized FundamentalModelCaseDetailRes getCaseDetail(Long caseId) {
        FundamentalModelCaseMockRecord caseRecord = getCaseRecord(caseId);
        return toDetailResponse(caseRecord);
    }

    public synchronized FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request) {
        FundamentalModelCaseMockRecord currentCase = getCaseRecord(caseId);
        String normalizedTitle = validateAndNormalizeTitle(request == null ? null : request.title());
        String description = request == null ? null : request.description();
        FundamentalModelCaseMockRecord updatedCase = currentCase.withMetadata(
            normalizedTitle,
            description,
            LocalDateTime.now()
        );
        caseStore.put(caseId, updatedCase);
        return toDetailResponse(updatedCase);
    }

    public synchronized void deleteCase(Long caseId) {
        FundamentalModelCaseMockRecord currentCase = getCaseRecord(caseId);
        caseStore.put(caseId, currentCase.withDeleted(LocalDateTime.now()));
    }

    public synchronized FundamentalModelRunAcceptRes runCase(Long caseId, FundamentalModelRunReq request) {
        FundamentalModelCaseMockRecord currentCase = getCaseRecord(caseId);
        if (currentCase.status() == FundamentalModelCaseStatus.RUNNING) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "case is already RUNNING"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String runId = UUID.randomUUID().toString();
        FundamentalModelRunMockRecord runRecord = new FundamentalModelRunMockRecord(
            runId,
            caseId,
            FundamentalModelRunStatus.RUNNING,
            now,
            now,
            null,
            null,
            null
        );
        FundamentalModelCaseMockRecord updatedCase = currentCase.withRunStarted(runId, now);

        runStore.put(runId, runRecord);
        caseStore.put(caseId, updatedCase);

        return new FundamentalModelRunAcceptRes(
            updatedCase.caseId(),
            runRecord.runId(),
            runRecord.status(),
            runRecord.requestedAt(),
            runRecord.startedAt(),
            buildStatusEndpoint(updatedCase.caseId())
        );
    }

    public synchronized FundamentalModelCaseStatusRes getCaseStatus(Long caseId) {
        FundamentalModelCaseMockRecord caseRecord = getCaseRecord(caseId);
        return toStatusResponse(caseRecord);
    }

    public synchronized FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        validateCallbackRequest(runId, request);

        FundamentalModelRunMockRecord currentRun = runStore.get(runId);
        if (currentRun == null) {
            throw AppHttpStatusException.of(
                HttpStatus.NOT_FOUND,
                ResultCode.Error.RSC_NOT_FOUND,
                "Fundamental model run을 찾을 수 없습니다."
            );
        }
        FundamentalModelCaseMockRecord currentCase = getCaseRecordByRun(currentRun);
        LocalDateTime processedAt = LocalDateTime.now();

        if (currentRun.status() != FundamentalModelRunStatus.RUNNING) {
            return handleTerminalCallbackReplay(currentRun, currentCase, request, processedAt);
        }

        FundamentalModelRunMockRecord updatedRun = currentRun.withTerminal(
            request.status(),
            request.finishedAt(),
            request.status() == FundamentalModelRunStatus.FAILED ? request.failureReason() : null,
            request.status() == FundamentalModelRunStatus.FAILED ? request.errorMessage() : null
        );
        runStore.put(runId, updatedRun);

        FundamentalModelCaseMockRecord latestCase = currentCase;
        if (runId.equals(currentCase.latestRunId())) {
            latestCase = currentCase.withTerminal(toCaseStatus(request.status()), processedAt);
            caseStore.put(latestCase.caseId(), latestCase);
        }

        return new FundamentalModelRunCallbackRes(
            updatedRun.runId(),
            latestCase.caseId(),
            updatedRun.status(),
            processedAt,
            false
        );
    }

    public synchronized void reset() {
        int removedCases = caseStore.size();
        int removedRuns = runStore.size();
        reloadSeedData();

        log.info("[Mock] Fundamental model case mock data reset: removedCases={}, removedRuns={}, loadedCases={}, loadedRuns={}",
            removedCases, removedRuns, caseStore.size(), runStore.size());
    }

    private void reloadSeedData() {
        try {
            loadSeedData();
            log.info("[Mock] Fundamental model mock data loaded: cases={}, runs={}",
                caseStore.size(), runStore.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load fundamental model mock data", e);
            caseStore.clear();
            runStore.clear();
            caseIdSequence.set(100);
        }
    }

    private void loadSeedData() throws IOException {
        ClassPathResource resource = new ClassPathResource(MOCK_DATA_RESOURCE);
        if (!resource.exists()) {
            caseStore.clear();
            runStore.clear();
            caseIdSequence.set(100);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            List<FundamentalModelCaseMockRecord> cases = readCases(root.path("cases"));
            List<FundamentalModelRunMockRecord> runs = readRuns(root.path("runs"));

            caseStore.clear();
            runStore.clear();
            cases.forEach(record -> caseStore.put(record.caseId(), record));
            runs.forEach(record -> runStore.put(record.runId(), record));

            long maxCaseId = cases.stream()
                .map(FundamentalModelCaseMockRecord::caseId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(100L);
            caseIdSequence.set(Math.max(100L, maxCaseId));
        }
    }

    private List<FundamentalModelCaseMockRecord> readCases(JsonNode casesNode) {
        if (!casesNode.isArray()) {
            return Collections.emptyList();
        }

        List<FundamentalModelCaseMockRecord> records = new ArrayList<>();
        for (JsonNode caseNode : casesNode) {
            records.add(new FundamentalModelCaseMockRecord(
                caseNode.path("caseId").asLong(),
                caseNode.path("title").asText(null),
                caseNode.path("description").asText(null),
                FundamentalModelCaseStatus.from(caseNode.path("status").asText()),
                caseNode.path("assigneeName").asText(MOCK_ASSIGNEE_NAME),
                readFiles(caseNode.path("files")),
                caseNode.path("latestRunId").isMissingNode() || caseNode.path("latestRunId").isNull()
                    ? null
                    : caseNode.path("latestRunId").asText(),
                caseNode.path("delYn").asText(DELETE_YN_N),
                LocalDateTime.parse(caseNode.path("createdAt").asText()),
                LocalDateTime.parse(caseNode.path("updatedAt").asText())
            ));
        }
        return List.copyOf(records);
    }

    private List<FundamentalModelFileMockRecord> readFiles(JsonNode filesNode) {
        if (!filesNode.isArray()) {
            return Collections.emptyList();
        }

        List<FundamentalModelFileMockRecord> records = new ArrayList<>();
        for (JsonNode fileNode : filesNode) {
            records.add(new FundamentalModelFileMockRecord(
                fileNode.path("mockFileId").asText(),
                fileNode.path("fileIndex").asInt(),
                fileNode.path("originalFileName").asText(null),
                fileNode.path("contentType").asText(null),
                fileNode.path("size").asLong()
            ));
        }
        return List.copyOf(records);
    }

    private List<FundamentalModelRunMockRecord> readRuns(JsonNode runsNode) {
        if (!runsNode.isArray()) {
            return Collections.emptyList();
        }

        List<FundamentalModelRunMockRecord> records = new ArrayList<>();
        for (JsonNode runNode : runsNode) {
            records.add(new FundamentalModelRunMockRecord(
                runNode.path("runId").asText(),
                runNode.path("caseId").asLong(),
                FundamentalModelRunStatus.from(runNode.path("status").asText()),
                LocalDateTime.parse(runNode.path("requestedAt").asText()),
                LocalDateTime.parse(runNode.path("startedAt").asText()),
                readNullableDateTime(runNode.path("finishedAt")),
                runNode.path("failureReason").asText(null),
                runNode.path("errorMessage").asText(null)
            ));
        }
        return List.copyOf(records);
    }

    private LocalDateTime readNullableDateTime(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return LocalDateTime.parse(node.asText());
    }

    private void validateSearchCriteria(FundamentalModelCaseListReq request) {
        if (normalizeSearchKeyword(request.keyword()) != null && request.searchType() == null) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "keyword가 입력된 경우 searchType은 필수입니다."
            );
        }
    }

    private boolean matchesSearch(FundamentalModelCaseMockRecord record, FundamentalModelCaseListReq request) {
        String keyword = normalizeSearchKeyword(request.keyword());
        if (keyword == null || request.searchType() == null) {
            return true;
        }

        String lowercaseKeyword = keyword.toLowerCase();
        if (request.searchType() == FundamentalModelCaseSearchType.TITLE) {
            return record.title().toLowerCase().contains(lowercaseKeyword);
        }
        if (request.searchType() == FundamentalModelCaseSearchType.ASSIGNEE_NAME) {
            return record.assigneeName().toLowerCase().contains(lowercaseKeyword);
        }
        return false;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    private String validateAndNormalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "title은 필수입니다.");
        }
        String normalized = title.trim();
        if (normalized.length() > TITLE_MAX_LENGTH) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "title은 200자를 초과할 수 없습니다.");
        }
        return normalized;
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

    private void validateCallbackRequest(String pathRunId, FundamentalModelRunCallbackReq request) {
        if (!pathRunId.equals(request.runId())) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "path runId와 body runId가 일치해야 합니다.");
        }
        if (!request.status().isTerminal()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback status는 COMPLETED 또는 FAILED 여야 합니다.");
        }
    }

    private FundamentalModelRunCallbackRes handleTerminalCallbackReplay(
        FundamentalModelRunMockRecord currentRun,
        FundamentalModelCaseMockRecord currentCase,
        FundamentalModelRunCallbackReq request,
        LocalDateTime processedAt
    ) {
        if (isSameTerminalPayload(currentRun, request)) {
            return new FundamentalModelRunCallbackRes(
                currentRun.runId(),
                currentCase.caseId(),
                currentRun.status(),
                processedAt,
                true
            );
        }

        if (currentRun.status() == FundamentalModelRunStatus.FAILED
            && request.status() == FundamentalModelRunStatus.COMPLETED) {
            log.info("[Mock] Late COMPLETED callback ignored: runId={}, caseId={}",
                currentRun.runId(), currentRun.caseId());
            return new FundamentalModelRunCallbackRes(
                currentRun.runId(),
                currentCase.caseId(),
                currentRun.status(),
                processedAt,
                false
            );
        }

        throw AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.BIZ_RULE_VIOLATION,
            "terminal callback payload differs from the stored run state"
        );
    }

    private boolean isSameTerminalPayload(FundamentalModelRunMockRecord currentRun, FundamentalModelRunCallbackReq request) {
        return currentRun.status() == request.status()
            && Objects.equals(currentRun.failureReason(), request.failureReason())
            && Objects.equals(currentRun.errorMessage(), request.errorMessage());
    }

    private FundamentalModelCaseMockRecord getCaseRecord(Long caseId) {
        FundamentalModelCaseMockRecord record = caseStore.get(caseId);
        if (record == null || !isActive(record)) {
            throw AppHttpStatusException.of(
                HttpStatus.NOT_FOUND,
                ResultCode.Error.RSC_NOT_FOUND,
                "Fundamental model case를 찾을 수 없습니다."
            );
        }
        return record;
    }

    private boolean isActive(FundamentalModelCaseMockRecord record) {
        return record != null && !DELETE_YN_Y.equalsIgnoreCase(record.delYn());
    }

    private FundamentalModelCaseMockRecord getCaseRecordByRun(FundamentalModelRunMockRecord runRecord) {
        FundamentalModelCaseMockRecord caseRecord = caseStore.get(runRecord.caseId());
        if (caseRecord == null) {
            throw AppHttpStatusException.of(
                HttpStatus.NOT_FOUND,
                ResultCode.Error.RSC_NOT_FOUND,
                "Fundamental model case를 찾을 수 없습니다."
            );
        }
        return caseRecord;
    }

    private List<FundamentalModelFileMockRecord> toFileRecords(Long caseId, List<MultipartFile> files) {
        List<FundamentalModelFileMockRecord> records = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            int fileIndex = index + 1;
            records.add(new FundamentalModelFileMockRecord(
                "mock_file_%d_%d".formatted(caseId, fileIndex),
                fileIndex,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
            ));
        }
        return List.copyOf(records);
    }

    private FundamentalModelCaseListItemRes toListItemResponse(FundamentalModelCaseMockRecord record) {
        return new FundamentalModelCaseListItemRes(
            record.caseId(),
            record.title(),
            record.status(),
            record.assigneeName(),
            record.createdAt(),
            record.updatedAt()
        );
    }

    private FundamentalModelCaseStatusRes toStatusResponse(FundamentalModelCaseMockRecord record) {
        FundamentalModelRunStatusRes latestRun = record.latestRunId() == null
            ? null
            : toRunStatusResponse(runStore.get(record.latestRunId()));

        return new FundamentalModelCaseStatusRes(
            record.caseId(),
            record.title(),
            record.status(),
            latestRun,
            record.createdAt(),
            record.updatedAt()
        );
    }

    private FundamentalModelCaseDetailRes toDetailResponse(FundamentalModelCaseMockRecord record) {
        FundamentalModelRunStatusRes latestRun = record.latestRunId() == null
            ? null
            : toRunStatusResponse(runStore.get(record.latestRunId()));

        return new FundamentalModelCaseDetailRes(
            record.caseId(),
            record.title(),
            record.description(),
            record.status(),
            record.assigneeName(),
            record.files().stream().map(this::toFileResponse).toList(),
            latestRun,
            record.createdAt(),
            record.updatedAt()
        );
    }

    private FundamentalModelRunStatusRes toRunStatusResponse(FundamentalModelRunMockRecord run) {
        if (run == null) {
            return null;
        }
        return new FundamentalModelRunStatusRes(
            run.runId(),
            run.status(),
            run.requestedAt(),
            run.startedAt(),
            run.finishedAt(),
            run.failureReason(),
            run.errorMessage()
        );
    }

    private FundamentalModelCaseFileRes toFileResponse(FundamentalModelFileMockRecord record) {
        return new FundamentalModelCaseFileRes(
            record.fileIndex(),
            record.originalFileName(),
            record.contentType(),
            record.size(),
            record.mockFileId()
        );
    }

    private FundamentalModelCaseStatus toCaseStatus(FundamentalModelRunStatus status) {
        return switch (status) {
            case COMPLETED -> FundamentalModelCaseStatus.COMPLETED;
            case FAILED -> FundamentalModelCaseStatus.FAILED;
            case RUNNING -> FundamentalModelCaseStatus.RUNNING;
        };
    }

    private String buildStatusEndpoint(Long caseId) {
        return "/api/v1/mi/fundamental-model/cases/%d/status"
            .formatted(caseId);
    }

    private record FundamentalModelCaseMockRecord(
        Long caseId,
        String title,
        String description,
        FundamentalModelCaseStatus status,
        String assigneeName,
        List<FundamentalModelFileMockRecord> files,
        String latestRunId,
        String delYn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {

        private FundamentalModelCaseMockRecord withMetadata(String title, String description, LocalDateTime updatedAt) {
            return new FundamentalModelCaseMockRecord(
                caseId,
                title,
                description,
                status,
                assigneeName,
                files,
                latestRunId,
                delYn,
                createdAt,
                updatedAt
            );
        }

        private FundamentalModelCaseMockRecord withDeleted(LocalDateTime updatedAt) {
            return new FundamentalModelCaseMockRecord(
                caseId,
                title,
                description,
                status,
                assigneeName,
                files,
                latestRunId,
                DELETE_YN_Y,
                createdAt,
                updatedAt
            );
        }

        private FundamentalModelCaseMockRecord withRunStarted(String runId, LocalDateTime updatedAt) {
            return new FundamentalModelCaseMockRecord(
                caseId,
                title,
                description,
                FundamentalModelCaseStatus.RUNNING,
                assigneeName,
                files,
                runId,
                delYn,
                createdAt,
                updatedAt
            );
        }

        private FundamentalModelCaseMockRecord withTerminal(FundamentalModelCaseStatus terminalStatus, LocalDateTime updatedAt) {
            return new FundamentalModelCaseMockRecord(
                caseId,
                title,
                description,
                terminalStatus,
                assigneeName,
                files,
                latestRunId,
                delYn,
                createdAt,
                updatedAt
            );
        }
    }

    private record FundamentalModelRunMockRecord(
        String runId,
        Long caseId,
        FundamentalModelRunStatus status,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String failureReason,
        String errorMessage
    ) {

        private FundamentalModelRunMockRecord withTerminal(
            FundamentalModelRunStatus terminalStatus,
            LocalDateTime finishedAt,
            String failureReason,
            String errorMessage
        ) {
            return new FundamentalModelRunMockRecord(
                runId,
                caseId,
                terminalStatus,
                requestedAt,
                startedAt,
                finishedAt,
                failureReason,
                errorMessage
            );
        }
    }

    private record FundamentalModelFileMockRecord(
        String mockFileId,
        Integer fileIndex,
        String originalFileName,
        String contentType,
        Long size
    ) {
    }
}
