package com.sk.domain.mi.service;

import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sk.common.config.database.DataSourceContextHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.CombineUtil;
import com.sk.common.utils.DateBindingUtil;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.dashboard.DashboardChartFactorGraphDatasource;
import com.sk.domain.mi.dto.response.dashboard.DashboardChartFactorGraphRes;
import com.sk.domain.mi.dto.response.dashboard.DashboardInfoRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsDto;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactor;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorExplanatoryMap;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorGraphMap;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorMap;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorRes;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartData;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartDateRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardDataCache;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardDataSource;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChart;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartLine;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;
import com.sk.domain.mi.dto.response.dashboard.charts.metaData.DashboardChartFactorGraphMetadata;
import com.sk.domain.mi.dto.response.dashboard.charts.metaData.DashboardMetadata;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardSummary;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardSummaryRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartPredictDataType;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;
import com.sk.domain.shared.enums.dashboard.DashboardChartDataTableType;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import com.sk.domain.shared.mapper.dynamic.DashboardMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardService {
    private final GoodsDataRepository goodsDataRepository;
    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final DashboardMapper dashboardMapper;
    private final FileService fileService;
    private final AuthorizationService authorizationService;

    private final int CHART_EXPLANATORY_TOP_RANK_LIMIT = 5;
    private final String CHART_EXPLANATORY_PLUS_DIRECTION = "+";
    private final String CHART_EXPLANATORY_MINUS_DIRECTION = "-";

    private final String DASHBOARD_CHART_CACHE_KEY = "dashboard_chart:%d_based_on_date:%s";
    // TODO: 캐시 유효 기간 수정 필요(테스트를 위해 임시로 1초로 설정)
    // private static final int DASHBOARD_CHART_CACHE_TTL = 3600; // 대시보드 차트 캐시 유효 기간 (1시간)
    private static final int DASHBOARD_CHART_CACHE_TTL = 1; // 대시보드 차트 캐시 유효 기간 (1초)

    private final int DASHBOARD_CHART_START_DATE_FROM_MONTH = 11; // 차트 데이터 조회 기준 날짜 (N달 전)
    private static final int MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX = 0; // 예측 결과 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스
    private static final String DASHBOARD_CHART_LAST_UPDATE_DATE_LIMIT = "1"; // 마지막 Update Date 조회 하기 위한 LIMIT
    private static final String MARKET_DATA_PREDICT_DATE_COLUMN_NAME = "prdc_fulf_dt"; // 예측 수행 날짜 컬럼명
    private static final String DASHBOARD_CHART_DATA_TABLE_HEADER_UNIT = "Unit"; // 차트 데이터 테이블 헤더 단위 컬럼명

    private static final String EXCEL_SHEET_NAME = "dashboard_chart";

    @Value("#{'${server.insight.latest.except-provider-no}'.split(',')}")
    private List<Integer> exceptProviderNoList;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 데이터 존재 여부 조회
     *
     * @param companyNo
     * @return boolean
     */
    public boolean getDataExists(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        return chartPredictSettingRepository.existsByCustomerCompany(customerCompany);
    }

    /**
     * 대시보드 기본 정보 조회
     *
     * @param companyNo
     * @return DashboardInfoRes
     */
    public DashboardInfoRes getDashboardInfo(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 최근 업데이트 일시(예측 데이터중 가장 최근 업데이트 날짜)
        List<LocalDate> lastUpdatedList = chartPredictSettingDetails.stream()
                .filter(chartPredictSettingDetail -> chartPredictSettingDetail.getIsChartLinePredictVal())
                .map(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();

                    // 날짜 칼럼
                    String dateColumnName = MARKET_DATA_PREDICT_DATE_COLUMN_NAME;

                    List<Map<String, Object>> lastUpdateDateData = getLastUpdateDateData(goodsDataSource, dateColumnName);

                    if (lastUpdateDateData.isEmpty()) {
                        return null;
                    }

                    return DateUtil.parseDate(lastUpdateDateData.get(0).get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME));
                })
                .collect(Collectors.toList());

        // 예측 데이터 마지막 업데이트 중에 가장 날짜가 최근인 값
        LocalDate lastUpdated = lastUpdatedList.stream()
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        return DashboardInfoRes.of(chartPredictSetting, lastUpdated);
    }

    /**
     * Latest Insights 목록 조회
     *
     * @param companyNo
     * @return List<LatestInsights>
     */
    public List<LatestInsightsRes> getLatestInsights(int companyNo) {

        List<LatestInsightsDto> datas = goodsDataRepository.findLatestInsights(companyNo, exceptProviderNoList, PageRequest.of(0, 5));

        return datas.stream()
                .map(data -> new LatestInsightsRes(
                        data.getProviderNo(),
                        data.getProviderNm(),
                        data.getGoodsNo(),
                        data.getGoodsNm(),
                        data.getGoodsDataNo(),
                        data.getGoodsDataNm(),
                        data.getModifyDt(),
                        isDataViewable(companyNo, data.getGoodsNo())))
                .collect(Collectors.toList());
    }

    /**
     * 대시보드 Summary 데이터 조회
     *
     * @param companyNo 회사 번호
     * @param date      날짜
     * @return DashboardSummaryRes
     */
    public DashboardSummaryRes getDashboardSummary(int companyNo, String date) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        // 예측 데이터 조회
        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 대시보드 요약 파일 다운로드 가능 여부 체크
        boolean isDownloadable = isDownloadableDashboardSummaryFile(chartPredictSettingDetails, companyNo);

        List<DashboardSummary> summaries = getDashboardSummaries(chartPredictSettingDetails, date, null);

        boolean isFileExists = summaries.stream()
                .anyMatch(summary -> summary.explanatoryFilePath() != null);

        return DashboardSummaryRes.of(summaries, isFileExists && isDownloadable);
    }

    /**
     * 대시보드 Summary 파일 다운로드
     *
     * @param companyNo 회사 번호
     * @param date      날짜
     * @return byte[]z
     */
    public FileDownloadRes getDashboardSummaryFileDownload(int companyNo, String date, String predictDate, String zipFileName) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        // 예측 데이터 조회
        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        boolean isDownloadable = isDownloadableDashboardSummaryFile(chartPredictSettingDetails, companyNo);

        if (!isDownloadable) {
            throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED, String.format("다운로드 권한이 없습니다. %s", companyNo));
        }

        // 대시보드 요약 데이터 조회
        List<DashboardSummary> summaries = getDashboardSummaries(chartPredictSettingDetails, date, predictDate);

        if (summaries == null || summaries.isEmpty()) {
            log.warn("No dashboard summaries found for companyNo: {}, date: {}, predictDate: {}",
                    companyNo, date, predictDate);
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "대시보드 요약 데이터가 없습니다.");
        }

        List<String> filePaths = summaries.stream()
                .filter(Objects::nonNull)
                .map(DashboardSummary::explanatoryFilePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (filePaths.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "다운로드 가능한 파일이 없습니다.");
        }

        return fileService.directDownloadFiles(filePaths, zipFileName);
    }

    /**
     * 대시보드 예측 데이터 Summary List 데이터 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param date 날짜
     * @param predictDate 예측 날짜
     * @return List<DashboardSummary>
     */
    @Transactional
    public List<DashboardSummary> getDashboardSummaries(List<ChartPredictSettingDetail> chartPredictSettingDetails, String date,
            String predictDate) {
        String baseDate = date != null ? date : DateUtil.toYmd(LocalDate.now());

        String startDate = predictDate != null ? DateUtil.toYmd(DateUtil.parseDate(predictDate).withDayOfMonth(1)) : null;
        String endDate = predictDate != null ? DateUtil.toYmd(DateUtil.parseDate(predictDate).with(TemporalAdjusters.lastDayOfMonth())) : null;

        return chartPredictSettingDetails.stream()
                .filter(chartPredictSettingDetail -> chartPredictSettingDetail.getIsChartLinePredictVal())
                .flatMap(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSourceDetail = chartPredictSettingDetail.getGoodsDataSourceDetail();
                    if (goodsDataSourceDetail == null || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                        return Stream.empty();
                    }

                    DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

                    String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

                    // summary 데이터 리스트 조회
                    List<DashboardChartSummaryMap> summaries = dashboardMapper.chartSummary(queryWhere, baseDate, startDate, endDate);

                    // explanatoryFilePath 디코딩
                    Stream<DashboardChartSummaryMap> decodedSummaries = summaries != null ? summaries.stream().map(summary -> {
                        if (summary.getExplanatoryFilePath() == null) {
                            return summary;
                        }
                        try {
                            String decodedPath = URLDecoder.decode(summary.getExplanatoryFilePath(), StandardCharsets.UTF_8);
                            log.debug("Decoded explanatoryFilePath: {}", decodedPath);
                            return DashboardChartSummaryMap.builder()
                                    .marketIndexAnalysisTargetCd(summary.getMarketIndexAnalysisTargetCd())
                                    .predictionFulfillDt(summary.getPredictionFulfillDt())
                                    .mnemonicCd(summary.getMnemonicCd())
                                    .predictionResultDt(summary.getPredictionResultDt())
                                    .explanatoryTitleContent(summary.getExplanatoryTitleContent())
                                    .explanatorySummaryContent(summary.getExplanatorySummaryContent())
                                    .explanatoryFilePath(decodedPath)
                                    .build();
                        } catch (IllegalArgumentException e) {
                            log.warn("Failed to decode explanatoryFilePath: {}, error: {}",
                                    summary.getExplanatoryFilePath(), e.getMessage());
                            return summary; // 디코딩 실패 시 원본 반환
                        }
                    }) : Stream.empty();
                    return decodedSummaries;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                summary -> summary.getMarketIndexAnalysisTargetCd() + "|" +
                                        summary.getPredictionFulfillDt() + "|" +
                                        summary.getMnemonicCd() + "|" +
                                        summary.getPredictionResultDt(),
                                summary -> summary,
                                (existing, replacement) -> existing // 중복 키가 있으면 기존 값 유지
                        ),
                        map -> map.values().stream()
                                .map(DashboardSummary::of)
                                .collect(Collectors.toList())));
    }

    /**
     * 대시보드 요약 파일 다운로드 가능 여부 체크
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param companyNo 회사 번호
     * @return boolean
     */
    private boolean isDownloadableDashboardSummaryFile(List<ChartPredictSettingDetail> chartPredictSettingDetails,
            int companyNo) {

        PermissionRes permissionRes = authorizationService.getPermissions();

        return chartPredictSettingDetails.stream()
                .filter(chartPredictSettingDetail -> chartPredictSettingDetail.getGoodsDataSourceDetail().getIsPredictData())
                .allMatch(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                            .findByGoodsDataSourceNoAndIsDeletedFalse(chartPredictSettingDetail.getGoodsDataSourceDetail().getGoodsDataSourceNo())
                            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    String.format("데이터소스 조회 실패: %s", chartPredictSettingDetail.getGoodsDataSourceDetail().getGoodsDataSourceNo())));
                    GoodsData goodsData = goodsDataSourceDetail.getGoodsData();
                    Goods goods = goodsData.getGoods();
                    return permissionRes.getGoodsDownloadableMap(companyNo).getOrDefault(goods.getGoodsNo(), false);
                });
    }

    /**
     * 조회 가능한 차트 날짜 조회
     *
     * @param companyNo
     * @return DashboardChartDateRes
     */
    public DashboardChartDateRes getChartDates(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        return DashboardChartDateRes.of(chartPredictSettingDetails.parallelStream()
                .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
                .flatMap(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();
                    if (goodsDataSource == null) {
                        return Stream.empty();
                    }

                    // 데이터 조회
                    List<Map<String, Object>> rawData = getDateDataRows(goodsDataSource, MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                    return rawData.stream()
                            .map(row -> {
                                Object dateValue = row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                                if (dateValue == null) {
                                    return null;
                                }
                                return DateUtil.parseDate(dateValue);
                            })
                            .filter(Objects::nonNull);
                })
                .distinct()
                .collect(Collectors.toList()));
    }

    /**
     * 대시보드 차트 데이터 조회
     *
     * @param customerCompanyNo
     * @param date
     * @return
     */
    public DashboardChartRes getChart(Integer customerCompanyNo, String date) {
        String finalDate = (date == null || date.isEmpty()) ? DateUtil.toYearMonth(LocalDate.now()) : date;
        LocalDate baseDate = date == null || date.isEmpty() ? LocalDate.now() : DateUtil.fromYearMonth(date);

        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(customerCompanyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", customerCompanyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 차트 데이터 조회
        GoodsDataSourceDetail[] actualGoodsDataSourceDetail = {null};
        Map<Integer, DashboardChart> chartMap = new HashMap<>();

        Boolean isDownloadFunction = Boolean.TRUE; // 다운로드 기능 여부(processChartData 함수를 호출하는 함수가 Download 권한을 체크하는지 View 권한을 체크하는지)
        List<DashboardChartData> chartDataResults = processChartData(
                chartPredictSettingDetails,
                permissionRes,
                isDownloadFunction,
                customerCompanyNo,
                baseDate,
                chartPredictSetting,
                actualGoodsDataSourceDetail,
                chartMap);

        // ConcurrentHashMap을 List<Map<Integer, DashboardChart>>로 변환
        List<Map<Integer, DashboardChart>> associatedCharts = chartMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .values().stream()
                .map(HashMap::new)
                .collect(Collectors.toList());

        // 예측 데이터 마지막 업데이트 중 가장 최근 날짜
        LocalDate lastUpdated = chartDataResults.stream()
                .map(DashboardChartData::lastUpdated)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        // DashboardChartDataTableType 결정
        DashboardChartDataTableType dataTableType = calculateDashboardChartDataTableType(chartPredictSettingDetails);

        // DashboardMetadata 생성
        DashboardMetadata dashboardMetadata = DashboardMetadata.of(
                customerCompany.getMarketIndexAnalysisTargetCd(),
                lastUpdated,
                chartDataResults.stream()
                        .map(DashboardChartData::forecastGoodsDataSourceDetail)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null),
                finalDate,
                dataTableType,
                chartPredictSetting.getPredictTargetRepresentSpecNm());

        // associatedCharts에서 DashboardChart 리스트 추출
        List<DashboardChart> finalCharts = associatedCharts.stream()
                .flatMap(map -> map.values().stream())
                .map(chart -> {
                    DashboardChartLine actualLines = chart.actualChartLines() != null
                            ? chart.actualChartLines()
                            : DashboardChartLine.of(null, null, null, null, null);
                    DashboardChartLine forecastLines = chart.forecastChartLines() != null
                            ? chart.forecastChartLines()
                            : DashboardChartLine.of(null, null, null, null, null);
                    return new DashboardChart(
                            chart.chartLineDescription(),
                            chart.chartLineType(),
                            actualLines,
                            forecastLines);
                })
                .sorted(Comparator
                        .comparing(
                                DashboardChart::chartLineType,
                                Comparator.nullsLast(
                                        Comparator.comparing(ChartPredictLineType::getCode, Comparator.nullsLast(Comparator.naturalOrder()))))
                        .thenComparing(c->c.actualChartLines().goodsDataSourceNo() == null
                                ,
                                Comparator.nullsLast(Comparator.comparing(b->!b)))
                        .thenComparing(
                                DashboardChart::chartLineDescription,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .toList();

        // dashboardDataSources 수집
        List<DashboardDataSource> dashboardDataSources = chartDataResults.stream()
                .map(DashboardChartData::dashboardDataSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // isFileDownload 최종 값
        boolean finalIsFileDownload = chartDataResults.stream()
                .map(DashboardChartData::dashboardChart) // DashboardChart 추출
                .filter(Objects::nonNull) // dashboardChart가 null인 경우 필터링
                .flatMap(chart -> Stream.of(chart.actualChartLines(), chart.forecastChartLines())) // actual과 forecast 추출
                .filter(Objects::nonNull) // DashboardChartLine이 null인 경우 필터링
                .map(DashboardChartLine::isPermitted) // isDownloadable 값 추출
                .anyMatch(permission -> permission != null && permission); // 하나라도 true인지 확인

        return DashboardChartRes.of(dashboardMetadata, finalCharts, dashboardDataSources, finalIsFileDownload);
    }

    private DashboardChartDataTableType calculateDashboardChartDataTableType(List<ChartPredictSettingDetail> chartPredictSettingDetails){
        DashboardChartDataTableType dataTableType = DashboardChartDataTableType.FORECAST_SINGLE_LAYER;
        List<ChartPredictSettingDetail> chartPredictSettingDetailPredictList = chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal).toList();
        if(chartPredictSettingDetailPredictList.size() >= 2){
            Integer associateDataSource = null;
            for(ChartPredictSettingDetail predict : chartPredictSettingDetailPredictList){
                if(predict.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo().equals(predict.getGoodsDataSourceDetail().getGoodsDataSourceNo())){
                    continue;
                }
                if(associateDataSource == null){
                    associateDataSource = predict.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo();
                    continue;
                }
                if(!associateDataSource.equals(predict.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo())){
                    dataTableType = DashboardChartDataTableType.FORECAST_DUAL_LAYER;
                }
            }
        }
        return dataTableType;
    }

    /**
     * 차트 데이터 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param permissionRes 권한 정보
     * @param isDownloadFunction 다운로드 기능 여부(해당 함수를 호출하는 함수가 Download 권한을 체크하는지 View 권한을 체크하는지)
     * @param customerCompanyNo 고객 회사 번호
     * @param baseDate 기준 날짜
     * @param chartPredictSetting 차트 설정
     * @param actualGoodsDataSourceDetail 실제 데이터 소스 상세 정보
     * @param chartMap 차트 맵
     * @return 차트 데이터 목록
     */
    private List<DashboardChartData> processChartData(
            List<ChartPredictSettingDetail> chartPredictSettingDetails,
            PermissionRes permissionRes,
            Boolean isDownloadFunction,
            Integer customerCompanyNo,
            LocalDate baseDate,
            ChartPredictSetting chartPredictSetting,
            GoodsDataSourceDetail[] actualGoodsDataSourceDetail,
            Map<Integer, DashboardChart> chartMap) {
        return chartPredictSettingDetails.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChartPredictSettingDetail::getIsChartLinePredictVal))
                .map(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();
                    GoodsData goodsData = goodsDataSource.getGoodsData();
                    Goods goods = goodsData.getGoods();

                    // 권한 조회
                    boolean isPermitted = isDownloadFunction ? permissionRes.getGoodsDownloadableMap(customerCompanyNo)
                            .getOrDefault(goods.getGoodsNo(), false)
                            : permissionRes.getGoodsViewableMap(customerCompanyNo)
                                    .getOrDefault(goods.getGoodsNo(), false);

                    // 차트 데이터 타입 설정
                    ChartPredictDataType chartPredictDataType;
                    if (chartPredictSettingDetail.getIsFuture()) {
                        chartPredictDataType = ChartPredictDataType.FUTURE;
                    } else if (chartPredictSettingDetail.getIsChartLinePredictVal()) {
                        chartPredictDataType = ChartPredictDataType.FORECAST;
                        isPermitted = true; // 예측 데이터는 무조건 권한 있음
                    } else {
                        chartPredictDataType = ChartPredictDataType.ACTUAL;
                        // Actual 데이터 소스 화폐 단위 (최초 설정만 유지)
                        if (actualGoodsDataSourceDetail[0] == null) {
                            actualGoodsDataSourceDetail[0] = goodsDataSource;
                        }
                    }

                    // 칼럼 목록 조회
                    String[] tableColumnList = Arrays.stream(goodsDataSource.getTableColumnListNm().split(","))
                            .map(String::trim)
                            .toArray(String[]::new);
                    String dateColumnName = tableColumnList[MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX];
                    String defaultDataColumnName = tableColumnList[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

                    // 날짜 범위 설정
                    LocalDate endDate = baseDate.plusMonths(chartPredictSetting.getPredictPeriodMonthsCnt())
                            .with(TemporalAdjusters.lastDayOfMonth());
                    LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH)
                            .withDayOfMonth(1);

                    String cacheKey = getDashboardChartCacheKey(chartPredictSettingDetail.getChartPredictSettingDetailNo(),
                            baseDate.toString());

                    // 차트 캐시 조회
                    List<DashboardChartRawData> monthlyAverages = null;
                    String cachedData = redisTemplate.opsForValue().get(cacheKey);
                    LocalDate lastUpdated = null;
                    DashboardDataSource dashboardDataSource = null;

                    if (cachedData != null) {
                        try {
                            DashboardDataCache dashboardDataCache = objectMapper.readValue(cachedData, DashboardDataCache.class);
                            monthlyAverages = dashboardDataCache.monthlyAverages();
                            lastUpdated = dashboardDataCache.lastUpdated();
                            dashboardDataSource = dashboardDataCache.dataSource();
                        } catch (Exception e) {
                            redisTemplate.delete(cacheKey);
                            log.error("마켓데이터 캐시 조회 실패 : {}", e.getMessage());
                        }
                    }

                    if (monthlyAverages == null) {
                        // 원시 데이터 조회
                        List<Map<String, Object>> rawData = chartPredictSettingDetail.getIsChartLinePredictVal()
                                ? getForecastDataRows(goodsDataSource, dateColumnName, defaultDataColumnName, startDate, endDate)
                                : getActualDataRows(goodsDataSource, dateColumnName, startDate, endDate);

                        // 예측 데이터인 경우
                        if (chartPredictSettingDetail.getIsChartLinePredictVal() && !rawData.isEmpty()) {
                            String baseYm = DateUtil.toYearMonth(baseDate);
                            List<Map<String, Object>> baseYmRawData = rawData.stream()
                                .filter(
                                    r->((String)r.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME)).substring(0, 6).equals(baseYm)
                                )
                                .toList();
                            lastUpdated = DateUtil.parseDate(baseYmRawData.get(baseYmRawData.size() - 1).get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME));
                            dashboardDataSource = DashboardDataSource.of(goodsDataSource, chartPredictSettingDetail);
                        }

                        // 월별 날짜 생성
                        List<LocalDate> monthlyDates = generateMonthlyDates(startDate, endDate);

                        // 월별 평균 계산
                        monthlyAverages = calculateMonthlyAverages(monthlyDates, rawData, dateColumnName, defaultDataColumnName,
                                chartPredictSettingDetail.getIsChartLinePredictVal(), lastUpdated);

                        // 캐시 저장
                        DashboardDataCache dashboardDataCache = DashboardDataCache.of(monthlyAverages, lastUpdated, dashboardDataSource);
                        saveDashboardChartCache(cacheKey, dashboardDataCache);
                    }

                    DashboardChartLine dashboardChartLine = DashboardChartLine.of(isPermitted, chartPredictDataType,
                            goodsDataSource.getGoodsDataSourceNo(), monthlyAverages, chartPredictSettingDetail.getPredictChartDetailExpn());

                    // DashboardChart 생성

                    Integer goodsDataSourceNo = chartPredictSettingDetail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo(); // chartMap
                                                                                                                                       // sort해서
                                                                                                                                       // actual 데이터
                                                                                                                                       // 먼저 나오도록 먼저
                                                                                                                                       // 정리
                    DashboardChart dashboardChart = chartMap.compute(goodsDataSourceNo, (key, existingChart) -> {
                        if (existingChart == null) {
                            return DashboardChart.of(
                                    chartPredictSettingDetail,
                                    chartPredictSettingDetail.getIsChartLinePredictVal() ? null : dashboardChartLine,
                                    chartPredictSettingDetail.getIsChartLinePredictVal() ? dashboardChartLine : null);
                        } else {
                            return DashboardChart.builder()
                                    .chartLineDescription(existingChart.chartLineDescription() != null
                                            ? existingChart.chartLineDescription()
                                            : chartPredictSettingDetail.getPredictChartDetailExpn())
                                    .chartLineType(existingChart.chartLineType() != null
                                            ? existingChart.chartLineType()
                                            : chartPredictSettingDetail.getChartPredictLineSeparateCd())
                                    .actualChartLines(chartPredictSettingDetail.getIsChartLinePredictVal()
                                            ? existingChart.actualChartLines()
                                            : dashboardChartLine)
                                    .forecastChartLines(chartPredictSettingDetail.getIsChartLinePredictVal()
                                            ? dashboardChartLine
                                            : existingChart.forecastChartLines())
                                    .build();
                        }
                    });

                    return DashboardChartData.of(lastUpdated, dashboardDataSource, actualGoodsDataSourceDetail[0], dashboardChart);
                })
                .collect(Collectors.toList());
    }

    /**
     * 월별 날짜 생성
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return List<LocalDate>
     */
    public List<LocalDate> generateMonthlyDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> monthlyDates = new ArrayList<>();
        LocalDate current = startDate.withDayOfMonth(1); // 월의 첫 번째 날
        while (!current.isAfter(endDate)) {
            monthlyDates.add(current);
            current = current.plusMonths(1).withDayOfMonth(1);
        }
        return monthlyDates;
    }

    /**
     * 월별 평균 계산
     *
     * @param monthlyDates 월별 날짜 목록
     * @param rawData 원시 데이터 목록
     * @param dateColumnName 날짜 칼럼명
     * @param defaultDataColumnName 기본 데이터 칼럼명
     * @return List<DashboardChartRawData>
     */
    public List<DashboardChartRawData> calculateMonthlyAverages(List<LocalDate> monthlyDates, List<Map<String, Object>> rawData,
            String dateColumnName, String defaultDataColumnName, boolean isPredict, LocalDate lastUpdated) {
        // 결과 리스트
        List<DashboardChartRawData> monthlyAverages = new ArrayList<>();

        // DateTimeFormatter for YYYY-MM
        DateTimeFormatter yyyyMMFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // rawData를 월별로 그룹화
        Map<String, List<Double>> monthlyValues = new TreeMap<>();
        Map<String, String> monthlyPredictDates = new TreeMap<>(); // prdc_fulf_dt 저장
        for (Map<String, Object> row : rawData) {
            String yearMonth = DateUtil.toYyyyMm(DateUtil.parseDate(row.get(dateColumnName)));
            String predictDate = null;

            // 기본 데이터 칼럼 값
            Object val = row.get(defaultDataColumnName);
            Double valueData;

            // 예측 데이터인 경우에만 예측수행날짜가 있다
            if (isPredict) {
                predictDate = (String) row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                // 예측 수행 날짜가 다르면 skip ( 제외 )
                if(predictDate.equals(DateUtil.toYmd(lastUpdated))){
                    monthlyPredictDates.put(yearMonth, predictDate);
                }else{
                    continue;
                }
                // monthlyPredictDates.put(yearMonth, predictDate);
            }

            // 기본 데이터 칼럼 값 형식 변환 Object -> Double
            if (val == null) {
                valueData = null;
            } else if (val instanceof Double) {
                valueData = (Double) val;
            } else if (val instanceof BigDecimal) {
                valueData = ((BigDecimal) val).doubleValue();
            } else {
                try {
                    valueData = Double.parseDouble(val.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid number format: {}, column: {}, skipping value", val, defaultDataColumnName, e);
                    valueData = null;
                }
            }

            // 월별 평균 그룹화
            monthlyValues.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(valueData);
        }

        // 월별 평균 계산
        for (LocalDate month : monthlyDates) {
            String yearMonth = month.format(yyyyMMFormatter);
            List<Double> values = monthlyValues.getOrDefault(yearMonth, Collections.emptyList());
            String predictDate = isPredict ? monthlyPredictDates.getOrDefault(yearMonth, null) : null;

            // 월별 평균 결과 데이터
            DashboardChartRawData dashboardChartRawData;
            if (values.isEmpty()) {
                // 예측 데이터인 경우
                if (isPredict) {
                    dashboardChartRawData = DashboardChartRawData.ofForecast(yearMonth, predictDate, null);
                    // 정형 데이터인 경우
                } else {
                    dashboardChartRawData = DashboardChartRawData.ofActual(yearMonth, null);
                }
            } else {
                // 월별 평균 계산
                double average = values.stream()
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                // 소수점 2자리
                BigDecimal bd = BigDecimal.valueOf(average)
                        .setScale(2, RoundingMode.HALF_UP);
                // 예측 데이터인 경우
                if (isPredict) {
                    dashboardChartRawData = DashboardChartRawData.ofForecast(yearMonth, predictDate, bd.doubleValue());
                    // 정형 데이터인 경우
                } else {
                    dashboardChartRawData = DashboardChartRawData.ofActual(yearMonth, bd.doubleValue());
                }
            }

            // 월별 평균 결과 저장
            monthlyAverages.add(dashboardChartRawData);
        }
        return monthlyAverages;
    }

    /**
     * 대시보드 그래프 캐시 키 생성
     *
     * @param chartPredictSettingDetailNo 차트 예측 설정 상세 번호
     * @return 캐시 키
     */
    private String getDashboardChartCacheKey(Integer chartPredictSettingDetailNo, String basedOnDate) {
        return String.format(DASHBOARD_CHART_CACHE_KEY, chartPredictSettingDetailNo, basedOnDate);
    }

    /**
     * Redis에 대시보드 그래프 요약 캐시 저장
     *
     * @param key 캐시 키
     * @param cache 캐시 객체
     */
    private void saveDashboardChartCache(String key, DashboardDataCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(DASHBOARD_CHART_CACHE_TTL));
        } catch (Exception e) {
            log.error("Failed to serialize and save cache for key: {}", key, e);
        }
    }

    /**
     * 예측 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName 날짜 칼럼명
     * @param valueColumnName 기본 데이터 칼럼명
     * @param startDate 시작일
     * @param endDate 종료일
     * @return List<Map<String, Object>>
     */
    private List<Map<String, Object>> getForecastDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String valueColumnName,
            LocalDate startDate,
            LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        String END_DATE = DateUtil.toYmd(endDate);
        String START_DATE = DateUtil.toYmd(startDate);

        String dbQueryConditionCntt = goodsDataSourceDetail.getQueryConditionCntt();
        String dbColumnList = goodsDataSourceDetail.getTableColumnListNm();

        // TODO: 정규식 패턴 수정 필요 and가 몇개 있을지 모름

        // 정규 표현식 패턴을 이용해서 getTableColumnList 에서 칼럼, 데이터 추출
        String pattern = "([a-zA-Z_]+)\\s*=\\s*'([^']*)'\\s*and\\s*([a-zA-Z_]+)\\s*=\\s*'([^']*)'";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(dbQueryConditionCntt);

        String key1 = null;
        String value1 = null;
        String key2 = null;
        String value2 = null;

        if (matcher.find()) {
            key1 = matcher.group(1);
            value1 = matcher.group(2);
            key2 = matcher.group(3);
            value2 = matcher.group(4);
        } else {
            throw new IllegalArgumentException("Invalid condition format: " + dbQueryConditionCntt);
        }

        String columns = String.format("%s, %s", dbColumnList, MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
        String tableName = String.format("%s AS t1", goodsDataSourceDetail.getTargetTableNm());
        String innerJoin = String.format(
                """
                            INNER JOIN
                            (
                                SELECT
                                    SUBSTRING(%s, 1, 6) AS yyyymm,
                                    MAX(%s) AS max_date
                                FROM %s
                                WHERE
                                    %s BETWEEN '%s' AND '%s'
                                    AND %s = '%s'
                                    AND %s = '%s'
                                GROUP BY SUBSTRING(%s, 1, 6)
                            ) AS l
                            ON t1.%s = l.max_date
                            AND t1.%s = '%s'
                            AND t1.%s = '%s'
                        """,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                goodsDataSourceDetail.getTargetTableNm(),
                dateColumnName, START_DATE, END_DATE,
                key1, value1,
                key2, value2,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                key1, value1,
                key2, value2);
        String orderByColumns = String.format("%s", MARKET_DATA_PREDICT_DATE_COLUMN_NAME);

        return mybatisDynamicQueryService.getDynamicForecast(
                DynamicDataDetailReq.ofInnerJoin(goodsDataSourceDetail.getTargetDbNm(), tableName, columns, innerJoin, orderByColumns));
    }

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @param startDate             데이터 범위 시작일 (포함, 예측 데이터는 YYYYMMDD 형식, 정형 데이터는 yyyy-MM-dd 형식)
     * @param endDate               데이터 범위 종료일 (포함, 예측 데이터는 YYYYMMDD 형식, 정형 데이터는 yyyy-MM-dd 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getActualDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, LocalDate startDate,
            LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        SchemaMetadata meta = mybatisDynamicQueryService.getMeta(DynamicDataReq.ofMetaQuery(goodsDataSourceDetail));
        Pair<String, String> range = DateBindingUtil.range(meta, startDate, endDate);
        String whereClause = CombineUtil.createBetweenClause(dateColumnName, range.getLeft(), range.getRight());

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }

    /**
     * 마지막 Update Date 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getLastUpdateDateData(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {

        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        String orderByColumns = String.format("%s DESC", dateColumnName);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofLimitAndOrderBy(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), dateColumnName, goodsDataSourceDetail.getQueryConditionCntt(),
                DASHBOARD_CHART_LAST_UPDATE_DATE_LIMIT, orderByColumns));
    }

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getDateDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {
        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        // yyyyMM 형식으로 데이터 묶기 위함
        int startDateLength = 1;
        int endDateLength = 6;
        String dateColumn = String.format("MAX(%s) as %s", dateColumnName, dateColumnName);
        String groupBy = "SUBSTRING(%s, %d, %d)".formatted(dateColumnName, startDateLength, endDateLength);
        String orderBy = String.format("%s DESC", groupBy);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofGroupByOrderBy(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), dateColumn, goodsDataSourceDetail.getQueryConditionCntt(), groupBy, orderBy));
    }

    /**
     * 차트 파일 다운로드
     *
     * @param customerCompanyNo 고객사 번호
     * @param date 조회할 년월
     * @return DashboardChartRes
     */
    public byte[] getChartFileDownload(int customerCompanyNo, String date) throws IOException {
        LocalDate baseDate = date == null || date.isEmpty() ? LocalDate.now() : DateUtil.fromYearMonth(date);

        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(customerCompanyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", customerCompanyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 차트 데이터 조회
        GoodsDataSourceDetail[] actualGoodsDataSourceDetail = {null};
        Map<Integer, DashboardChart> chartMap = new HashMap<>();

        Boolean isDownloadFunction = Boolean.TRUE; // 다운로드 기능 여부(processChartData 함수를 호출하는 함수가 Download 권한을 체크하는지 View 권한을 체크하는지)
        List<DashboardChartData> chartDataResults = processChartData(
                chartPredictSettingDetails,
                permissionRes,
                isDownloadFunction,
                customerCompanyNo,
                baseDate,
                chartPredictSetting,
                actualGoodsDataSourceDetail,
                chartMap);

        // isFileDownload 최종 값
        boolean finalIsFileDownload = chartDataResults.stream()
                .map(DashboardChartData::dashboardChart) // DashboardChart 추출
                .filter(Objects::nonNull) // dashboardChart가 null인 경우 필터링
                .flatMap(chart -> Stream.of(chart.actualChartLines(), chart.forecastChartLines())) // actual과 forecast 추출
                .filter(Objects::nonNull) // DashboardChartLine이 null인 경우 필터링
                .map(DashboardChartLine::isPermitted) // isDownloadable 값 추출
                .anyMatch(permission -> permission != null && permission); // 하나라도 true인지 확인

        // 데이터 중 모두 권한이 없으면 다운로드 불가능, 하나라도 권한이 있으면 데이터 다운 가능
        if (!finalIsFileDownload) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "다운로드 가능한 데이터가 없습니다.");
        }

        // ConcurrentHashMap을 List<Map<Integer, DashboardChart>>로 변환
        List<Map<Integer, DashboardChart>> associatedCharts = chartMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .values().stream()
                .map(HashMap::new)
                .collect(Collectors.toList());

        // associatedCharts에서 DashboardChart 리스트 추출
        List<DashboardChart> finalCharts = associatedCharts.stream()
                .flatMap(map -> map.values().stream())
                .map(chart -> {
                    DashboardChartLine actualLines = chart.actualChartLines() != null
                            ? chart.actualChartLines()
                            : DashboardChartLine.of(null, null, null, null, null);
                    DashboardChartLine forecastLines = chart.forecastChartLines() != null
                            ? chart.forecastChartLines()
                            : DashboardChartLine.of(null, null, null, null, null);
                    return new DashboardChart(
                            chart.chartLineDescription(),
                            chart.chartLineType(),
                            actualLines,
                            forecastLines);
                })
                .sorted(Comparator
                        .comparing(
                                DashboardChart::chartLineType,
                                Comparator.nullsLast(
                                        Comparator.comparing(ChartPredictLineType::getCode, Comparator.nullsLast(Comparator.naturalOrder()))))
                        .thenComparing(
                                DashboardChart::chartLineDescription,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // DashboardChartDataTableType 결정
        DashboardChartDataTableType dataTableType = calculateDashboardChartDataTableType(chartPredictSettingDetails);

        // 날짜 범위 설정
        LocalDate endDate = baseDate.plusMonths(chartPredictSetting.getPredictPeriodMonthsCnt())
                .with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH).withDayOfMonth(1);
        // 월별 날짜 생성
        List<LocalDate> monthlyDates = generateMonthlyDates(startDate, endDate);

        GoodsDataSourceDetail forecastGoodsDataSourceDetail = chartDataResults.stream()
                .map(DashboardChartData::forecastGoodsDataSourceDetail)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        String currency = forecastGoodsDataSourceDetail != null ? forecastGoodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm() : null;
        String currentUnitCd = forecastGoodsDataSourceDetail != null ? forecastGoodsDataSourceDetail.getGoodsDataSourceUnitNm() : null;

        // 엑셀 파일 생성
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);

            int EXCEL_HEADER_ROW_INDEX = 0; // 엑셀 헤더 행 인덱스
            int EXCEL_HEADER_COLUMN_INDEX = 0; // 엑셀 헤더 열 인덱스
            int EXCEL_LABEL_COLUMN_INDEX = 1; // 엑셀 라벨 열 인덱스
            int MAX_COLUMNS_TO_AUTO_SIZE = 50; // 엑셀 최대 열 수

            String FORECAST_KEY = "Forecast";
            String ACTUAL_KEY = "Actual";
            String NOT_EXIST_VALUE = "-";

            // 헤더 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 병합 셀 스타일 생성
            CellStyle mergedCellStyle = workbook.createCellStyle();
            mergedCellStyle.setAlignment(HorizontalAlignment.CENTER);
            mergedCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 엑셀 헤더 생성
            Row headerRow = sheet.createRow(EXCEL_HEADER_ROW_INDEX);

            String headerUnit = currentUnitCd != null ? String.format("%s/%s", currency, currentUnitCd) : DASHBOARD_CHART_DATA_TABLE_HEADER_UNIT;
            headerRow.createCell(EXCEL_HEADER_COLUMN_INDEX).setCellValue(String.format(headerUnit));

            Cell headerCell = headerRow.createCell(EXCEL_HEADER_COLUMN_INDEX);
            headerCell.setCellStyle(headerStyle);
            headerCell.setCellValue(headerUnit);

            int headerStartIndex = dataTableType.getLayer(); // 타입별 헤더 시작 인덱스
            if (headerStartIndex > 1) {
                for (int i = 1; i < headerStartIndex; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellStyle(headerStyle);
                }
            }

            for (int i = headerStartIndex; i < monthlyDates.size() + headerStartIndex; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(DateUtil.toYyyyMm(monthlyDates.get(i - headerStartIndex)));
            }

            if (dataTableType == DashboardChartDataTableType.FORECAST_SINGLE_LAYER) {
                int EXCEL_DESCRIPTION_COLUMN_INDEX = 0; // Futures 설명 열 인덱스
                int rowIndex = 1;
                int EXCEL_COLUMN_WIDTH = 15 * 256; // 엑셀 열 너비


                List<DashboardChartLine> forecastChartLineList = finalCharts.stream()
                    .filter(chart -> chart.chartLineType() != null)
                    .map(DashboardChart::forecastChartLines)
                    .filter(dashboardChartLine -> dashboardChartLine.goodsDataSourceNo() != null)
                    .sorted(Comparator.comparing(DashboardChartLine::dataExplanation))
                    .toList();

                for(DashboardChartLine forecastChartLine : forecastChartLineList){
                    List<DashboardChartRawData> forecastRawData = forecastChartLine.rawData();
                    Row forecastRow = sheet.createRow(rowIndex);
                    forecastRow.createCell(EXCEL_DESCRIPTION_COLUMN_INDEX)
                        .setCellValue(forecastChartLine.dataExplanation() == null ? FORECAST_KEY : forecastChartLine.dataExplanation());
                    if (forecastRawData != null) {
                        for (int i = headerStartIndex; i < forecastRawData.size() + headerStartIndex; i++) {
                            Double forecastValue = forecastRawData.get(i - headerStartIndex).value();
                            if (forecastValue != null) {
                                forecastRow.createCell(i).setCellValue(forecastValue);
                            } else {
                                forecastRow.createCell(i).setCellValue(NOT_EXIST_VALUE);
                            }
                        }
                    }
                    rowIndex++;
                }

                // 세번째 행: Actual
                DashboardChart mainChart = finalCharts.stream()
                    .filter(chart -> chart.chartLineType() != null)
                    .findFirst()
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "차트 데이터가 없습니다."));
                DashboardChartLine actualChartLine = mainChart.actualChartLines();
                List<DashboardChartRawData> actualRawData = actualChartLine.rawData();
                Row actualRow = sheet.createRow(rowIndex);
                actualRow.createCell(EXCEL_DESCRIPTION_COLUMN_INDEX).setCellValue(ACTUAL_KEY);
                if (actualRawData != null) {
                    for (int i = headerStartIndex; i < actualRawData.size() + headerStartIndex; i++) {
                        Double actualValue = actualRawData.get(i - headerStartIndex).value();
                        if (actualValue != null) {
                            actualRow.createCell(i).setCellValue(actualValue);
                        } else {
                            actualRow.createCell(i).setCellValue(NOT_EXIST_VALUE);
                        }
                    }
                }
                rowIndex++;

                // 네번째 행부터: Futures
                List<DashboardChart> futuresCharts = finalCharts.stream()
                        .filter(chart -> {
                            return ChartPredictDataType.FUTURE.equals(chart.actualChartLines().dataType());
                        })
                        .toList();

                for (int i = rowIndex; i < futuresCharts.size() + rowIndex; i++) {
                    DashboardChartLine futuresChartLine = futuresCharts.get(i - rowIndex).actualChartLines();
                    List<DashboardChartRawData> futuresRawData = futuresChartLine.rawData();
                    if(futuresRawData == null) continue;

                    Row futuresRow = sheet.createRow(i);
                    futuresRow.createCell(EXCEL_DESCRIPTION_COLUMN_INDEX)
                            .setCellValue(futuresCharts.get(i - rowIndex).chartLineDescription());

                    for (int j = headerStartIndex; j < futuresRawData.size() + headerStartIndex; j++) {
                        Double futuresValue = futuresRawData.get(j - headerStartIndex).value();
                        if (futuresValue != null) {
                            futuresRow.createCell(j).setCellValue(futuresValue);
                        } else {
                            futuresRow.createCell(j).setCellValue(NOT_EXIST_VALUE);
                        }
                    }
                }

                // 열 너비 자동 조정
                int totalColumns = monthlyDates.size() + headerStartIndex;
                int columnsToAdjust = Math.min(totalColumns, MAX_COLUMNS_TO_AUTO_SIZE);
                log.info("Adjusting {} of {} columns", columnsToAdjust, totalColumns);
                for (int i = 0; i < columnsToAdjust; i++) {
                    sheet.autoSizeColumn(i);
                }
                for (int i = columnsToAdjust; i < totalColumns; i++) {
                    sheet.setColumnWidth(i, EXCEL_COLUMN_WIDTH);
                }
            } else if (dataTableType == DashboardChartDataTableType.FORECAST_DUAL_LAYER) {
                int EXCEL_DATA_ROW_INDEX = 1; // Data 행 인덱스

                // finalCharts 순회하여 각 그룹 처리
                for (DashboardChart chart : finalCharts) {
                    // Forecast 행
                    Row forecastRow = sheet.createRow(EXCEL_DATA_ROW_INDEX);
                    Cell mergedCell = forecastRow.createCell(EXCEL_HEADER_COLUMN_INDEX);
                    mergedCell.setCellValue(chart.chartLineDescription());
                    mergedCell.setCellStyle(mergedCellStyle);
                    forecastRow.createCell(EXCEL_LABEL_COLUMN_INDEX).setCellValue(FORECAST_KEY);

                    DashboardChartLine forecastChartLine = chart.forecastChartLines();
                    if (forecastChartLine != null) {
                        List<DashboardChartRawData> forecastRawData = forecastChartLine.rawData() != null ? forecastChartLine.rawData()
                                : Collections.emptyList();
                        for (int i = headerStartIndex; i < forecastRawData.size() + headerStartIndex; i++) {
                            Double forecastValue = forecastRawData.get(i - headerStartIndex).value();
                            if (forecastValue != null) {
                                forecastRow.createCell(i).setCellValue(forecastValue);
                            } else {
                                forecastRow.createCell(i).setCellValue(NOT_EXIST_VALUE);
                            }
                        }
                    }

                    // Actual 행
                    Row actualRow = sheet.createRow(EXCEL_DATA_ROW_INDEX + 1);
                    actualRow.createCell(EXCEL_LABEL_COLUMN_INDEX).setCellValue(ACTUAL_KEY);

                    DashboardChartLine actualChartLine = chart.actualChartLines();
                    if (actualChartLine != null) {
                        List<DashboardChartRawData> actualRawData = actualChartLine.rawData() != null ? actualChartLine.rawData()
                                : Collections.emptyList();
                        for (int i = headerStartIndex; i < actualRawData.size() + headerStartIndex; i++) {
                            Double actualValue = actualRawData.get(i - headerStartIndex).value();
                            if (actualValue != null) {
                                actualRow.createCell(i).setCellValue(actualValue);
                            } else {
                                actualRow.createCell(i).setCellValue(NOT_EXIST_VALUE);
                            }
                        }
                    }

                    // A열 병합 (rowIndex ~ rowIndex+1)
                    sheet.addMergedRegion(new CellRangeAddress(EXCEL_DATA_ROW_INDEX, EXCEL_DATA_ROW_INDEX + 1, EXCEL_HEADER_COLUMN_INDEX,
                            EXCEL_HEADER_COLUMN_INDEX));
                    RegionUtil.setBorderTop(BorderStyle.THIN, new CellRangeAddress(
                            EXCEL_DATA_ROW_INDEX, EXCEL_DATA_ROW_INDEX + 1, EXCEL_HEADER_COLUMN_INDEX, EXCEL_HEADER_COLUMN_INDEX), sheet);
                    RegionUtil.setBorderBottom(BorderStyle.THIN, new CellRangeAddress(
                            EXCEL_DATA_ROW_INDEX, EXCEL_DATA_ROW_INDEX + 1, EXCEL_HEADER_COLUMN_INDEX, EXCEL_HEADER_COLUMN_INDEX), sheet);
                    RegionUtil.setBorderLeft(BorderStyle.THIN, new CellRangeAddress(
                            EXCEL_DATA_ROW_INDEX, EXCEL_DATA_ROW_INDEX + 1, EXCEL_HEADER_COLUMN_INDEX, EXCEL_HEADER_COLUMN_INDEX), sheet);
                    RegionUtil.setBorderRight(BorderStyle.THIN, new CellRangeAddress(
                            EXCEL_DATA_ROW_INDEX, EXCEL_DATA_ROW_INDEX + 1, EXCEL_HEADER_COLUMN_INDEX, EXCEL_HEADER_COLUMN_INDEX), sheet);

                    EXCEL_DATA_ROW_INDEX += 2; // 다음 그룹으로 이동
                }
            }

            // 엑셀 파일을 ByteArrayOutputStream에 작성
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            } catch (IOException e) {
                throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, String.format("엑셀 파일 생성 실패 : %s", e.getMessage()));
            }
        } catch (IOException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, String.format("엑셀 파일 생성 실패 : %s", e.getMessage()));
        }
    }

    /**
     * 차트 주요 요인 분석 카테고리 조회
     *
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @return DashboardChartFactorCategoryRes
     */
    public DashboardChartFactorCategoryRes getChartFactorCategory(int companyNo, int goodsDataSourceNo, String predictDate, String date) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        return DashboardChartFactorCategoryRes.of(dashboardMapper.chartFactorCategory(queryWhere, predictDate, date));
    }

    /**
     * 차트 주요 요인 분석 조회
     *
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @return DashboardChartFactorRes
     */
    public DashboardChartFactorRes getChartFactors(int companyNo, int goodsDataSourceNo, String predictDate, String date, String explanatoryLevelNm) {
        // 사용자 정보 조회
        String currentDate = DateUtil.toYmd(LocalDate.now());
        customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        // 차트 주요 요인 분석 조회 (+)
        List<DashboardChartFactorMap> chartPlusFactorsMap = dashboardMapper.chartFactors(queryWhere, explanatoryLevelNm, predictDate,
                date, CHART_EXPLANATORY_PLUS_DIRECTION, CHART_EXPLANATORY_TOP_RANK_LIMIT);

        // 차트 주요 요인 분석 조회 (-)
        List<DashboardChartFactorMap> chartMinusFactorsMap = dashboardMapper.chartFactors(queryWhere, explanatoryLevelNm, predictDate,
                date, CHART_EXPLANATORY_MINUS_DIRECTION, CHART_EXPLANATORY_TOP_RANK_LIMIT);

        // Map -> Dto
        List<DashboardChartFactor> chartPlusFactors = chartPlusFactorsMap.stream()
                .map(DashboardChartFactor::of)
                .toList();

        // Map -> Dto
        List<DashboardChartFactor> chartMinusFactors = chartMinusFactorsMap.stream()
                .map(DashboardChartFactor::of)
                .toList();

        // 차트 주요 요인 분석 조회 요약 조회
        DashboardChartFactorExplanatoryMap chartFactorExplanatory = dashboardMapper
                .chartFactorExplanatory(queryWhere, explanatoryLevelNm, predictDate).orElse(null);

        String summary = chartFactorExplanatory != null ? chartFactorExplanatory.getExplanatoryDetailCntt() : null;

        return DashboardChartFactorRes.of(explanatoryLevelNm, summary, chartPlusFactors, chartMinusFactors);
    }

    /**
     * 차트 주요 요인 분석 라인 그래프 조회
     *
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @return DashboardChartFactorGraphRes
     */
    public DashboardChartFactorGraphRes getChartFactorGraph(int companyNo, int goodsDataSourceNo, String predictDate, String date,
            String explanatoryLevelNm, String predictionInputMnemonicCd, String marketIndexAnalysisTargetCd, String predictionInputMnemonicDetailCd) {
        LocalDate baseDate = date == null || date.isEmpty() ? LocalDate.now() : DateUtil.fromYearMonth(date);

        // 차트 예측 설정 조회
        ChartPredictSetting chartPredictSetting = chartPredictSettingRepository.findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "차트 예측 설정 조회 실패"));

        String currentDate = DateUtil.toYmd(LocalDate.now());
        customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "GoodsDataSourceDetail not found"));

        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        // 설명력 조회
        DashboardChartFactorExplanatoryMap chartFactorGraphExplanatory = dashboardMapper
                .chartFactorGraphExplanatory(queryWhere, explanatoryLevelNm, predictDate, predictionInputMnemonicCd)
                .orElse(null);

        String summary = chartFactorGraphExplanatory != null ? chartFactorGraphExplanatory.getExplanatoryDetailCntt() : null;

        // 그래프 Table 조회
        DashboardChartFactorGraphMap chartFactorGraph = dashboardMapper
                .chartFactorGraph(marketIndexAnalysisTargetCd, predictionInputMnemonicDetailCd)
                .orElse(null);

        // 그래프 RawData GoodsDataSourceNo
        Integer actualGoodsDataSourceNo = chartFactorGraph != null ? chartFactorGraph.getGoodsDataSourceNo() : null;

        // Date 기준
        GoodsDataSourceDetail actualGoodsDataSourceDetail = goodsDataSourceDetailRepository
                .findByGoodsDataSourceNoAndIsDeletedFalse(actualGoodsDataSourceNo)
                .orElse(null);

        if (actualGoodsDataSourceDetail != null) {

            GoodsData actualGoodsData = actualGoodsDataSourceDetail.getGoodsData();
            Goods actualGoods = actualGoodsData.getGoods();

            // 권한 조회
            Boolean isPermitted = permissionRes.getGoodsViewableMap(companyNo).getOrDefault(actualGoods.getGoodsNo(), false);

            String[] actualTableColumnList = actualGoodsDataSourceDetail.getTableColumnListNm().split(",");
            // 날짜 칼럼
            String actualDateColumnName = actualTableColumnList[MARKET_DATA_DATE_COLUMN_INDEX];

            // 기본 데이터 칼럼
            String actualDefaultDataColumnName = actualTableColumnList[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            LocalDate endDate = baseDate.with(TemporalAdjusters.lastDayOfMonth());

            LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH).withDayOfMonth(1);

            // 그래프 RawData 조회
            List<Map<String, Object>> graphRawData = getActualDataRows(actualGoodsDataSourceDetail, actualDateColumnName, startDate, endDate);

            // 월별 날짜 생성
            List<LocalDate> monthlyDates = generateMonthlyDates(startDate, endDate);

            // 월별 평균 계산
            List<DashboardChartRawData> monthlyAverages = isPermitted ? calculateMonthlyAverages(monthlyDates, graphRawData, actualDateColumnName,
                    actualDefaultDataColumnName, false, null) : null;

            DashboardChartFactorGraphMetadata graphMetadata = DashboardChartFactorGraphMetadata.of(
                    actualGoodsDataSourceDetail, chartPredictSetting);

            DashboardChartFactorGraphDatasource graphDatasource = DashboardChartFactorGraphDatasource.of(actualGoodsDataSourceDetail, isPermitted);

            return DashboardChartFactorGraphRes.of(summary, graphMetadata, monthlyAverages, graphDatasource);
        } else {

            DashboardChartFactorGraphMetadata graphMetadata = DashboardChartFactorGraphMetadata.of(chartPredictSetting);

            return DashboardChartFactorGraphRes.of(summary, graphMetadata);
        }
    }

    boolean isDataViewable(int companyNo, int goodsNo) {

        if (companyNo <= 0 || goodsNo <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Invalid parameters for data viewability check");
        }

        PermissionRes permissionRes = authorizationService.getPermissions();
        return permissionRes.getGoodsViewableMap(companyNo).getOrDefault(goodsNo, Boolean.FALSE);
    }
}
