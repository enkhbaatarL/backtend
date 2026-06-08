package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FilterOption;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFileRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListItemRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelPriceRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelTradeFlowsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesFilterOptionsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.VolumesSegmentFilterOptions;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalModelCaseServiceImpl implements FundamentalModelCaseService {

    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final String FILE_RELATION_SCENARIO_CASE = "SCNCASE";

    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileService fileService;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request) {
        FundamentalModelCaseSearchType searchType = request == null ? null : request.searchType();
        String keyword = normalizeKeyword(request == null ? null : request.keyword());
        if (keyword != null && searchType == null) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "keyword가 전달되면 searchType은 필수입니다."
            );
        }
        if (keyword == null) {
            searchType = null;
        }

        List<FundamentalModelCaseListItemRes> cases = scenarioCaseRepository.findListRows(searchType, keyword)
            .stream()
            .map(this::toListItemResponse)
            .toList();
        return new FundamentalModelCaseListRes(cases.size(), cases);
    }

    @Override
    @Transactional
    public FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    ) {
        String normalizedTitle = validateAndNormalizeTitle(title);
        validateFiles(files);

        Integer actorUserNo = authorizationService.getUserNo();
        String actorLoginId = authorizationService.getSubject();
        String companyCd = authorizationService.getCompanyCd();
        Role role = authorizationService.getCurrentRole();

        ScenarioCase savedCase = scenarioCaseRepository.saveAndFlush(
            ScenarioCase.createPending(actorUserNo, normalizedTitle, description, actorLoginId)
        );

        List<Long> uploadedFileNos = new ArrayList<>();
        try {
            List<FundamentalModelCaseFileRes> uploadedFiles = uploadCaseFiles(
                companyCd,
                role,
                savedCase.getCaseId(),
                files,
                uploadedFileNos
            );

            return new FundamentalModelCaseCreateRes(
                savedCase.getCaseId(),
                savedCase.getTitle(),
                savedCase.getDescription(),
                savedCase.getStatus(),
                uploadedFiles,
                savedCase.getCreateDt(),
                buildStatusEndpoint(savedCase.getCaseId())
            );
        } catch (RuntimeException e) {
            compensateUploadedFiles(uploadedFileNos);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelCaseDetailRes getCaseDetail(Long caseId) {
        ScenarioCaseDetailRow caseRow = scenarioCaseRepository.findDetailRow(caseId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model case not found. caseId: %d".formatted(caseId)
            ));

        List<FundamentalModelCaseFileRes> files = findCaseFiles(caseId);
        FundamentalModelRunStatusRes latestRun = findLatestRun(caseRow.lastRunId());

        return new FundamentalModelCaseDetailRes(
            caseRow.caseId(),
            caseRow.title(),
            caseRow.description(),
            caseRow.status(),
            caseRow.assigneeName(),
            caseRow.assigneeUserNo(),
            files,
            latestRun,
            caseRow.createdAt(),
            caseRow.updatedAt()
        );
    }

    @Override
    @Transactional
    public FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request) {
        String normalizedTitle = validateAndNormalizeTitle(request == null ? null : request.title());
        String description = request == null ? null : request.description();
        ScenarioCase scenarioCase = findActiveOwnedCase(caseId);

        scenarioCase.updateMetadata(
            normalizedTitle,
            description,
            authorizationService.getSubject()
        );
        scenarioCaseRepository.flush();

        return getCaseDetail(caseId);
    }

    @Override
    @Transactional
    public void deleteCase(Long caseId) {
        ScenarioCase scenarioCase = findActiveOwnedCase(caseId);
        scenarioCase.softDelete(authorizationService.getSubject());
    }

    @Override
    public FundamentalModelRunAcceptRes runCase(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model case run DB implementation is not ready.");
    }

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelCaseStatusRes getCaseStatus(Long caseId) {
        ScenarioCase scenarioCase = findActiveCase(caseId);
        FundamentalModelRunStatusRes latestRun = scenarioCase.getStatus() == FundamentalModelCaseStatus.PENDING
            ? null
            : findLatestRun(scenarioCase.getLastRunId());

        return new FundamentalModelCaseStatusRes(
            scenarioCase.getCaseId(),
            scenarioCase.getTitle(),
            scenarioCase.getStatus(),
            latestRun,
            scenarioCase.getCreateDt(),
            scenarioCase.getModifyDt()
        );
    }

    @Override
    public FundamentalModelVolumesFilterOptionsRes getVolumesFilterOptions() {
        List<FilterOption> allRegions = Arrays.stream(FundamentalModelRegion.values())
            .map(r -> new FilterOption(r.getCode(), r.getLabel()))
            .toList();
        List<FilterOption> granularities = Arrays.stream(FundamentalModelGranularity.values())
            .map(g -> new FilterOption(g.getCode(), g.getLabel()))
            .toList();

        VolumesSegmentFilterOptions global = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.GLOBAL,
            Arrays.stream(FundamentalModelDataType.values())
                .map(t -> new FilterOption(t.getCode(), t.getLabel()))
                .toList(),
            allRegions,
            granularities
        );
        VolumesSegmentFilterOptions lng = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.LNG,
            List.of(
                new FilterOption(FundamentalModelDataType.LNG_IMPORTS.getCode(), FundamentalModelDataType.LNG_IMPORTS.getLabel()),
                new FilterOption(FundamentalModelDataType.LNG_EXPORTS.getCode(), FundamentalModelDataType.LNG_EXPORTS.getLabel())
            ),
            allRegions,
            granularities
        );
        VolumesSegmentFilterOptions trade = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.TRADE,
            Arrays.stream(TradeFlowDirection.values())
                .map(d -> new FilterOption(d.getCode(), d.getLabel()))
                .toList(),
            allRegions,
            List.of()
        );

        return new FundamentalModelVolumesFilterOptionsRes(List.of(global, lng, trade));
    }

    @Override
    public FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request) {
        throw new UnsupportedOperationException("Fundamental model volumes DB implementation is not ready.");
    }

    @Override
    public FundamentalModelTradeFlowsRes getTradeFlows(Long caseId, FundamentalModelTradeFlowsReq request) {
        throw new UnsupportedOperationException("Fundamental model trade flows DB implementation is not ready.");
    }

    @Override
    public FundamentalModelPriceRes getPrice(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model price DB implementation is not ready.");
    }

    @Override
    public FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        throw new UnsupportedOperationException("Fundamental model run callback DB implementation is not ready.");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Fundamental model case reset DB implementation is not ready.");
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    private ScenarioCase findActiveCase(Long caseId) {
        return scenarioCaseRepository.findByCaseIdAndIsDeletedFalse(caseId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model case not found. caseId: %d".formatted(caseId)
            ));
    }

    private ScenarioCase findActiveOwnedCase(Long caseId) {
        ScenarioCase scenarioCase = findActiveCase(caseId);
        Integer actorUserNo = authorizationService.getUserNo();
        if (!scenarioCase.getUserNo().equals(actorUserNo)) {
            throw AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model case not found. caseId: %d".formatted(caseId)
            );
        }
        return scenarioCase;
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

    private List<FundamentalModelCaseFileRes> uploadCaseFiles(
        String companyCd,
        Role role,
        Long caseId,
        List<MultipartFile> files,
        List<Long> uploadedFileNos
    ) {
        Integer fileRelationshipObjectDistinguishNo = Math.toIntExact(caseId);
        List<FundamentalModelCaseFileRes> uploadedFiles = new ArrayList<>();

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
            uploadedFiles.add(new FundamentalModelCaseFileRes(
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
                log.warn("Failed to compensate scenario case uploaded file. fileNo={}", fileNo, compensationException);
            }
        }
    }

    private String buildStatusEndpoint(Long caseId) {
        return "/api/v1/mi/fundamental-model/cases/%d/status".formatted(caseId);
    }

    private FundamentalModelCaseListItemRes toListItemResponse(ScenarioCaseListRow row) {
        return new FundamentalModelCaseListItemRes(
            row.caseId(),
            row.title(),
            row.status(),
            row.assigneeName(),
            row.createdAt(),
            row.updatedAt()
        );
    }

    private List<FundamentalModelCaseFileRes> findCaseFiles(Long caseId) {
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );

        List<FundamentalModelCaseFileRes> files = new ArrayList<>();
        for (int index = 0; index < uploadedFiles.size(); index++) {
            UploadedFile uploadedFile = uploadedFiles.get(index);
            files.add(new FundamentalModelCaseFileRes(
                uploadedFile.getFileNo(),
                index + 1,
                uploadedFile.getOriginalFileNm(),
                uploadedFile.getMimeTypeNm(),
                uploadedFile.getFileSize()
            ));
        }
        return files;
    }

    private FundamentalModelRunStatusRes findLatestRun(String lastRunId) {
        if (lastRunId == null) {
            return null;
        }

        return scenarioCaseRunHistoryRepository.findById(lastRunId)
            .map(this::toLatestRunResponse)
            .orElse(null);
    }

    private FundamentalModelRunStatusRes toLatestRunResponse(ScenarioCaseRunHistory runHistory) {
        return new FundamentalModelRunStatusRes(
            runHistory.getRunId(),
            runHistory.getStatus(),
            runHistory.getCreateDt(),
            runHistory.getExecutionBeginDt(),
            runHistory.getExecutionEndDt(),
            runHistory.getFailureReasonCode(),
            runHistory.getErrorMessage()
        );
    }
}
