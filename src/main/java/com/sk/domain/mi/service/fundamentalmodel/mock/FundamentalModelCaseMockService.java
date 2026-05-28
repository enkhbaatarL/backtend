package com.sk.domain.mi.service.fundamentalmodel.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
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
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesRes;
import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FundamentalModelCaseMockService {

    private static final String MOCK_DATA_RESOURCE = "mock/fundamental-model/fundamental-model-cases.json";
    private static final String VOLUMES_DATA_RESOURCE = "mock/fundamental-model/fundamental-model-volumes-data.json";
    private static final String PRICE_DATA_RESOURCE = "mock/fundamental-model/fundamental-model-price-data.json";
    private static final int REQUIRED_FILE_COUNT = 3;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final DateTimeFormatter RUN_ID_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String MOCK_ASSIGNEE_NAME = "홍길동";
    private static final String DELETE_YN_Y = "Y";
    private static final String DELETE_YN_N = "N";
    private static final String CALLBACK_PATH_TEMPLATE = "/api/v1/mi/fundamental-model/runs/%s/callback";
    private final ObjectMapper objectMapper;
    private final AtomicLong caseIdSequence = new AtomicLong(100);
    private final AtomicLong runSeqSequence = new AtomicLong(0);
    private final ConcurrentMap<Long, FundamentalModelCaseMockRecord> caseStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FundamentalModelRunMockRecord> runStore = new ConcurrentHashMap<>();
    private final KubernetesJobService kubernetesJobService;
    @Value("${server.domain:http://localhost:8080}")
    private String serverDomain = "http://localhost:8080";
    private Map<FundamentalModelSegment, FundamentalModelVolumesSeed> volumesSeedStore = emptyVolumesSeedStore();
    private FundamentalModelPriceSeed priceSeed = emptyPriceSeed();

    public FundamentalModelCaseMockService() {
        this(new ObjectMapper().findAndRegisterModules(), null);
    }

    @Autowired
    public FundamentalModelCaseMockService(ObjectMapper objectMapper, KubernetesJobService kubernetesJobService) {
        this.objectMapper = objectMapper;
        this.kubernetesJobService = kubernetesJobService;
    }

    @PostConstruct
    void init() {
        reloadSeedData();
        loadResultSeedData();
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

    public synchronized FundamentalModelRunAcceptRes runCase(Long caseId) {
        FundamentalModelCaseMockRecord currentCase = getCaseRecord(caseId);
        if (currentCase.status() == FundamentalModelCaseStatus.RUNNING) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "case is already RUNNING"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String runId = generateRunId(now);
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

        // TODO: k8s 클러스터 연결 환경 준비 후 주석 해제
        // if (kubernetesJobService != null) {
        //     long runSeq = runSeqSequence.incrementAndGet();
        //     kubernetesJobService.createFundamentalModelJob(buildJobArgs(currentCase, runId, runSeq))
        //         .whenComplete((ignored, ex) -> {
        //             if (ex != null) {
        //                 log.warn("[Mock] Kubernetes job creation failed: runId={}, caseId={}", runId, caseId, ex);
        //                 markRunFailed(runId, caseId, ex.getMessage());
        //             }
        //         });
        // }

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

    public synchronized FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request) {
        FundamentalModelCaseMockRecord caseRecord = getResultReadyCaseRecord(caseId);
        FundamentalModelSegment segment = validateSegment(request);
        FundamentalModelGranularity granularity = request == null || request.granularity() == null
            ? FundamentalModelGranularity.MONTHLY
            : request.granularity();
        List<FundamentalModelDataType> appliedTypes = normalizeTypes(request == null ? null : request.types());
        List<FundamentalModelRegion> appliedRegions = normalizeRegions(request == null ? null : request.regions());

        FundamentalModelVolumesSeed seed = volumesSeedStore.getOrDefault(segment, emptyVolumesSeed());
        boolean allRegionsApplied = appliedRegions.size() == FundamentalModelRegion.values().length;
        List<FundamentalModelVolumesRes.VolumesRow> filteredRows = seed.rows().stream()
            .filter(row -> matchesVolumesFilters(row, appliedTypes, appliedRegions, allRegionsApplied))
            .toList();

        List<FundamentalModelVolumesRes.VolumesColumn> columns = seed.columns();
        List<FundamentalModelVolumesRes.VolumesRow> rows = filteredRows;
        if (granularity == FundamentalModelGranularity.YEARLY) {
            YearlyVolumes yearlyVolumes = aggregateYearly(columns, filteredRows);
            columns = yearlyVolumes.columns();
            rows = yearlyVolumes.rows();
        }

        return new FundamentalModelVolumesRes(
            caseRecord.caseId(),
            segment,
            granularity,
            new FundamentalModelVolumesRes.VolumesAppliedFilters(appliedTypes, appliedRegions),
            columns,
            rows
        );
    }

    public synchronized FundamentalModelPriceRes getPrice(Long caseId) {
        FundamentalModelCaseMockRecord caseRecord = getResultReadyCaseRecord(caseId);
        return new FundamentalModelPriceRes(
            caseRecord.caseId(),
            priceSeed.chart(),
            priceSeed.annualTable(),
            priceSeed.monthlyTable()
        );
    }

    public synchronized FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        validateCallbackRequest(request);

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
            updatedRun.status(),
            processedAt
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

    private void loadResultSeedData() {
        volumesSeedStore = loadVolumesSeedData();
        priceSeed = loadPriceSeedData();
        log.info("[Mock] Fundamental model result mock data loaded: volumeSegments={}, priceSeriesSets={}",
            volumesSeedStore.size(), priceSeed.chart().seriesSets().size());
    }

    private Map<FundamentalModelSegment, FundamentalModelVolumesSeed> loadVolumesSeedData() {
        ClassPathResource resource = new ClassPathResource(VOLUMES_DATA_RESOURCE);
        if (!resource.exists()) {
            return emptyVolumesSeedStore();
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            Map<FundamentalModelSegment, FundamentalModelVolumesSeed> seeds = emptyVolumesSeedStore();
            for (FundamentalModelSegment segment : FundamentalModelSegment.values()) {
                JsonNode segmentNode = root.path(segment.getCode());
                if (!segmentNode.isMissingNode() && !segmentNode.isNull()) {
                    seeds.put(segment, readVolumesSeed(segmentNode));
                }
            }
            return seeds;
        } catch (IOException | IllegalArgumentException e) {
            log.error("[Mock] Failed to load fundamental model volumes data", e);
            return emptyVolumesSeedStore();
        }
    }

    private FundamentalModelVolumesSeed readVolumesSeed(JsonNode segmentNode) {
        List<FundamentalModelVolumesRes.VolumesColumn> columns = objectMapper.convertValue(
            segmentNode.path("columns"),
            new TypeReference<>() {
            }
        );
        List<FundamentalModelVolumesRes.VolumesRow> rows = objectMapper.convertValue(
            segmentNode.path("rows"),
            new TypeReference<>() {
            }
        );
        return new FundamentalModelVolumesSeed(
            columns == null ? Collections.emptyList() : List.copyOf(columns),
            rows == null ? Collections.emptyList() : List.copyOf(rows)
        );
    }

    private FundamentalModelPriceSeed loadPriceSeedData() {
        ClassPathResource resource = new ClassPathResource(PRICE_DATA_RESOURCE);
        if (!resource.exists()) {
            return emptyPriceSeed();
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            FundamentalModelPriceRes.PriceChart chart = objectMapper.treeToValue(
                root.path("chart"),
                FundamentalModelPriceRes.PriceChart.class
            );
            FundamentalModelPriceRes.PriceTable annualTable = objectMapper.treeToValue(
                root.path("annualTable"),
                FundamentalModelPriceRes.PriceTable.class
            );
            FundamentalModelPriceRes.PriceTable monthlyTable = objectMapper.treeToValue(
                root.path("monthlyTable"),
                FundamentalModelPriceRes.PriceTable.class
            );
            return new FundamentalModelPriceSeed(
                chart == null ? new FundamentalModelPriceRes.PriceChart(Collections.emptyList()) : chart,
                annualTable == null ? emptyPriceTable() : annualTable,
                monthlyTable == null ? emptyPriceTable() : monthlyTable
            );
        } catch (IOException | IllegalArgumentException e) {
            log.error("[Mock] Failed to load fundamental model price data", e);
            return emptyPriceSeed();
        }
    }

    private Map<FundamentalModelSegment, FundamentalModelVolumesSeed> emptyVolumesSeedStore() {
        Map<FundamentalModelSegment, FundamentalModelVolumesSeed> seeds = new EnumMap<>(FundamentalModelSegment.class);
        Arrays.stream(FundamentalModelSegment.values())
            .forEach(segment -> seeds.put(segment, emptyVolumesSeed()));
        return seeds;
    }

    private FundamentalModelVolumesSeed emptyVolumesSeed() {
        return new FundamentalModelVolumesSeed(Collections.emptyList(), Collections.emptyList());
    }

    private FundamentalModelPriceSeed emptyPriceSeed() {
        return new FundamentalModelPriceSeed(
            new FundamentalModelPriceRes.PriceChart(Collections.emptyList()),
            emptyPriceTable(),
            emptyPriceTable()
        );
    }

    private FundamentalModelPriceRes.PriceTable emptyPriceTable() {
        return new FundamentalModelPriceRes.PriceTable(Collections.emptyList(), Collections.emptyList());
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

    private void validateCallbackRequest(FundamentalModelRunCallbackReq request) {
        if (!request.status().isTerminal()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback status는 COMPLETED 또는 FAILED 여야 합니다.");
        }
    }

    private FundamentalModelSegment validateSegment(FundamentalModelVolumesReq request) {
        if (request == null || request.segment() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "segment는 필수입니다.");
        }
        return request.segment();
    }

    private List<FundamentalModelDataType> normalizeTypes(List<FundamentalModelDataType> types) {
        if (types == null || types.isEmpty()) {
            return List.of(FundamentalModelDataType.values());
        }
        return List.copyOf(types);
    }

    private List<FundamentalModelRegion> normalizeRegions(List<FundamentalModelRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return List.of(FundamentalModelRegion.values());
        }
        return List.copyOf(regions);
    }

    private boolean matchesVolumesFilters(
        FundamentalModelVolumesRes.VolumesRow row,
        List<FundamentalModelDataType> appliedTypes,
        List<FundamentalModelRegion> appliedRegions,
        boolean allRegionsApplied
    ) {
        if (row.type() != null && !appliedTypes.contains(row.type())) {
            return false;
        }
        if (row.type() == null && appliedTypes.size() != FundamentalModelDataType.values().length) {
            return false;
        }
        if (row.region() == null) {
            return allRegionsApplied;
        }
        return appliedRegions.contains(row.region());
    }

    private YearlyVolumes aggregateYearly(
        List<FundamentalModelVolumesRes.VolumesColumn> monthlyColumns,
        List<FundamentalModelVolumesRes.VolumesRow> monthlyRows
    ) {
        Map<Integer, List<Integer>> indexesByYear = new LinkedHashMap<>();
        for (int index = 0; index < monthlyColumns.size(); index++) {
            Integer year = monthlyColumns.get(index).year();
            if (year != null) {
                indexesByYear.computeIfAbsent(year, ignored -> new ArrayList<>()).add(index);
            }
        }

        List<FundamentalModelVolumesRes.VolumesColumn> yearlyColumns = indexesByYear.keySet().stream()
            .map(year -> new FundamentalModelVolumesRes.VolumesColumn(year, null, String.valueOf(year)))
            .toList();
        List<FundamentalModelVolumesRes.VolumesRow> yearlyRows = monthlyRows.stream()
            .map(row -> new FundamentalModelVolumesRes.VolumesRow(
                row.type(),
                row.region(),
                row.country(),
                row.rowKind(),
                aggregateValuesByYear(row.values(), indexesByYear)
            ))
            .toList();

        return new YearlyVolumes(yearlyColumns, yearlyRows);
    }

    private List<Double> aggregateValuesByYear(List<Double> values, Map<Integer, List<Integer>> indexesByYear) {
        return indexesByYear.values().stream()
            .map(indexes -> indexes.stream()
                .map(index -> valueAt(values, index))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum())
            .toList();
    }

    private Double valueAt(List<Double> values, int index) {
        if (values == null || index >= values.size()) {
            return null;
        }
        return values.get(index);
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
                currentRun.status(),
                processedAt
            );
        }

        if (currentRun.status() == FundamentalModelRunStatus.FAILED
            && request.status() == FundamentalModelRunStatus.COMPLETED) {
            log.info("[Mock] Late COMPLETED callback ignored: runId={}, caseId={}",
                currentRun.runId(), currentRun.caseId());
            return new FundamentalModelRunCallbackRes(
                currentRun.runId(),
                currentRun.status(),
                processedAt
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

    private synchronized void markRunFailed(String runId, Long caseId, String errorMessage) {
        FundamentalModelRunMockRecord currentRun = runStore.get(runId);
        if (currentRun == null || currentRun.status() != FundamentalModelRunStatus.RUNNING) {
            return;
        }
        runStore.put(runId, currentRun.withTerminal(
            FundamentalModelRunStatus.FAILED,
            LocalDateTime.now(),
            "JOB_SUBMIT_FAILED",
            errorMessage
        ));

        FundamentalModelCaseMockRecord currentCase = caseStore.get(caseId);
        if (currentCase != null && runId.equals(currentCase.latestRunId())) {
            caseStore.put(caseId, currentCase.withTerminal(FundamentalModelCaseStatus.FAILED, LocalDateTime.now()));
        }
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

    private FundamentalModelCaseMockRecord getResultReadyCaseRecord(Long caseId) {
        FundamentalModelCaseMockRecord record = getCaseRecord(caseId);
        if (record.status() == FundamentalModelCaseStatus.PENDING
            || record.status() == FundamentalModelCaseStatus.RUNNING) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Fundamental model result is not ready."
            );
        }
        if (record.status() == FundamentalModelCaseStatus.FAILED) {
            throw AppHttpStatusException.of(
                HttpStatus.NOT_FOUND,
                ResultCode.Error.RSC_NOT_FOUND,
                "Fundamental model result를 찾을 수 없습니다."
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

    private FundamentalModelJobArgs buildJobArgs(FundamentalModelCaseMockRecord caseRecord, String runId, long runSeq) {
        String callbackUrl = serverDomain + CALLBACK_PATH_TEMPLATE.formatted(runId);
        return FundamentalModelJobArgs.builder()
            .runId(runId)
            .runSeq(runSeq)
            .demandExcelPath(findFileMockPath(caseRecord.files(), "DEMAND"))
            .supplyExcelPath(findFileMockPath(caseRecord.files(), "SUPPLY"))
            .tradeExcelPath(findFileMockPath(caseRecord.files(), "TRADE"))
            .callbackUrl(callbackUrl)
            .build();
    }

    private String findFileMockPath(List<FundamentalModelFileMockRecord> files, String keyword) {
        return files.stream()
            .filter(f -> f.originalFileName() != null
                && f.originalFileName().toUpperCase(Locale.ROOT).contains(keyword))
            .findFirst()
            .map(f -> "mock-storage://" + f.mockFileId())
            .orElse(null);
    }

    private String buildStatusEndpoint(Long caseId) {
        return "/api/v1/mi/fundamental-model/cases/%d/status"
            .formatted(caseId);
    }

    private String generateRunId(LocalDateTime requestedAt) {
        String uuid = UUID.randomUUID().toString();
        String suffix = uuid.substring(uuid.length() - 4).toUpperCase(Locale.ROOT);
        return "%s_%s".formatted(RUN_ID_TIMESTAMP_FORMATTER.format(requestedAt), suffix);
    }

    private record FundamentalModelVolumesSeed(
        List<FundamentalModelVolumesRes.VolumesColumn> columns,
        List<FundamentalModelVolumesRes.VolumesRow> rows
    ) {
    }

    private record FundamentalModelPriceSeed(
        FundamentalModelPriceRes.PriceChart chart,
        FundamentalModelPriceRes.PriceTable annualTable,
        FundamentalModelPriceRes.PriceTable monthlyTable
    ) {
    }

    private record YearlyVolumes(
        List<FundamentalModelVolumesRes.VolumesColumn> columns,
        List<FundamentalModelVolumesRes.VolumesRow> rows
    ) {
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
