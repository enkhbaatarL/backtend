package com.sk.domain.mi.service.fundamentalmodel.mock;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FundamentalModelCaseMockService {

    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final String MOCK_ASSIGNEE_NAME = "홍길동";

    private final AtomicLong caseIdSequence = new AtomicLong(100);
    private final ConcurrentMap<Long, FundamentalModelCaseMockRecord> caseStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FundamentalModelRunMockRecord> runStore = new ConcurrentHashMap<>();

    public synchronized FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request) {
        validateSearchPair(request);

        List<FundamentalModelCaseListItemRes> cases = caseStore.values().stream()
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
        caseStore.clear();
        runStore.clear();

        log.info("[Mock] Fundamental model case mock data reset: removedCases={}, removedRuns={}",
            removedCases, removedRuns);
    }

    private void validateSearchPair(FundamentalModelCaseListReq request) {
        if ((request.searchType() == null) != (request.keyword() == null)) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "searchType과 keyword는 함께 입력해야 합니다."
            );
        }
    }

    private boolean matchesSearch(FundamentalModelCaseMockRecord record, FundamentalModelCaseListReq request) {
        if (request.searchType() == null || request.keyword() == null || request.keyword().trim().isEmpty()) {
            return true;
        }

        String keyword = request.keyword().trim().toLowerCase();
        if (request.searchType() == FundamentalModelCaseSearchType.TITLE) {
            return record.title().toLowerCase().contains(keyword);
        }
        if (request.searchType() == FundamentalModelCaseSearchType.ASSIGNEE_NAME) {
            return record.assigneeName().toLowerCase().contains(keyword);
        }
        return false;
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
        if (record == null) {
            throw AppHttpStatusException.of(
                HttpStatus.NOT_FOUND,
                ResultCode.Error.RSC_NOT_FOUND,
                "Fundamental model case를 찾을 수 없습니다."
            );
        }
        return record;
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {

        private FundamentalModelCaseMockRecord withRunStarted(String runId, LocalDateTime updatedAt) {
            return new FundamentalModelCaseMockRecord(
                caseId,
                title,
                description,
                FundamentalModelCaseStatus.RUNNING,
                assigneeName,
                files,
                runId,
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
