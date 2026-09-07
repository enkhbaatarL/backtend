package com.sk.domain.mi.service.fundamentalmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FilterOption;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFileRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFilesRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListItemRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelDownloadFileRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelFiveScaleTruncatingNumberSerializer;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelPeriodFilterOptions;
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
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelVolumeUnit;
import com.sk.domain.shared.enums.fundamentalmodel.PriceSeriesType;
import com.sk.domain.shared.enums.fundamentalmodel.TradeCountryHeaderKind;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowSelector;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowUnit;
import com.sk.domain.shared.enums.fundamentalmodel.VolumesRowKind;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCasePriceRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseTradeFlowHeaderRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseTradeFlowRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseVolumeRegionBucketRepository;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowHeaderRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseVolumeRegionBucketRow;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class FundamentalModelCaseServiceImpl implements FundamentalModelCaseService {

    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final List<String> REQUIRED_INPUT_FILE_ROLE_KEYWORDS = List.of("DEMAND", "SUPPLY", "TRADE");
    private static final String REQUIRED_INPUT_FILE_ROLE_MESSAGE =
        "files는 파일명에 DEMAND, SUPPLY, TRADE 키워드를 각각 정확히 1개씩 포함해야 합니다.";
    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";
    private static final String FILE_RELATION_SCENARIO_CASE_HISTORY = "SCNRCASEH";
    private static final String CALLBACK_ACTOR = "SYSTEM";
    private static final String JOB_SUBMIT_FAILURE_REASON = "JOB_SUBMIT_FAILED";
    private static final String CALLBACK_PATH_TEMPLATE = "/api/v1/mi/fundamental-model/runs/%s/callback";
    private static final String FUNDAMENTAL_MODEL_CASE_NOT_FOUND_MESSAGE = "fundamental model case not found. caseId: %d";
    private static final String PERIOD_UNIT_YEAR = "Y";
    private static final String PERIOD_UNIT_MONTH = "M";
    private static final int VOLUMES_JSON_SCHEMA_VERSION = 1;
    private static final String VOLUMES_ATTR_TOTAL = "TOTAL";
    private static final int TRADE_FLOW_YEAR_MIN = 2023;
    private static final int TRADE_FLOW_YEAR_MAX = 2030;
    private static final String VOLUME_YEARLY_PERIOD_PATTERN = "\\d{4}";
    private static final String VOLUME_MONTHLY_PERIOD_PATTERN = "\\d{4}-(0[1-9]|1[0-2])";
    private static final String TRADE_FLOW_AXIS_FROM = "FROM";
    private static final String TRADE_FLOW_AXIS_TO = "TO";
    private static final String TRADE_FLOW_TOTAL_KEY = "TOTAL";
    private static final String TOTAL_LABEL = "Total";
    private static final List<FundamentalModelPriceRes.PriceColumn> ANNUAL_PRICE_COLUMNS = List.of(
        new FundamentalModelPriceRes.PriceColumn("year", "Year"),
        new FundamentalModelPriceRes.PriceColumn("priceRegion", "Price Region"),
        new FundamentalModelPriceRes.PriceColumn("baseCasePrice", "Base Case Price"),
        new FundamentalModelPriceRes.PriceColumn("userAdjustedPrice", "User Adjusted Price")
    );
    private static final List<FundamentalModelPriceRes.PriceColumn> MONTHLY_PRICE_COLUMNS = List.of(
        new FundamentalModelPriceRes.PriceColumn("year", "Year"),
        new FundamentalModelPriceRes.PriceColumn("month", "Month"),
        new FundamentalModelPriceRes.PriceColumn("priceRegion", "Price Region"),
        new FundamentalModelPriceRes.PriceColumn("baseCasePrice", "Base Case Price"),
        new FundamentalModelPriceRes.PriceColumn("userAdjustedPrice", "User Adjusted Price")
    );
    private static final DateTimeFormatter RUN_ID_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<FundamentalModelCaseStatus> RUN_CLAIMABLE_CASE_STATUSES = List.of(
        FundamentalModelCaseStatus.PENDING,
        FundamentalModelCaseStatus.COMPLETED,
        FundamentalModelCaseStatus.FAILED
    );

    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ScenarioCaseVolumeRegionBucketRepository scenarioCaseVolumeRegionBucketRepository;
    private final ScenarioCasePriceRepository scenarioCasePriceRepository;
    private final ScenarioCaseTradeFlowHeaderRepository scenarioCaseTradeFlowHeaderRepository;
    private final ScenarioCaseTradeFlowRepository scenarioCaseTradeFlowRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileService fileService;
    private final AuthorizationService authorizationService;
    private final KubernetesJobService kubernetesJobService;
    private final TransactionOperations transactionOperations;
    private final ObjectMapper objectMapper;

    @Value("${kubernetes.callback-base-url:}")
    private String callbackBaseUrl;

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
        String companyCd = authorizationService.getCompanyCdWithPriority();
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
        return getCaseDetailInternal(caseId);
    }

    private FundamentalModelCaseDetailRes getCaseDetailInternal(Long caseId) {
        ScenarioCaseDetailRow caseRow = scenarioCaseRepository.findDetailRow(caseId)
            .orElseThrow(() -> caseNotFound(caseId));

        List<FundamentalModelDownloadFileRes> inputFiles = findDownloadFiles(
            caseId,
            FILE_RELATION_SCENARIO_CASE
        );
        ScenarioCaseRunHistory latestRunHistory = findLatestRunHistory(caseRow.lastRunId()).orElse(null);
        List<FundamentalModelDownloadFileRes> outputFiles = findOutputFiles(latestRunHistory);
        FundamentalModelRunStatusRes latestRun = latestRunHistory == null ? null : toLatestRunResponse(latestRunHistory);

        return new FundamentalModelCaseDetailRes(
            caseRow.caseId(),
            caseRow.title(),
            caseRow.description(),
            caseRow.status(),
            caseRow.assigneeName(),
            caseRow.assigneeUserNo(),
            new FundamentalModelCaseFilesRes(inputFiles, outputFiles),
            latestRun,
            caseRow.createdAt(),
            caseRow.updatedAt()
        );
    }

    @Override
    @Transactional
    public FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request) {
        String normalizedTitle = validateAndNormalizeTitle(request == null ? null : request.title());
        String description = request.description();
        ScenarioCase scenarioCase = findActiveOwnedCase(caseId);

        scenarioCase.updateMetadata(
            normalizedTitle,
            description,
            authorizationService.getSubject()
        );
        scenarioCaseRepository.flush();

        return getCaseDetailInternal(caseId);
    }

    @Override
    @Transactional
    public void deleteCase(Long caseId) {
        ScenarioCase scenarioCase = findActiveOwnedCase(caseId);
        scenarioCase.softDelete(authorizationService.getSubject());
    }

    @Override
    @Transactional
    public FundamentalModelRunAcceptRes runCase(Long caseId) {
        String actorLoginId = authorizationService.getSubject();
        findActiveOwnedCase(caseId);
        LocalDateTime requestedAt = LocalDateTime.now();
        String runId = generateRunId(requestedAt);

        int claimCount = scenarioCaseRepository.claimRun(
            caseId,
            runId,
            FundamentalModelCaseStatus.RUNNING,
            RUN_CLAIMABLE_CASE_STATUSES,
            requestedAt,
            actorLoginId
        );
        if (claimCount == 0) {
            handleRunClaimFailure(caseId);
        }

        ScenarioCaseRunHistory runHistory = scenarioCaseRunHistoryRepository.saveAndFlush(
            ScenarioCaseRunHistory.createRunning(caseId, runId, actorLoginId)
        );
        Long runSeq = scenarioCaseRunHistoryRepository.findRunSeqByRunId(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model run_seq not found after insert. runId: %s".formatted(runId)
            ));
        LocalDateTime executionBeginDt = scenarioCaseRunHistoryRepository.findExecutionBeginDtByRunId(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model execution begin time not found after insert. runId: %s".formatted(runId)
            ));
        FundamentalModelJobArgs jobArgs = buildJobArgs(caseId, runId, runSeq);
        submitJobAfterCommit(caseId, runId, jobArgs);

        return new FundamentalModelRunAcceptRes(
            caseId,
            runId,
            FundamentalModelRunStatus.RUNNING,
            runHistory.getCreateDt(),
            executionBeginDt,
            buildStatusEndpoint(caseId)
        );
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
    @Transactional(readOnly = true)
    public FundamentalModelVolumesFilterOptionsRes getVolumesFilterOptions(Long caseId) {
        List<FilterOption> allRegions = Arrays.stream(FundamentalModelRegion.values())
            .map(r -> new FilterOption(r.getCode(), r.getLabel()))
            .toList();
        List<FilterOption> granularities = Arrays.stream(FundamentalModelGranularity.values())
            .map(g -> new FilterOption(g.getCode(), g.getLabel()))
            .toList();
        ScenarioCaseRunHistory latestRunHistory = caseId == null ? null : findCompletedLatestRunHistory(findCompletedCaseForResultQuery(caseId));

        VolumesSegmentFilterOptions global = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.GLOBAL,
            Arrays.stream(FundamentalModelDataType.values())
                .map(t -> new FilterOption(t.getCode(), t.getLabel()))
                .toList(),
            allRegions,
            granularities,
            findVolumePeriodOptions(latestRunHistory, FundamentalModelSegment.GLOBAL)
        );
        VolumesSegmentFilterOptions lng = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.LNG,
            List.of(
                new FilterOption(FundamentalModelDataType.LNG_IMPORTS.getCode(), FundamentalModelDataType.LNG_IMPORTS.getLabel()),
                new FilterOption(FundamentalModelDataType.LNG_EXPORTS.getCode(), FundamentalModelDataType.LNG_EXPORTS.getLabel())
            ),
            allRegions,
            granularities,
            findVolumePeriodOptions(latestRunHistory, FundamentalModelSegment.LNG)
        );
        VolumesSegmentFilterOptions trade = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.TRADE,
            Arrays.stream(TradeFlowDirection.values())
                .map(d -> new FilterOption(d.getCode(), d.getLabel()))
                .toList(),
            allRegions,
            List.of(),
            FundamentalModelPeriodFilterOptions.empty()
        );

        return new FundamentalModelVolumesFilterOptionsRes(List.of(global, lng, trade));
    }

    private FundamentalModelPeriodFilterOptions findVolumePeriodOptions(
        ScenarioCaseRunHistory latestRunHistory,
        FundamentalModelSegment segment
    ) {
        if (latestRunHistory == null) {
            return FundamentalModelPeriodFilterOptions.empty();
        }
        return new FundamentalModelPeriodFilterOptions(
            findVolumePeriodOptions(latestRunHistory.getRunSeq(), segment, FundamentalModelGranularity.YEARLY),
            findVolumePeriodOptions(latestRunHistory.getRunSeq(), segment, FundamentalModelGranularity.MONTHLY)
        );
    }

    private List<FilterOption> findVolumePeriodOptions(
        Long runSeq,
        FundamentalModelSegment segment,
        FundamentalModelGranularity granularity
    ) {
        String periodUnitSeparateCode = toPeriodUnitSeparateCode(granularity);
        List<ScenarioCaseVolumeRegionBucketRow> bucketRows =
            scenarioCaseVolumeRegionBucketRepository.findRowsByRunSeqAndFilters(
                runSeq,
                segment.getCode(),
                periodUnitSeparateCode,
                allowedVolumeTypes(segment).stream().map(FundamentalModelDataType::getCode).toList(),
                Arrays.stream(FundamentalModelRegion.values()).map(FundamentalModelRegion::getCode).toList()
            );
        if (bucketRows.isEmpty()) {
            return List.of();
        }
        Set<String> periodKeys = new HashSet<>();
        parseVolumeBuckets(segment, periodUnitSeparateCode, bucketRows).values().stream()
            .flatMap(bucket -> bucket.periodKeys().stream())
            .forEach(periodKeys::add);
        return periodKeys.stream()
            .sorted()
            .map(periodKey -> toPeriodFilterOption(granularity, periodKey))
            .toList();
    }

    private FilterOption toPeriodFilterOption(FundamentalModelGranularity granularity, String periodKey) {
        if (granularity == FundamentalModelGranularity.YEARLY) {
            return new FilterOption(periodKey, periodKey);
        }
        String value = toVolumeColumnKey(periodKey);
        String label = Month.of(Integer.parseInt(periodKey.substring(4, 6)))
            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return new FilterOption(value, label);
    }

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request) {
        validateVolumesRequest(request);
        FundamentalModelGranularity granularity = request.granularity() != null
            ? request.granularity()
            : FundamentalModelGranularity.MONTHLY;
        String periodUnitSeparateCode = toPeriodUnitSeparateCode(granularity);
        List<FundamentalModelDataType> appliedTypes = normalizeVolumeTypes(request.segment(), request.types());
        List<FundamentalModelRegion> appliedRegions = normalizeRegions(request.regions());
        List<String> appliedPeriods = normalizeVolumePeriods(granularity, request.periods());

        ScenarioCase scenarioCase = findCompletedCaseForResultQuery(caseId);

        ScenarioCaseRunHistory latestRunHistory = findCompletedLatestRunHistory(scenarioCase);
        List<ScenarioCaseVolumeRegionBucketRow> bucketRows =
            scenarioCaseVolumeRegionBucketRepository.findRowsByRunSeqAndFilters(
                latestRunHistory.getRunSeq(),
                request.segment().getCode(),
                periodUnitSeparateCode,
                appliedTypes.stream().map(FundamentalModelDataType::getCode).toList(),
                appliedRegions.stream().map(FundamentalModelRegion::getCode).toList()
            );
        if (bucketRows.isEmpty()) {
            return emptyVolumesResponse(caseId, request.segment(), granularity, appliedTypes, appliedRegions, appliedPeriods);
        }

        return buildVolumesResponse(
            caseId,
            request.segment(),
            granularity,
            appliedTypes,
            appliedRegions,
            appliedPeriods,
            parseVolumeBuckets(request.segment(), periodUnitSeparateCode, bucketRows)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelTradeFlowsRes getTradeFlows(Long caseId, FundamentalModelTradeFlowsReq request) {
        validateTradeFlowsRequest(request);
        TradeFlowUnit unit = request.unit() != null ? request.unit() : TradeFlowUnit.MTPA;
        List<TradeFlowDirection> appliedTypes = normalizeTradeFlowTypes(request.types());
        List<FundamentalModelRegion> appliedRegions = normalizeRegions(request.regions());

        ScenarioCase scenarioCase = findCompletedCaseForResultQuery(caseId);

        ScenarioCaseRunHistory latestRunHistory = findCompletedLatestRunHistory(scenarioCase);
        List<ScenarioCaseTradeFlowHeaderRow> headerRows =
            scenarioCaseTradeFlowHeaderRepository.findRowsByRunSeqAndSelectorAndYear(
                latestRunHistory.getRunSeq(),
                request.selector().getCode(),
                request.year()
            );
        if (headerRows.isEmpty()) {
            return emptyTradeFlowsResponse(caseId, request.selector(), request.year(), unit, appliedTypes, appliedRegions);
        }

        List<ScenarioCaseTradeFlowRow> flowRows =
            scenarioCaseTradeFlowRepository.findRowsByRunSeqAndSelectorAndYear(
                latestRunHistory.getRunSeq(),
                request.selector().getCode(),
                request.year()
            );

        TradeFlowHeaders headers = parseTradeFlowHeaders(headerRows);
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> filteredFrom =
            filterFromCountries(headers.fromCountries(), appliedTypes, appliedRegions);
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> filteredTo =
            filterToCountries(headers.toCountries(), appliedTypes, appliedRegions);
        List<FundamentalModelTradeFlowsRes.TradeFlowItem> filteredFlows =
            filterTradeFlows(toTradeFlowItems(flowRows), filteredFrom, filteredTo);

        return new FundamentalModelTradeFlowsRes(
            caseId,
            request.selector(),
            request.year(),
            unit,
            new FundamentalModelTradeFlowsRes.TradeFlowsAppliedFilters(appliedTypes, appliedRegions),
            appendFromTotalHeader(filteredFrom),
            filteredTo,
            appendFromTotalFlows(filteredTo, filteredFlows)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FundamentalModelPriceRes getPrice(Long caseId) {
        ScenarioCase scenarioCase = findCompletedCaseForResultQuery(caseId);

        ScenarioCaseRunHistory latestRunHistory = findCompletedLatestRunHistory(scenarioCase);
        List<ScenarioCasePriceRow> priceRows = scenarioCasePriceRepository.findRowsByRunSeq(latestRunHistory.getRunSeq());

        return buildPriceResponse(caseId, priceRows);
    }

    @Override
    @Transactional
    public FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        validateCallbackRequest(request);

        ScenarioCaseRunHistory runHistory = scenarioCaseRunHistoryRepository.findById(runId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "fundamental model run not found. runId: %s".formatted(runId)
            ));

        LocalDateTime processedAt = LocalDateTime.now();
        if (runHistory.getStatus() != FundamentalModelRunStatus.RUNNING) {
            if (isBackendForcedTerminal(runHistory)) {
                log.info(
                    "LATE_CALLBACK_IGNORED backend-forced terminal run. runId={}, failureReason={}",
                    runId,
                    runHistory.getFailureReasonCode()
                );
                return new FundamentalModelRunCallbackRes(runId, runHistory.getStatus(), processedAt);
            }
            if (isSameTerminalPayload(runHistory, request)) {
                return new FundamentalModelRunCallbackRes(runId, runHistory.getStatus(), processedAt);
            }
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "terminal run callback payload conflicts with existing result. runId: %s".formatted(runId)
            );
        }

        String failureReason = request.status() == FundamentalModelRunStatus.FAILED ? request.failureReason() : null;
        String errorMessage = request.status() == FundamentalModelRunStatus.FAILED ? request.errorMessage() : null;
        LocalDateTime finishedAtKst = DateUtil.utcToKst(request.finishedAt());
        int updateCount = scenarioCaseRunHistoryRepository.updateRunningToTerminal(
            runId,
            request.status(),
            FundamentalModelRunStatus.RUNNING,
            finishedAtKst,
            failureReason,
            errorMessage,
            processedAt,
            CALLBACK_ACTOR
        );
        if (updateCount == 0) {
            ScenarioCaseRunHistory refreshedRun = scenarioCaseRunHistoryRepository.findById(runId)
                .orElseThrow(() -> AppErrorException.of(
                    ResultCode.Error.RSC_NOT_FOUND,
                    "fundamental model run not found. runId: %s".formatted(runId)
                ));
            if (isBackendForcedTerminal(refreshedRun)) {
                log.info(
                    "LATE_CALLBACK_IGNORED backend-forced terminal run after concurrent transition. runId={}, failureReason={}",
                    runId,
                    refreshedRun.getFailureReasonCode()
                );
                return new FundamentalModelRunCallbackRes(runId, refreshedRun.getStatus(), processedAt);
            }
            if (isSameTerminalPayload(refreshedRun, request)) {
                return new FundamentalModelRunCallbackRes(runId, refreshedRun.getStatus(), processedAt);
            }
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "terminal run callback payload conflicts with existing result. runId: %s".formatted(runId)
            );
        }

        int caseUpdateCount = scenarioCaseRepository.updateLatestRunStatus(
            runId,
            FundamentalModelCaseStatus.valueOf(request.status().name()),
            processedAt,
            CALLBACK_ACTOR
        );
        if (caseUpdateCount == 0) {
            log.info("Callback did not update case because run is not the active latest run. runId={}", runId);
        }

        return new FundamentalModelRunCallbackRes(runId, request.status(), processedAt);
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

    private void handleRunClaimFailure(Long caseId) {
        ScenarioCase scenarioCase = scenarioCaseRepository.findById(caseId)
            .orElseThrow(() -> caseNotFound(caseId));
        if (Boolean.TRUE.equals(scenarioCase.getIsDeleted())) {
            throw caseNotFound(caseId);
        }
        if (scenarioCase.getStatus() == FundamentalModelCaseStatus.RUNNING) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model case is already running. caseId: %d".formatted(caseId)
            );
        }
        throw AppErrorException.of(
            ResultCode.Error.BIZ_RULE_VIOLATION,
            "fundamental model case cannot be run from current status. caseId: %d".formatted(caseId)
        );
    }

    private ScenarioCaseRunHistory findCompletedLatestRunHistory(ScenarioCase scenarioCase) {
        String lastRunId = scenarioCase.getLastRunId();
        if (lastRunId == null) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "completed fundamental model case has no lastRunId. caseId: %d".formatted(scenarioCase.getCaseId())
            );
        }

        ScenarioCaseRunHistory runHistory = findLatestRunHistory(lastRunId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model latest run not found. caseId: %d, runId: %s"
                    .formatted(scenarioCase.getCaseId(), lastRunId)
            ));
        if (!Objects.equals(runHistory.getCaseId(), scenarioCase.getCaseId())) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model latest run case mismatch. caseId: %d, runId: %s"
                    .formatted(scenarioCase.getCaseId(), lastRunId)
            );
        }
        if (runHistory.getStatus() != FundamentalModelRunStatus.COMPLETED) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model latest run is not completed. caseId: %d, runId: %s"
                    .formatted(scenarioCase.getCaseId(), lastRunId)
            );
        }
        if (runHistory.getRunSeq() == null) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "completed fundamental model run has no runSeq. caseId: %d, runId: %s"
                    .formatted(scenarioCase.getCaseId(), lastRunId)
            );
        }
        return runHistory;
    }

    private void validateTradeFlowsRequest(FundamentalModelTradeFlowsReq request) {
        if (request == null || request.selector() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "selector는 필수입니다.");
        }
        if (request.year() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "year는 필수입니다.");
        }
        if (request.year() < TRADE_FLOW_YEAR_MIN || request.year() > TRADE_FLOW_YEAR_MAX) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "year는 %d~%d 범위여야 합니다.".formatted(TRADE_FLOW_YEAR_MIN, TRADE_FLOW_YEAR_MAX)
            );
        }
    }

    private List<TradeFlowDirection> normalizeTradeFlowTypes(List<TradeFlowDirection> types) {
        if (types == null || types.isEmpty()) {
            return List.of(TradeFlowDirection.values());
        }
        return List.copyOf(types);
    }

    private List<FundamentalModelRegion> normalizeRegions(List<FundamentalModelRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return List.of(FundamentalModelRegion.values());
        }
        return List.copyOf(regions);
    }

    private List<String> normalizeVolumePeriods(FundamentalModelGranularity granularity, List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return List.of();
        }
        List<String> normalizedPeriods = new ArrayList<>();
        Set<String> seenPeriods = new HashSet<>();
        for (String period : periods) {
            String normalizedPeriod = normalizeVolumePeriod(granularity, period);
            if (seenPeriods.add(normalizedPeriod)) {
                normalizedPeriods.add(normalizedPeriod);
            }
        }
        return List.copyOf(normalizedPeriods);
    }

    private String normalizeVolumePeriod(FundamentalModelGranularity granularity, String period) {
        if (period == null || period.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "periods는 빈 값을 포함할 수 없습니다.");
        }
        String trimmedPeriod = period.trim();
        if (granularity == FundamentalModelGranularity.YEARLY) {
            if (!trimmedPeriod.matches(VOLUME_YEARLY_PERIOD_PATTERN)) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "YEARLY periods는 yyyy 형식이어야 합니다.");
            }
            return trimmedPeriod;
        }
        if (!trimmedPeriod.matches(VOLUME_MONTHLY_PERIOD_PATTERN)) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "MONTHLY periods는 yyyy-MM 형식이어야 합니다.");
        }
        return trimmedPeriod.substring(0, 4) + trimmedPeriod.substring(5, 7);
    }

    private void validateVolumesRequest(FundamentalModelVolumesReq request) {
        if (request == null || request.segment() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "segment는 필수입니다.");
        }
        if (request.segment() == FundamentalModelSegment.TRADE) {
            throw AppErrorException.of(
                ResultCode.Error.REQ_INVALID_PARAMETER,
                "segment=TRADE는 volumes API에서 허용되지 않습니다. trade flows API를 사용하세요."
            );
        }
    }

    private List<FundamentalModelDataType> normalizeVolumeTypes(
        FundamentalModelSegment segment,
        List<FundamentalModelDataType> types
    ) {
        List<FundamentalModelDataType> allowedTypes = allowedVolumeTypes(segment);
        if (types == null || types.isEmpty()) {
            return allowedTypes;
        }
        for (FundamentalModelDataType type : types) {
            if (!allowedTypes.contains(type)) {
                throw AppErrorException.of(
                    ResultCode.Error.REQ_INVALID_PARAMETER,
                    "segment=%s에서 허용되지 않는 volumes type입니다. type=%s".formatted(segment.getCode(), type.getCode())
                );
            }
        }
        return List.copyOf(types);
    }

    private List<FundamentalModelDataType> allowedVolumeTypes(FundamentalModelSegment segment) {
        if (segment == FundamentalModelSegment.LNG) {
            return List.of(FundamentalModelDataType.LNG_IMPORTS, FundamentalModelDataType.LNG_EXPORTS);
        }
        return List.of(FundamentalModelDataType.values());
    }

    private String toPeriodUnitSeparateCode(FundamentalModelGranularity granularity) {
        return granularity == FundamentalModelGranularity.YEARLY ? PERIOD_UNIT_YEAR : PERIOD_UNIT_MONTH;
    }

    private Map<VolumeBucketKey, ParsedVolumeBucket> parseVolumeBuckets(
        FundamentalModelSegment segment,
        String periodUnitSeparateCode,
        List<ScenarioCaseVolumeRegionBucketRow> bucketRows
    ) {
        Map<VolumeBucketKey, ParsedVolumeBucket> buckets = new LinkedHashMap<>();
        for (ScenarioCaseVolumeRegionBucketRow bucketRow : bucketRows) {
            FundamentalModelDataType dataType = parseVolumeDataType(bucketRow.dataTypeCode());
            FundamentalModelRegion region = parseVolumeRegion(bucketRow.regionCode());
            VolumeBucketKey bucketKey = new VolumeBucketKey(dataType, region);
            if (buckets.put(bucketKey, parseVolumeBucket(segment, periodUnitSeparateCode, dataType, region, bucketRow)) != null) {
                throw invalidVolumeData(
                    "fundamental model volume bucket is duplicated. type=%s, region=%s"
                        .formatted(dataType.getCode(), region.getCode())
                );
            }
        }
        return buckets;
    }

    private ParsedVolumeBucket parseVolumeBucket(
        FundamentalModelSegment segment,
        String periodUnitSeparateCode,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region,
        ScenarioCaseVolumeRegionBucketRow bucketRow
    ) {
        if (!Objects.equals(bucketRow.jsonSchemaVersion(), VOLUMES_JSON_SCHEMA_VERSION)) {
            throw invalidVolumeData(
                "unsupported fundamental model volume json schema version. version=%s".formatted(bucketRow.jsonSchemaVersion())
            );
        }

        JsonNode root = readVolumeJson(bucketRow.volumeDataJson(), dataType, region);
        validateVolumeBucketHeader(root, segment, periodUnitSeparateCode, dataType, region);

        List<String> periodKeys = parseVolumePeriodKeys(root.path("periodKeys"), periodUnitSeparateCode, dataType, region);
        JsonNode rowsNode = root.path("rows");
        if (!rowsNode.isArray()) {
            throw invalidVolumeData("fundamental model volume bucket rows must be an array.");
        }
        List<ParsedVolumeRow> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowsNode.size(); rowIndex++) {
            ParsedVolumeRow parsedRow = parseVolumeRow(periodKeys, dataType, region, rowsNode.get(rowIndex));
            if (parsedRow != null) {
                rows.add(parsedRow);
            }
        }

        if (rows.isEmpty() || rows.get(0).rowKind() != VolumesRowKind.REGION_SUBTOTAL) {
            throw invalidVolumeData(
                "fundamental model volume bucket first row must be REGION_SUBTOTAL. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).rowKind() == VolumesRowKind.REGION_SUBTOTAL) {
                throw invalidVolumeData(
                    "fundamental model volume bucket must not include REGION_SUBTOTAL after first row. type=%s, region=%s"
                        .formatted(dataType.getCode(), region.getCode())
                );
            }
        }
        return new ParsedVolumeBucket(periodKeys, rows);
    }

    private JsonNode readVolumeJson(String volumeDataJson, FundamentalModelDataType dataType, FundamentalModelRegion region) {
        if (volumeDataJson == null || volumeDataJson.isBlank()) {
            throw invalidVolumeData(
                "fundamental model volume bucket json is blank. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }
        try {
            return objectMapper.readTree(volumeDataJson);
        } catch (Exception e) {
            throw invalidVolumeData(
                "fundamental model volume bucket json is invalid. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }
    }

    private void validateVolumeBucketHeader(
        JsonNode root,
        FundamentalModelSegment segment,
        String periodUnitSeparateCode,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region
    ) {
        int schemaVersion = root.path("schemaVersion").asInt(-1);
        if (schemaVersion != VOLUMES_JSON_SCHEMA_VERSION
            || !segment.getCode().equals(root.path("segment").asText(null))
            || !periodUnitSeparateCode.equals(root.path("periodUnit").asText(null))
            || !dataType.getCode().equals(root.path("dataType").asText(null))
            || !region.getCode().equals(root.path("region").asText(null))) {
            throw invalidVolumeData(
                "fundamental model volume bucket header mismatch. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }
    }

    private List<String> parseVolumePeriodKeys(
        JsonNode periodKeysNode,
        String periodUnitSeparateCode,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region
    ) {
        if (!periodKeysNode.isArray() || periodKeysNode.isEmpty()) {
            throw invalidVolumeData(
                "fundamental model volume bucket periodKeys is empty. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }

        List<String> periodKeys = new ArrayList<>();
        String previousPeriodKey = null;
        for (JsonNode periodKeyNode : periodKeysNode) {
            String periodKey = periodKeyNode.asText(null);
            validateVolumePeriodKey(periodUnitSeparateCode, periodKey, dataType, region);
            if (previousPeriodKey != null && previousPeriodKey.compareTo(periodKey) >= 0) {
                throw invalidVolumeData(
                    "fundamental model volume bucket periodKeys must be sorted ascending. type=%s, region=%s"
                        .formatted(dataType.getCode(), region.getCode())
                );
            }
            periodKeys.add(periodKey);
            previousPeriodKey = periodKey;
        }
        return List.copyOf(periodKeys);
    }

    private void validateVolumePeriodKey(
        String periodUnitSeparateCode,
        String periodKey,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region
    ) {
        if (PERIOD_UNIT_YEAR.equals(periodUnitSeparateCode)) {
            if (periodKey == null || !periodKey.matches("\\d{4}")) {
                throw invalidVolumePeriod(periodUnitSeparateCode, periodKey, dataType, region);
            }
            return;
        }
        if (PERIOD_UNIT_MONTH.equals(periodUnitSeparateCode)) {
            if (periodKey == null || !periodKey.matches("\\d{6}")) {
                throw invalidVolumePeriod(periodUnitSeparateCode, periodKey, dataType, region);
            }
            int month = Integer.parseInt(periodKey.substring(4, 6));
            if (month < 1 || month > 12) {
                throw invalidVolumePeriod(periodUnitSeparateCode, periodKey, dataType, region);
            }
            return;
        }
        throw invalidVolumePeriod(periodUnitSeparateCode, periodKey, dataType, region);
    }

    private AppErrorException invalidVolumePeriod(
        String periodUnitSeparateCode,
        String periodKey,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region
    ) {
        return invalidVolumeData(
            "fundamental model volume bucket has invalid period. periodUnit=%s, periodKey=%s, type=%s, region=%s"
                .formatted(periodUnitSeparateCode, periodKey, dataType.getCode(), region.getCode())
        );
    }

    private ParsedVolumeRow parseVolumeRow(
        List<String> periodKeys,
        FundamentalModelDataType dataType,
        FundamentalModelRegion region,
        JsonNode rowNode
    ) {
        // 현재 API는 TOTAL attrType만 노출한다. 향후 추가될 분해(breakdown) row는 건너뛴다(null 반환).
        String attrType = rowNode.path("attrType").asText(null);
        if (!VOLUMES_ATTR_TOTAL.equals(attrType)) {
            return null;
        }

        String country = rowNode.path("country").isNull() ? null : rowNode.path("country").asText(null);
        VolumesRowKind rowKind = country == null ? VolumesRowKind.REGION_SUBTOTAL : VolumesRowKind.COUNTRY;
        if (country != null && country.isBlank()) {
            throw invalidVolumeData(
                "fundamental model volume country row has blank country. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }

        JsonNode valuesNode = rowNode.path("values");
        if (!valuesNode.isArray() || valuesNode.size() != periodKeys.size()) {
            throw invalidVolumeData(
                "fundamental model volume row values length mismatch. type=%s, region=%s"
                    .formatted(dataType.getCode(), region.getCode())
            );
        }

        Map<String, Double> values = new LinkedHashMap<>();
        for (int i = 0; i < periodKeys.size(); i++) {
            String periodKey = periodKeys.get(i);
            JsonNode valueNode = valuesNode.get(i);
            if (valueNode == null || !valueNode.isNumber()) {
                throw invalidVolumeData(
                    "fundamental model volume row has missing or non-numeric value. type=%s, region=%s, periodKey=%s"
                        .formatted(dataType.getCode(), region.getCode(), periodKey)
                );
            }
            values.put(periodKey, FundamentalModelFiveScaleTruncatingNumberSerializer.truncate(valueNode.decimalValue()));
        }

        return new ParsedVolumeRow(
            rowKind,
            country,
            values
        );
    }

    private FundamentalModelVolumesRes buildVolumesResponse(
        Long caseId,
        FundamentalModelSegment segment,
        FundamentalModelGranularity granularity,
        List<FundamentalModelDataType> appliedTypes,
        List<FundamentalModelRegion> appliedRegions,
        List<String> appliedPeriods,
        Map<VolumeBucketKey, ParsedVolumeBucket> buckets
    ) {
        List<String> responsePeriodKeys = null;
        List<FundamentalModelVolumesRes.VolumesRow> responseRows = new ArrayList<>();

        for (FundamentalModelDataType type : appliedTypes) {
            responsePeriodKeys = appendTypeVolumeRows(
                type,
                appliedRegions,
                appliedPeriods,
                buckets,
                responsePeriodKeys,
                responseRows
            );
        }

        if (responsePeriodKeys == null) {
            return emptyVolumesResponse(caseId, segment, granularity, appliedTypes, appliedRegions, appliedPeriods);
        }

        return new FundamentalModelVolumesRes(
            caseId,
            segment,
            granularity,
            FundamentalModelVolumeUnit.MTPA,
            new FundamentalModelVolumesRes.VolumesAppliedFilters(appliedTypes, appliedRegions, toResponsePeriods(granularity, appliedPeriods)),
            toVolumeColumns(granularity, responsePeriodKeys),
            List.copyOf(responseRows)
        );
    }

    private List<String> appendTypeVolumeRows(
        FundamentalModelDataType type,
        List<FundamentalModelRegion> appliedRegions,
        List<String> appliedPeriods,
        Map<VolumeBucketKey, ParsedVolumeBucket> buckets,
        List<String> responsePeriodKeys,
        List<FundamentalModelVolumesRes.VolumesRow> responseRows
    ) {
        Map<String, Double> typeTotals = null;
        for (FundamentalModelRegion region : appliedRegions) {
            ParsedVolumeBucket bucket = buckets.get(new VolumeBucketKey(type, region));
            if (bucket == null) {
                continue;
            }
            List<String> bucketPeriodKeys = filterVolumePeriodKeys(bucket.periodKeys(), appliedPeriods);
            if (bucketPeriodKeys.isEmpty()) {
                continue;
            }
            responsePeriodKeys = resolveResponsePeriodKeys(responsePeriodKeys, bucketPeriodKeys, type, region);
            if (typeTotals == null) {
                typeTotals = zeroValues(responsePeriodKeys);
            }
            // 26.07.29 취약점 진단 검토, '누락된 널 값 검사-102234', 오탐 -> typeTotals는 직전 분기에서 zeroValues(신규 맵 반환)로 초기화되어 not null 보장됨.
            appendVolumeBucketRows(type, region, bucket, responsePeriodKeys, typeTotals, responseRows);
        }
        if (typeTotals == null) {
            return responsePeriodKeys;
        }
        responseRows.add(new FundamentalModelVolumesRes.VolumesRow(
            type,
            type.getLabel(),
            null,
            null,
            VolumesRowKind.TYPE_TOTAL,
            TOTAL_LABEL,
            // 26.07.29 취약점 진단 검토, '누락된 널 값 검사-102233', 오탐 -> typeTotals는 위 null 검사 후 반환 분기로 not null 보장됨.
            toResponseValues(typeTotals, responsePeriodKeys)
        ));
        return responsePeriodKeys;
    }

    private List<String> resolveResponsePeriodKeys(
        List<String> responsePeriodKeys,
        List<String> bucketPeriodKeys,
        FundamentalModelDataType type,
        FundamentalModelRegion region
    ) {
        if (responsePeriodKeys == null) {
            return bucketPeriodKeys;
        }
        if (!responsePeriodKeys.equals(bucketPeriodKeys)) {
            throw invalidVolumeData(
                "fundamental model volume bucket periodKeys mismatch. type=%s, region=%s"
                    .formatted(type.getCode(), region.getCode())
            );
        }
        return responsePeriodKeys;
    }

    private List<String> filterVolumePeriodKeys(List<String> periodKeys, List<String> appliedPeriods) {
        if (appliedPeriods.isEmpty()) {
            return periodKeys;
        }
        Set<String> appliedPeriodSet = new HashSet<>(appliedPeriods);
        return periodKeys.stream()
            .filter(appliedPeriodSet::contains)
            .toList();
    }

    private void appendVolumeBucketRows(
        FundamentalModelDataType type,
        FundamentalModelRegion region,
        ParsedVolumeBucket bucket,
        List<String> responsePeriodKeys,
        Map<String, Double> typeTotals,
        List<FundamentalModelVolumesRes.VolumesRow> responseRows
    ) {
        ParsedVolumeRow subtotalRow = bucket.rows().stream()
            .filter(row -> row.rowKind() == VolumesRowKind.REGION_SUBTOTAL)
            .findFirst()
            .orElseThrow(() -> invalidVolumeData(
                "fundamental model volume bucket has no REGION_SUBTOTAL row. type=%s, region=%s"
                    .formatted(type.getCode(), region.getCode())
            ));

        responseRows.add(toVolumeResponseRow(type, region, subtotalRow, responsePeriodKeys));
        addValues(typeTotals, subtotalRow.values());
        bucket.rows().stream()
            .filter(row -> row.rowKind() == VolumesRowKind.COUNTRY)
            .map(row -> toVolumeResponseRow(type, region, row, responsePeriodKeys))
            .forEach(responseRows::add);
    }

    private FundamentalModelVolumesRes emptyVolumesResponse(
        Long caseId,
        FundamentalModelSegment segment,
        FundamentalModelGranularity granularity,
        List<FundamentalModelDataType> appliedTypes,
        List<FundamentalModelRegion> appliedRegions,
        List<String> appliedPeriods
    ) {
        return new FundamentalModelVolumesRes(
            caseId,
            segment,
            granularity,
            FundamentalModelVolumeUnit.MTPA,
            new FundamentalModelVolumesRes.VolumesAppliedFilters(appliedTypes, appliedRegions, toResponsePeriods(granularity, appliedPeriods)),
            List.of(),
            List.of()
        );
    }

    private List<String> toResponsePeriods(FundamentalModelGranularity granularity, List<String> periodKeys) {
        if (granularity == FundamentalModelGranularity.YEARLY) {
            return periodKeys;
        }
        return periodKeys.stream()
            .map(this::toVolumeColumnKey)
            .toList();
    }

    private Map<String, Double> zeroValues(List<String> periodKeys) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (String periodKey : periodKeys) {
            values.put(periodKey, 0.0);
        }
        return values;
    }

    private void addValues(Map<String, Double> totals, Map<String, Double> values) {
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            totals.compute(
                entry.getKey(),
                (ignored, value) -> FundamentalModelFiveScaleTruncatingNumberSerializer.truncate(
                    BigDecimal.valueOf(value == null ? 0.0 : value).add(BigDecimal.valueOf(entry.getValue()))
                )
            );
        }
    }

    private FundamentalModelVolumesRes.VolumesRow toVolumeResponseRow(
        FundamentalModelDataType type,
        FundamentalModelRegion region,
        ParsedVolumeRow row,
        List<String> periodKeys
    ) {
        String rowLabel = row.rowKind() == VolumesRowKind.REGION_SUBTOTAL ? region.getLabel() : row.country();
        return new FundamentalModelVolumesRes.VolumesRow(
            type,
            type.getLabel(),
            region,
            row.country(),
            row.rowKind(),
            rowLabel,
            toResponseValues(row.values(), periodKeys)
        );
    }

    private Map<String, Double> toResponseValues(Map<String, Double> values, List<String> periodKeys) {
        Map<String, Double> responseValues = new LinkedHashMap<>();
        for (String periodKey : periodKeys) {
            responseValues.put(toVolumeColumnKey(periodKey), values.get(periodKey));
        }
        return responseValues;
    }

    private List<FundamentalModelVolumesRes.VolumesColumn> toVolumeColumns(
        FundamentalModelGranularity granularity,
        List<String> periodKeys
    ) {
        return periodKeys.stream()
            .map(periodKey -> {
                if (granularity == FundamentalModelGranularity.YEARLY) {
                    return new FundamentalModelVolumesRes.VolumesColumn(
                        periodKey,
                        periodKey,
                        Integer.parseInt(periodKey),
                        null
                    );
                }
                int year = Integer.parseInt(periodKey.substring(0, 4));
                int month = Integer.parseInt(periodKey.substring(4, 6));
                return new FundamentalModelVolumesRes.VolumesColumn(
                    toVolumeColumnKey(periodKey),
                    periodKey.substring(2, 4) + "-" + periodKey.substring(4, 6),
                    year,
                    month
                );
            })
            .toList();
    }

    private String toVolumeColumnKey(String periodKey) {
        if (periodKey.length() == 6) {
            return periodKey.substring(0, 4) + "-" + periodKey.substring(4, 6);
        }
        return periodKey;
    }

    private FundamentalModelDataType parseVolumeDataType(String dataTypeCode) {
        if (dataTypeCode == null || dataTypeCode.isBlank()) {
            throw invalidVolumeData("fundamental model volume bucket has blank data type.");
        }
        try {
            return FundamentalModelDataType.from(dataTypeCode);
        } catch (IllegalArgumentException e) {
            throw invalidVolumeData("fundamental model volume bucket has invalid data type. type=%s".formatted(dataTypeCode));
        }
    }

    private FundamentalModelRegion parseVolumeRegion(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            throw invalidVolumeData("fundamental model volume bucket has blank region.");
        }
        try {
            return FundamentalModelRegion.from(regionCode);
        } catch (IllegalArgumentException e) {
            throw invalidVolumeData("fundamental model volume bucket has invalid region. region=%s".formatted(regionCode));
        }
    }

    private AppErrorException invalidVolumeData(String message) {
        return AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, message);
    }

    private TradeFlowHeaders parseTradeFlowHeaders(List<ScenarioCaseTradeFlowHeaderRow> headerRows) {
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> fromCountries = new ArrayList<>();
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> toCountries = new ArrayList<>();

        for (ScenarioCaseTradeFlowHeaderRow row : headerRows) {
            FundamentalModelTradeFlowsRes.TradeCountryHeader header = toTradeCountryHeader(row);
            if (TRADE_FLOW_AXIS_FROM.equals(row.axisCode())) {
                if (header.kind() != TradeCountryHeaderKind.GRAND_TOTAL) {
                    fromCountries.add(header);
                }
            } else if (TRADE_FLOW_AXIS_TO.equals(row.axisCode())) {
                toCountries.add(header);
            } else {
                throw invalidTradeFlowData("fundamental model trade flow header has invalid axis. axis=%s".formatted(row.axisCode()));
            }
        }

        if (fromCountries.isEmpty() || toCountries.isEmpty()) {
            throw invalidTradeFlowData("fundamental model trade flow header rows must include FROM and TO axes.");
        }
        return new TradeFlowHeaders(fromCountries, toCountries);
    }

    private FundamentalModelTradeFlowsRes.TradeCountryHeader toTradeCountryHeader(ScenarioCaseTradeFlowHeaderRow row) {
        return new FundamentalModelTradeFlowsRes.TradeCountryHeader(
            row.headerKey(),
            row.headerLabelName(),
            parseTradeCountryHeaderKind(row.headerKindCode()),
            parseFundamentalModelRegion(row.regionCode()),
            row.nationName()
        );
    }

    private List<FundamentalModelTradeFlowsRes.TradeFlowItem> toTradeFlowItems(List<ScenarioCaseTradeFlowRow> flowRows) {
        return flowRows.stream()
            .map(row -> {
                if (row.flowValue() == null) {
                    throw invalidTradeFlowData(
                        "fundamental model trade flow row has null flow_val. fromKey=%s, toKey=%s"
                            .formatted(row.fromHeaderKey(), row.toHeaderKey())
                    );
                }
                return new FundamentalModelTradeFlowsRes.TradeFlowItem(
                    row.fromHeaderKey(),
                    row.toHeaderKey(),
                    parseFundamentalModelRegion(row.fromRegionCode()),
                    row.fromNationName(),
                    parseFundamentalModelRegion(row.toRegionCode()),
                    row.toNationName(),
                    FundamentalModelFiveScaleTruncatingNumberSerializer.truncate(row.flowValue())
                );
            })
            .toList();
    }

    private FundamentalModelRegion parseFundamentalModelRegion(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return null;
        }
        try {
            return FundamentalModelRegion.from(regionCode);
        } catch (IllegalArgumentException e) {
            throw invalidTradeFlowData("fundamental model trade flow row has invalid region. region=%s".formatted(regionCode));
        }
    }

    private TradeCountryHeaderKind parseTradeCountryHeaderKind(String headerKindCode) {
        if (headerKindCode == null || headerKindCode.isBlank()) {
            throw invalidTradeFlowData("fundamental model trade flow header has blank kind.");
        }
        try {
            return TradeCountryHeaderKind.from(headerKindCode);
        } catch (IllegalArgumentException e) {
            throw invalidTradeFlowData("fundamental model trade flow header has invalid kind. kind=%s".formatted(headerKindCode));
        }
    }

    private List<FundamentalModelTradeFlowsRes.TradeCountryHeader> filterFromCountries(
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> fromCountries,
        List<TradeFlowDirection> types,
        List<FundamentalModelRegion> regions
    ) {
        if (!types.contains(TradeFlowDirection.FROM_COUNTRY) || isAllRegions(regions)) {
            return fromCountries;
        }
        return fromCountries.stream()
            .filter(header -> header.region() != null && regions.contains(header.region()))
            .toList();
    }

    private List<FundamentalModelTradeFlowsRes.TradeCountryHeader> filterToCountries(
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> toCountries,
        List<TradeFlowDirection> types,
        List<FundamentalModelRegion> regions
    ) {
        if (!types.contains(TradeFlowDirection.TO_COUNTRY) || isAllRegions(regions)) {
            return toCountries;
        }
        return toCountries.stream()
            .filter(header -> header.region() == null || regions.contains(header.region()))
            .toList();
    }

    private boolean isAllRegions(List<FundamentalModelRegion> regions) {
        return regions.size() == FundamentalModelRegion.values().length;
    }

    private List<FundamentalModelTradeFlowsRes.TradeFlowItem> filterTradeFlows(
        List<FundamentalModelTradeFlowsRes.TradeFlowItem> flows,
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> fromCountries,
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> toCountries
    ) {
        Set<String> validFromKeys = toHeaderKeySet(fromCountries);
        Set<String> validToKeys = toHeaderKeySet(toCountries);
        return flows.stream()
            .filter(flow -> validFromKeys.contains(flow.fromKey())
                && validToKeys.contains(flow.toKey()))
            .toList();
    }

    private Set<String> toHeaderKeySet(List<FundamentalModelTradeFlowsRes.TradeCountryHeader> headers) {
        Set<String> keys = new HashSet<>();
        for (FundamentalModelTradeFlowsRes.TradeCountryHeader header : headers) {
            keys.add(header.key());
        }
        return keys;
    }

    private List<FundamentalModelTradeFlowsRes.TradeCountryHeader> appendFromTotalHeader(
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> fromCountries
    ) {
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> result = new ArrayList<>(fromCountries);
        result.add(new FundamentalModelTradeFlowsRes.TradeCountryHeader(
            TRADE_FLOW_TOTAL_KEY,
            TOTAL_LABEL,
            TradeCountryHeaderKind.GRAND_TOTAL,
            null,
            TOTAL_LABEL
        ));
        return List.copyOf(result);
    }

    private List<FundamentalModelTradeFlowsRes.TradeFlowItem> appendFromTotalFlows(
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> toCountries,
        List<FundamentalModelTradeFlowsRes.TradeFlowItem> flows
    ) {
        List<FundamentalModelTradeFlowsRes.TradeFlowItem> totalFlows = new ArrayList<>(flows);
        for (FundamentalModelTradeFlowsRes.TradeCountryHeader toHeader : toCountries) {
            BigDecimal rowTotal = flows.stream()
                .filter(flow -> toHeader.key().equals(flow.toKey()))
                .map(flow -> BigDecimal.valueOf(flow.value() != null ? flow.value() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (rowTotal.signum() != 0) {
                totalFlows.add(new FundamentalModelTradeFlowsRes.TradeFlowItem(
                    TRADE_FLOW_TOTAL_KEY,
                    toHeader.key(),
                    null,
                    TOTAL_LABEL,
                    toHeader.region(),
                    toHeader.country(),
                    FundamentalModelFiveScaleTruncatingNumberSerializer.truncate(rowTotal)
                ));
            }
        }
        return List.copyOf(totalFlows);
    }

    private FundamentalModelTradeFlowsRes emptyTradeFlowsResponse(
        Long caseId,
        TradeFlowSelector selector,
        Integer year,
        TradeFlowUnit unit,
        List<TradeFlowDirection> appliedTypes,
        List<FundamentalModelRegion> appliedRegions
    ) {
        return new FundamentalModelTradeFlowsRes(
            caseId,
            selector,
            year,
            unit,
            new FundamentalModelTradeFlowsRes.TradeFlowsAppliedFilters(appliedTypes, appliedRegions),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private AppErrorException invalidTradeFlowData(String message) {
        return AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, message);
    }

    private FundamentalModelPriceRes buildPriceResponse(Long caseId, List<ScenarioCasePriceRow> priceRows) {
        List<ParsedPriceRow> annualRows = new ArrayList<>();
        List<ParsedPriceRow> monthlyRows = new ArrayList<>();
        for (ScenarioCasePriceRow priceRow : priceRows) {
            ParsedPriceRow parsedRow = parsePriceRow(priceRow);
            if (PERIOD_UNIT_YEAR.equals(parsedRow.periodUnitSeparateCode())) {
                annualRows.add(parsedRow);
            } else {
                monthlyRows.add(parsedRow);
            }
        }
        LocalDate chartBaseDate = LocalDate.now();

        return new FundamentalModelPriceRes(
            caseId,
            FundamentalModelPriceRes.FIXED_UNIT,
            new FundamentalModelPriceRes.PriceChart(
                toPriceSeriesSets(filterMonthlyPriceChartRows(monthlyRows, YearMonth.from(chartBaseDate))),
                toPriceSeriesSets(filterYearlyPriceChartRows(annualRows, chartBaseDate.getYear()))
            ),
            new FundamentalModelPriceRes.PriceTable(
                ANNUAL_PRICE_COLUMNS,
                toPriceTableRows(annualRows)
            ),
            new FundamentalModelPriceRes.PriceTable(
                MONTHLY_PRICE_COLUMNS,
                toPriceTableRows(monthlyRows)
            )
        );
    }

    private List<ParsedPriceRow> filterMonthlyPriceChartRows(List<ParsedPriceRow> priceRows, YearMonth baseMonth) {
        YearMonth chartStartMonth = baseMonth.minusMonths(6);
        return priceRows.stream()
            .filter(row -> !YearMonth.of(row.year(), row.month()).isBefore(chartStartMonth))
            .toList();
    }

    private List<ParsedPriceRow> filterYearlyPriceChartRows(List<ParsedPriceRow> priceRows, int baseYear) {
        int chartStartYear = baseYear - 1;
        return priceRows.stream()
            .filter(row -> row.year() >= chartStartYear)
            .toList();
    }

    private ParsedPriceRow parsePriceRow(ScenarioCasePriceRow priceRow) {
        if (priceRow.priceRegionName() == null || priceRow.priceRegionName().isBlank()) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model price row has invalid price region."
            );
        }
        double baseCasePrice = toDoublePrice(priceRow.baseCasePrice(), "base_case_prc", priceRow);
        double userAdjustedPrice = toDoublePrice(priceRow.userAdjustedPrice(), "user_adj_prc", priceRow);
        String periodUnit = priceRow.periodUnitSeparateCode();
        String periodKey = priceRow.periodKey();

        if (PERIOD_UNIT_YEAR.equals(periodUnit)) {
            if (periodKey == null || !periodKey.matches("\\d{4}")) {
                throw invalidPricePeriod(priceRow);
            }
            return new ParsedPriceRow(
                periodUnit,
                Integer.parseInt(periodKey),
                null,
                priceRow.priceRegionName(),
                baseCasePrice,
                userAdjustedPrice
            );
        }
        if (PERIOD_UNIT_MONTH.equals(periodUnit)) {
            if (periodKey == null || !periodKey.matches("\\d{6}")) {
                throw invalidPricePeriod(priceRow);
            }
            int month = Integer.parseInt(periodKey.substring(4, 6));
            if (month < 1 || month > 12) {
                throw invalidPricePeriod(priceRow);
            }
            return new ParsedPriceRow(
                periodUnit,
                Integer.parseInt(periodKey.substring(0, 4)),
                month,
                priceRow.priceRegionName(),
                baseCasePrice,
                userAdjustedPrice
            );
        }

        throw invalidPricePeriod(priceRow);
    }

    private double toDoublePrice(Double value, String columnName, ScenarioCasePriceRow priceRow) {
        if (value == null) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model price row has null %s. periodUnit=%s, periodKey=%s, priceRegion=%s"
                    .formatted(columnName, priceRow.periodUnitSeparateCode(), priceRow.periodKey(), priceRow.priceRegionName())
            );
        }
        return value;
    }

    private AppErrorException invalidPricePeriod(ScenarioCasePriceRow priceRow) {
        return AppErrorException.of(
            ResultCode.Error.BIZ_RULE_VIOLATION,
            "fundamental model price row has invalid period. periodUnit=%s, periodKey=%s, priceRegion=%s"
                .formatted(priceRow.periodUnitSeparateCode(), priceRow.periodKey(), priceRow.priceRegionName())
        );
    }

    private List<FundamentalModelPriceRes.PriceRow> toPriceTableRows(List<ParsedPriceRow> priceRows) {
        return priceRows.stream()
            .map(row -> new FundamentalModelPriceRes.PriceRow(
                row.year(),
                row.month(),
                row.priceRegion(),
                row.baseCasePrice(),
                row.userAdjustedPrice()
            ))
            .toList();
    }

    private List<FundamentalModelPriceRes.PriceChartSeriesSet> toPriceSeriesSets(List<ParsedPriceRow> priceRows) {
        Map<String, List<ParsedPriceRow>> rowsByRegion = new LinkedHashMap<>();
        for (ParsedPriceRow row : priceRows) {
            rowsByRegion.computeIfAbsent(row.priceRegion(), ignored -> new ArrayList<>()).add(row);
        }

        return rowsByRegion.entrySet().stream()
            .map(entry -> new FundamentalModelPriceRes.PriceChartSeriesSet(
                entry.getKey(),
                List.of(
                    toPriceChartSeries(PriceSeriesType.BASE_CASE, entry.getValue(), true),
                    toPriceChartSeries(PriceSeriesType.USER_ADJUSTED, entry.getValue(), false)
                )
            ))
            .toList();
    }

    private FundamentalModelPriceRes.PriceChartSeries toPriceChartSeries(
        PriceSeriesType seriesType,
        List<ParsedPriceRow> priceRows,
        boolean baseCase
    ) {
        return new FundamentalModelPriceRes.PriceChartSeries(
            seriesType,
            seriesType.getLabel(),
            priceRows.stream()
                .map(row -> new FundamentalModelPriceRes.PriceChartPoint(
                    row.year(),
                    row.month(),
                    baseCase ? row.baseCasePrice() : row.userAdjustedPrice()
                ))
                .toList()
        );
    }

    private String generateRunId(LocalDateTime requestedAt) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return RUN_ID_TIMESTAMP_FORMATTER.format(requestedAt) + "_" + suffix;
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
                        log.warn("Kubernetes job creation failed: runId={}, caseId={}", runId, caseId, ex);
                        markRunFailed(runId, caseId, rootCauseMessage(ex));
                    }
                });
        } catch (RuntimeException e) {
            log.warn("Kubernetes job creation failed: runId={}, caseId={}", runId, caseId, e);
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
                log.info("Skip marking non-running fundamental model run as failed. runId={}, caseId={}", runId, caseId);
            }
        });
    }

    private String rootCauseMessage(Throwable throwable) {
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

    private void validateCallbackRequest(FundamentalModelRunCallbackReq request) {
        if (request == null || request.status() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback status는 필수입니다.");
        }
        if (!request.status().isTerminal()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback status는 COMPLETED 또는 FAILED 여야 합니다.");
        }
        if (request.finishedAt() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "finishedAt은 필수입니다.");
        }
    }

    private boolean isSameTerminalPayload(ScenarioCaseRunHistory runHistory, FundamentalModelRunCallbackReq request) {
        String requestFailureReason = request.status() == FundamentalModelRunStatus.FAILED ? request.failureReason() : null;
        String requestErrorMessage = request.status() == FundamentalModelRunStatus.FAILED ? request.errorMessage() : null;
        return runHistory.getStatus() == request.status()
            && Objects.equals(runHistory.getFailureReasonCode(), requestFailureReason)
            && Objects.equals(runHistory.getErrorMessage(), requestErrorMessage);
    }

    private boolean isBackendForcedTerminal(ScenarioCaseRunHistory runHistory) {
        return runHistory.getStatus() == FundamentalModelRunStatus.FAILED
            && ("TIMEOUT".equals(runHistory.getFailureReasonCode())
                || "SCENARIO_CANCELLED".equals(runHistory.getFailureReasonCode()));
    }

    private ScenarioCase findActiveCase(Long caseId) {
        // fundamental-model 엔드포인트는 독립 실행(INDEPENDENT)만 다룬다.
        // SCENARIO / MI_BASE 케이스는 active resource hiding 규칙에 따라 404 처리한다.
        return scenarioCaseRepository.findByCaseIdAndIsDeletedFalse(caseId)
            .filter(scenarioCase -> scenarioCase.getExecType() == FundamentalModelExecType.INDEPENDENT)
            .orElseThrow(() -> caseNotFound(caseId));
    }

    private ScenarioCase findCompletedCaseForResultQuery(Long caseId) {
        ScenarioCase scenarioCase = findActiveCase(caseId);
        if (scenarioCase.getStatus() != FundamentalModelCaseStatus.COMPLETED) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "fundamental model result is available only for completed case. caseId: %d".formatted(caseId)
            );
        }
        return scenarioCase;
    }

    private ScenarioCase findActiveOwnedCase(Long caseId) {
        ScenarioCase scenarioCase = findActiveCase(caseId);
        Integer actorUserNo = authorizationService.getUserNo();
        if (!scenarioCase.getUserNo().equals(actorUserNo)) {
            throw caseNotFound(caseId);
        }
        return scenarioCase;
    }

    private AppErrorException caseNotFound(Long caseId) {
        return AppErrorException.of(
            ResultCode.Error.RSC_NOT_FOUND,
            FUNDAMENTAL_MODEL_CASE_NOT_FOUND_MESSAGE.formatted(caseId)
        );
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

    private List<FundamentalModelDownloadFileRes> findDownloadFiles(
        Long relationshipObjectDistinguishNo,
        String relationshipObjectSeparateCd
    ) {
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(relationshipObjectDistinguishNo),
            relationshipObjectSeparateCd
        );

        List<FundamentalModelDownloadFileRes> files = new ArrayList<>();
        for (UploadedFile uploadedFile : uploadedFiles) {
            files.add(new FundamentalModelDownloadFileRes(
                uploadedFile.getOriginalFileNm()
            ));
        }
        return files;
    }

    private FundamentalModelJobArgs buildJobArgs(Long caseId, String runId, Long runSeq) {
        List<UploadedFile> inputFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );
        String demandExcelPath = findInputFileStoragePath(inputFiles, "DEMAND");
        String supplyExcelPath = findInputFileStoragePath(inputFiles, "SUPPLY");
        String tradeExcelPath = findInputFileStoragePath(inputFiles, "TRADE");
        validateRequiredInputFilePaths(demandExcelPath, supplyExcelPath, tradeExcelPath);

        return FundamentalModelJobArgs.builder()
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

    private String findInputFileStoragePath(List<UploadedFile> files, String keyword) {
        return files.stream()
            .filter(file -> file.getOriginalFileNm() != null
                && file.getOriginalFileNm().toUpperCase(Locale.ROOT).contains(keyword))
            .findFirst()
            .map(UploadedFile::getStoragePathNm)
            .orElse(null);
    }

    private List<FundamentalModelDownloadFileRes> findOutputFiles(ScenarioCaseRunHistory latestRunHistory) {
        if (latestRunHistory == null || latestRunHistory.getRunSeq() == null) {
            return List.of();
        }
        return findDownloadFiles(
            latestRunHistory.getRunSeq(),
            FILE_RELATION_SCENARIO_CASE_HISTORY
        );
    }

    private Optional<ScenarioCaseRunHistory> findLatestRunHistory(String lastRunId) {
        if (lastRunId == null) {
            return Optional.empty();
        }

        return scenarioCaseRunHistoryRepository.findById(lastRunId);
    }

    private FundamentalModelRunStatusRes findLatestRun(String lastRunId) {
        return findLatestRunHistory(lastRunId)
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

    private record TradeFlowHeaders(
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> fromCountries,
        List<FundamentalModelTradeFlowsRes.TradeCountryHeader> toCountries
    ) {
    }

    private record VolumeBucketKey(
        FundamentalModelDataType type,
        FundamentalModelRegion region
    ) {
    }

    private record ParsedVolumeBucket(
        List<String> periodKeys,
        List<ParsedVolumeRow> rows
    ) {
    }

    private record ParsedVolumeRow(
        VolumesRowKind rowKind,
        String country,
        Map<String, Double> values
    ) {
    }

    private record ParsedPriceRow(
        String periodUnitSeparateCode,
        Integer year,
        Integer month,
        String priceRegion,
        Double baseCasePrice,
        Double userAdjustedPrice
    ) {
    }

}
