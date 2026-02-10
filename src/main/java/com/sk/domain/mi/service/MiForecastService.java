package com.sk.domain.mi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.CombineUtil;
import com.sk.common.utils.DateBindingUtil;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import com.sk.domain.mi.dto.response.forecast.ForecastChartFactorGraphDatasource;
import com.sk.domain.mi.dto.response.forecast.ForecastChartFactorGraphRes;
import com.sk.domain.mi.dto.response.forecast.ForecastChartRes;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.ForecastTargetRes;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactor;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorExplanatoryMap;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorGraphMap;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorMap;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorRes;
import com.sk.domain.mi.dto.response.forecast.charts.ForecastChartData;
import com.sk.domain.mi.dto.response.forecast.charts.ForecastDataCache;
import com.sk.domain.mi.dto.response.forecast.charts.ForecastDataSource;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChart;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChartLine;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChartRawData;
import com.sk.domain.mi.dto.response.forecast.charts.metaData.ForecastChartFactorGraphMetadata;
import com.sk.domain.mi.dto.response.forecast.charts.metaData.ForecastMetadata;
import com.sk.domain.mi.dto.response.forecast.report.ForecastReport;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastRecordHeaderRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastRecordRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportDetailRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastSummary;
import com.sk.domain.mi.dto.response.forecast.table.ForecastTableRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.chart.ChartPredictDataType;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;
import com.sk.domain.shared.enums.dashboard.DashboardChartDataTableType;
import com.sk.domain.shared.enums.forecast.MnemonicCode;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import com.sk.domain.shared.mapper.dynamic.ForecastMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tcm.UiComponentSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiForecastService {

    private static final String DASHBOARD_CHART_LAST_UPDATE_DATE_LIMIT = "1";
    private static final String FORECAST_AVAILABLE_DATE_LIMIT = "10";
    private static final int MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX = 0;
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1;
    private static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final int FORECAST_CHART_CACHE_TTL = 10;
    private static final String FORECAST_CHART_CACHE_KEY = "forecast_chart:%d:%s:%s:%s";
    private static final String MARKET_DATA_PREDICT_DATE_COLUMN_NAME = "prdc_fulf_dt";
    private static final String MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME = "prdc_crtr_dt";
    private static final String MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME = "prdc_rslt_dt";
    private static final String MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME = "prdc_rslt_val";
    private static final String MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME = "prdc_crtr_val";
    private static final String MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME = "mnmnc_cd";
    private static final String MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME = "mrkt_idx_anly_tgt_cd";
    private static final String FORECAST_RECORD_SCREEN_CODE = "MI_FORECAST";
    private static final String FORECAST_RECORD_COMPONENT_TYPE = "TABLE";
    private static final String FORECAST_RECORD_COMPONENT_CODE = "RECORDS";
    private static final String FORECAST_MODEL_ACCURACY_COMPONENT_TYPE = "CARD";
    private static final String FORECAST_MODEL_ACCURACY_COMPONENT_CODE = "MODEL_ACCURACY";
    private static final String FORECAST_MODEL_ACCURACY_ITEM_KEY = "cumulativeHitRate";
    private static final String FORECAST_CHART_SCREEN_CODE = "MI_FORECAST";
    private static final String FORECAST_CHART_COMPONENT_TYPE = "CHART";
    private static final String FORECAST_CHART_COMPONENT_CODE = "MODEL_CHART_LINE";
    private static final String FORECAST_CHART_LINE_STRATEGY_KEY = "lineValueStrategy";
    private static final String LINE_STRATEGY_PAST_ACTUAL_FUTURE_PREDICT = "PAST_ACTUAL_FUTURE_PREDICT";
    private static final String ERR_CHART_NOT_CONFIGURED = "설정된 차트가 없습니다.";
    private static final int CHART_EXPLANATORY_TOP_RANK_LIMIT = 5;
    private static final String CHART_EXPLANATORY_PLUS_DIRECTION = "+";
    private static final String CHART_EXPLANATORY_MINUS_DIRECTION = "-";
    private static final int DASHBOARD_CHART_START_DATE_FROM_MONTH = 11;
    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;
    private final UiComponentSettingRepository uiComponentSettingRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final ForecastMapper forecastMapper;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final AuthorizationService authorizationService;
    private final FileService fileService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static <T> T visibleValue(boolean applyFilter, Map<String, Boolean> visibilityByKey,
        String key, T value) {
        if (!applyFilter) {
            return value;
        }
        Boolean isVisible = visibilityByKey.get(key);
        if (isVisible == null || isVisible) {
            return value;
        }
        return null;
    }

    private LineValueStrategy getLineValueStrategy(Integer companyNo) {
        Optional<UiComponentSetting> setting =
            uiComponentSettingRepository
                .findFirstByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndItemKeyAndIsDeletedFalse(
                    companyNo,
                    FORECAST_CHART_SCREEN_CODE,
                    FORECAST_CHART_COMPONENT_TYPE,
                    FORECAST_CHART_COMPONENT_CODE,
                    FORECAST_CHART_LINE_STRATEGY_KEY);
        if (setting.isEmpty() && companyNo != 0) {
            setting =
                uiComponentSettingRepository
                    .findFirstByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndItemKeyAndIsDeletedFalse(
                        0,
                        FORECAST_CHART_SCREEN_CODE,
                        FORECAST_CHART_COMPONENT_TYPE,
                        FORECAST_CHART_COMPONENT_CODE,
                        FORECAST_CHART_LINE_STRATEGY_KEY);
        }

        if (setting.isEmpty() || setting.get().getSettingValue() == null
            || setting.get().getSettingValue().isBlank()) {
            return LineValueStrategy.defaultStrategy();
        }

        try {
            LineValueStrategy parsed = objectMapper.readValue(
                setting.get().getSettingValue(),
                LineValueStrategy.class);
            return parsed != null ? parsed : LineValueStrategy.defaultStrategy();
        } catch (Exception e) {
            return LineValueStrategy.defaultStrategy();
        }
    }

    private List<ForecastChart> applyLineValueStrategy(List<ForecastChart> charts, LocalDate today) {
        return charts.stream()
            .map(chart -> {
                ForecastChartLine actual = chart.actualChartLines();
                ForecastChartLine forecast = chart.forecastChartLines();

                ForecastChartLine maskedActual = maskChartLineByCutoff(actual, today, true);
                ForecastChartLine maskedForecast = maskChartLineByCutoff(forecast, today, false);

                return new ForecastChart(
                    chart.chartName(),
                    chart.chartLineDescription(),
                    chart.chartLineType(),
                    maskedActual,
                    maskedForecast);
            })
            .toList();
    }

    private ForecastChartLine maskChartLineByCutoff(ForecastChartLine line, LocalDate today,
        boolean keepPastOrToday) {
        if (line == null || line.rawData() == null) {
            return line;
        }

        List<ForecastChartRawData> rawData = line.rawData();
        int todayIndex = findTodayIndex(rawData, today);
        List<ForecastChartRawData> masked;
        if (todayIndex < 0) {
            masked = keepPastOrToday ? List.of() : rawData;
        } else {
            masked = keepPastOrToday
                ? rawData.subList(0, todayIndex)
                : rawData.subList(todayIndex, rawData.size());
        }

        return ForecastChartLine.of(
            line.isPermitted(),
            line.dataType(),
            line.goodsDataSourceNo(),
            masked.isEmpty() ? List.of() : new ArrayList<>(masked),
            line.dataExplanation()
        );
    }

    private int findTodayIndex(List<ForecastChartRawData> rawData, LocalDate today) {
        int low = 0;
        int high = rawData.size() - 1;
        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            LocalDate start = parseRangeStart(rawData.get(mid));
            if (start == null) {
                return 0;
            }
            if (start.isAfter(today)) {
                high = mid - 1;
            } else {
                result = mid;
                low = mid + 1;
            }
        }
        return result;
    }

    private LocalDate parseRangeStart(ForecastChartRawData data) {
        LocalDate rangeStart = DateUtil.parseDate(data.startDate());
        if (rangeStart == null) {
            rangeStart = DateUtil.parseDate(data.resultDate());
        }
        return rangeStart;
    }

    /**
     * Forecast 차트 기간 조회
     *
     * @param customerCompanyNo 고객사 번호
     * @param xAxisUnit         x축 단위 (1D, 1W, 1M, 1Q, 1Y)
     * @param period            전체기간 (1W, 1M, 3M, 6M, YTD, 1Y, ALL)
     * @return ForecastChartRes
     */
    public ForecastChartRes getChart(Integer customerCompanyNo, String xAxisUnit, String period) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                customerCompanyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", customerCompanyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(
            customerCompany);

        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        AxisUnit axisUnit = AxisUnit.from(xAxisUnit);
        TotalPeriod totalPeriod = TotalPeriod.from(period);

        LocalDate today = LocalDate.now();
        LocalDate baseDate = calculateBaseDate(today,
            chartPredictSetting.getPredictPeriodMonthsCnt(),
            chartPredictSetting.getPredictPeriodUnitSeparateCd());
        LocalDate startDate = calculateStartDate(today, totalPeriod);

        validatePeriodLength(startDate, baseDate, axisUnit);

        List<ForecastChartRes.ForecastChartDateItem> dateList = buildDateList(startDate, baseDate,
            axisUnit);

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        GoodsDataSourceDetail[] actualGoodsDataSourceDetail = {null};
        Map<Integer, ForecastChart> chartMap = new HashMap<>();

        List<ForecastChartData> chartDataResults = processChartData(
            chartPredictSettingDetails,
            permissionRes,
            customerCompanyNo,
            startDate,
            baseDate,
            dateList,
            chartPredictSetting,
            actualGoodsDataSourceDetail,
            chartMap,
            axisUnit.label,
            totalPeriod.label,
            baseDate);

        List<Map<Integer, ForecastChart>> associatedCharts = chartMap.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .values().stream()
            .map(HashMap::new)
            .collect(Collectors.toList());

        LocalDate lastUpdated = chartDataResults.stream()
            .map(ForecastChartData::lastUpdated)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);

        DashboardChartDataTableType dataTableType = calculateDashboardChartDataTableType(
            chartPredictSettingDetails);

        ForecastMetadata dashboardMetadata = ForecastMetadata.of(
            customerCompany.getMarketIndexAnalysisTargetCd(),
            lastUpdated,
            Objects.requireNonNull(chartDataResults.stream()
                .map(ForecastChartData::forecastGoodsDataSourceDetail)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)),
            DateUtil.toYearMonth(baseDate),
            dataTableType,
            chartPredictSetting.getPredictTargetRepresentSpecNm());

        List<ForecastChart> finalCharts = associatedCharts.stream()
            .flatMap(map -> map.values().stream())
            .map(chart -> {
                ForecastChartLine actualLines = chart.actualChartLines() != null
                    ? chart.actualChartLines()
                    : ForecastChartLine.of(null, null, null, null, null);
                ForecastChartLine forecastLines = chart.forecastChartLines() != null
                    ? chart.forecastChartLines()
                    : ForecastChartLine.of(null, null, null, null, null);
                return new ForecastChart(
                    chart.chartName(),
                    chart.chartLineDescription(),
                    chart.chartLineType(),
                    actualLines,
                    forecastLines);
            })
            .sorted(Comparator
                .comparing(
                    ForecastChart::chartLineType,
                    Comparator.nullsLast(
                        Comparator.comparing(ChartPredictLineType::getCode,
                            Comparator.nullsLast(Comparator.naturalOrder()))))
                .thenComparing(c -> c.actualChartLines().goodsDataSourceNo() == null,
                    Comparator.nullsLast(Comparator.comparing(b -> !b)))
                .thenComparing(
                    ForecastChart::chartLineDescription,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LineValueStrategy lineValueStrategy = getLineValueStrategy(customerCompanyNo);
        if (lineValueStrategy.isPastActualFuturePredict()) {
            finalCharts = applyLineValueStrategy(finalCharts, today);
        }

        List<ForecastDataSource> dashboardDataSources = chartDataResults.stream()
            .map(ForecastChartData::dashboardDataSource)
            .filter(Objects::nonNull)
            .toList();

        boolean finalIsFileDownload = chartDataResults.stream()
            .map(ForecastChartData::dashboardChart)
            .filter(Objects::nonNull)
            .flatMap(chart -> Stream.of(chart.actualChartLines(), chart.forecastChartLines()))
            .filter(Objects::nonNull)
            .map(ForecastChartLine::isPermitted)
            .anyMatch(permission -> permission != null && permission);

        return ForecastChartRes.builder()
            .xAxisUnit(axisUnit.label)
            .period(totalPeriod.label)
            .startDate(DateUtil.toYmd(startDate))
            .endDate(DateUtil.toYmd(baseDate))
            .dateList(dateList)
            .charts(finalCharts)
            .chartsDataSource(dashboardDataSources)
            .isFileDownload(finalIsFileDownload)
            .metadata(dashboardMetadata)
            .build();
    }

    /**
     * 데이터 존재 여부 조회
     *
     * @param companyNo 회사 번호
     * @return boolean
     */
    public boolean getDataExists(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        return chartPredictSettingRepository.existsByCustomerCompany(customerCompany);
    }

    /**
     * Forecast 기본 정보 조회
     *
     * @param companyNo 회사 번호
     * @return ForecastInfoRes
     */
    public ForecastInfoRes getForecastInfo(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(
            customerCompany);

        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 차트선예측값여부 chrt_line_prdc_val_yn = N, 선물여부 futr_yn = N
        List<ForecastTargetRes> targetList = chartPredictSettingDetails.stream()
            .filter(detail -> Boolean.FALSE.equals(detail.getIsChartLinePredictVal()))
            .filter(detail -> Boolean.FALSE.equals(detail.getIsFuture()))
            .map(ForecastTargetRes::of)
            .toList();

        // 최근 업데이트 일시(예측 데이터중 가장 최근 업데이트 날짜)
        List<LocalDate> lastUpdatedList = chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .map(chartPredictSettingDetail -> {
                GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();

                List<Map<String, Object>> lastUpdateDateData = getLastUpdateDateData(
                    goodsDataSource
                );

                if (lastUpdateDateData.isEmpty()) {
                    return null;
                }

                return DateUtil.parseDate(
                    lastUpdateDateData.get(0).get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME));
            })
            .filter(Objects::nonNull)
            .toList();

        LocalDate lastUpdateDt = lastUpdatedList.stream()
            .max(LocalDate::compareTo)
            .orElse(null);

        List<ForecastRecordHeaderRes> recordHeaders =
            buildForecastRecordHeadersForCompany(companyNo);
        Boolean modelAccuracyVisible = getModelAccuracyVisible(companyNo);
        return ForecastInfoRes.of(chartPredictSetting, targetList, lastUpdateDt, recordHeaders,
            modelAccuracyVisible);
    }

    /**
     * Forecast record 목록 조회
     *
     * @param companyNo         회사 번호
     * @param goodsDataSourceNo 데이터 소스 번호(선택)
     * @param startDate         조회 시작일자(YYYYMMDD)
     * @param endDate           조회 종료일자(YYYYMMDD)
     * @param order             정렬 순서(NEWEST/OLDEST)
     * @param page              페이지번호
     * @param size              페이지당항목수
     * @return PaginationRes<ForecastRecordRes>
     */
    public PaginationRes<ForecastRecordRes> getForecastRecords(int companyNo,
        Integer goodsDataSourceNo, String startDate, String endDate, String order, int page,
        int size) {
        LocalDate start = DateUtil.parseDate(startDate);
        LocalDate end = DateUtil.parseDate(endDate);
        if (start == null || end == null || start.isAfter(end)) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "조회기간이 올바르지 않습니다.");
        }

        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes =
            chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails =
            chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .filter(detail -> goodsDataSourceNo.equals(
                    detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
                .toList();
        }

        List<ChartPredictSettingDetail> predictDetails = chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .toList();

        if (predictDetails.isEmpty()) {
            return PaginationRes.toRes(new PageImpl<>(List.of(), PageRequest.of(page - 1, size), 0));
        }

        String dateRangeWhere = String.format("%s BETWEEN '%s' AND '%s'",
            MARKET_DATA_PREDICT_DATE_COLUMN_NAME, startDate, endDate);
        List<String> conditionList = predictDetails.stream()
            .map(ChartPredictSettingDetail::getGoodsDataSourceDetail)
            .filter(Objects::nonNull)
            .map(GoodsDataSourceDetail::getQueryConditionCntt)
            .filter(Objects::nonNull)
            .filter(condition -> !condition.isBlank())
            .distinct()
            .toList();
        String combinedBaseWhere = conditionList.isEmpty()
            ? ""
            : conditionList.stream()
                .map(condition -> "(" + condition + ")")
                .collect(java.util.stream.Collectors.joining(" OR "));
        String whereClause = combinedBaseWhere.isBlank()
            ? dateRangeWhere
            : "(" + combinedBaseWhere + ") AND " + dateRangeWhere;

        boolean isTwoPeriodCompany = companyNo == 2015 || companyNo == 2013;
        String groupByColumns = isTwoPeriodCompany
            ? String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME)
            : String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);

        String orderByDirection =
            "OLDEST".equalsIgnoreCase(order) ? "ASC" : "DESC";
        String orderByColumns = isTwoPeriodCompany
            ? String.format("%s %s, %s DESC, %s ASC",
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME, orderByDirection,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME)
            : String.format("%s %s, %s DESC",
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME, orderByDirection,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);

        String columns = isTwoPeriodCompany
            ? String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME,
                "AVG(" + MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME + ") as avgPredictionResultVal")
            : String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                "MIN(" + MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME + ") as startPredictionResultDt",
                "MAX(" + MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME + ") as endPredictionResultDt",
                "AVG(" + MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME + ") as avgPredictionResultVal",
                "MAX(" + MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME + ") as predictionCriteriaVal");

        int offset = Math.max(0, (page - 1) * size);
        String limitClause = String.format("%d, %d", offset, size);

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource("midb")
                .tableName("tmm_bztgtprdcrsltdatam")
                .columns(columns)
                .whereClause(whereClause)
                .groupByColumns(groupByColumns)
                .orderByColumns(orderByColumns)
                .limit(limitClause)
                .build());

        String countSubQuery = String.format(
            "(SELECT %s FROM %s WHERE %s GROUP BY %s) t",
            groupByColumns,
            "tmm_bztgtprdcrsltdatam",
            whereClause,
            groupByColumns);
        List<Map<String, Object>> countRows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource("midb")
                .tableName(countSubQuery)
                .columns("count(*) as cnt")
                .build());
        int totalElements = 0;
        if (!countRows.isEmpty()) {
            String countValue = getStringValue(countRows.get(0).get("cnt"));
            if (countValue != null) {
                try {
                    totalElements = Integer.parseInt(countValue);
                } catch (NumberFormatException ignored) {
                    totalElements = 0;
                }
            }
        }

        List<ForecastRecordRes> results = companyNo == 2020
            ? buildForecastRecordResults(rows, chartPredictSettingDetails, predictDetails, companyNo,
                goodsDataSourceNo)
            : buildForecastRecordResultsForTwoPeriodCompany(rows, chartPredictSettingDetails,
                predictDetails, companyNo, goodsDataSourceNo);

        Pageable pageable = PageRequest.of(page - 1, size);
        return PaginationRes.toRes(new PageImpl<>(results, pageable, totalElements));
    }

    /**
     * Forecast record 엑셀 다운로드
     *
     * @param companyNo         회사 번호
     * @param goodsDataSourceNo 데이터 소스 번호(선택)
     * @param startDate         조회 시작일자(YYYYMMDD)
     * @param endDate           조회 종료일자(YYYYMMDD)
     * @param order             정렬 순서(NEWEST/OLDEST)
     * @return byte[] 엑셀 파일 데이터
     */
    public byte[] getForecastRecordsFileDownload(int companyNo, Integer goodsDataSourceNo,
        String startDate, String endDate, String order) throws IOException {
        LocalDate start = DateUtil.parseDate(startDate);
        LocalDate end = DateUtil.parseDate(endDate);
        if (start == null || end == null || start.isAfter(end)) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "조회기간이 올바르지 않습니다.");
        }

        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes =
            chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails =
            chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .filter(detail -> goodsDataSourceNo.equals(
                    detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
                .toList();
        }

        List<ChartPredictSettingDetail> predictDetails = chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .toList();

        if (predictDetails.isEmpty()) {
            return createEmptyForecastRecordExcel(companyNo);
        }

        String dateRangeWhere = String.format("%s BETWEEN '%s' AND '%s'",
            MARKET_DATA_PREDICT_DATE_COLUMN_NAME, startDate, endDate);
        List<String> conditionList = predictDetails.stream()
            .map(ChartPredictSettingDetail::getGoodsDataSourceDetail)
            .filter(Objects::nonNull)
            .map(GoodsDataSourceDetail::getQueryConditionCntt)
            .filter(Objects::nonNull)
            .filter(condition -> !condition.isBlank())
            .distinct()
            .toList();
        String combinedBaseWhere = conditionList.isEmpty()
            ? ""
            : conditionList.stream()
                .map(condition -> "(" + condition + ")")
                .collect(java.util.stream.Collectors.joining(" OR "));
        String whereClause = combinedBaseWhere.isBlank()
            ? dateRangeWhere
            : "(" + combinedBaseWhere + ") AND " + dateRangeWhere;

        boolean isTwoPeriodCompany = companyNo == 2015 || companyNo == 2013;
        String groupByColumns = isTwoPeriodCompany
            ? String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME)
            : String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);

        String orderByDirection =
            "OLDEST".equalsIgnoreCase(order) ? "ASC" : "DESC";
        String orderByColumns = isTwoPeriodCompany
            ? String.format("%s %s, %s DESC, %s ASC",
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME, orderByDirection,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME)
            : String.format("%s %s, %s DESC",
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME, orderByDirection,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);

        String columns = isTwoPeriodCompany
            ? String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME,
                "AVG(" + MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME + ") as avgPredictionResultVal")
            : String.join(", ",
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                "MIN(" + MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME + ") as startPredictionResultDt",
                "MAX(" + MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME + ") as endPredictionResultDt",
                "AVG(" + MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME + ") as avgPredictionResultVal",
                "MAX(" + MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME + ") as predictionCriteriaVal");

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource("midb")
                .tableName("tmm_bztgtprdcrsltdatam")
                .columns(columns)
                .whereClause(whereClause)
                .groupByColumns(groupByColumns)
                .orderByColumns(orderByColumns)
                .build());

        List<ForecastRecordRes> results = companyNo == 2020
            ? buildForecastRecordResults(rows, chartPredictSettingDetails, predictDetails, companyNo,
                goodsDataSourceNo)
            : buildForecastRecordResultsForTwoPeriodCompany(rows, chartPredictSettingDetails,
                predictDetails, companyNo, goodsDataSourceNo);

        return createForecastRecordExcel(companyNo, results);
    }

    /**
     * Forecast daily record 엑셀 다운로드
     *
     * @param predictionFulfillDt  예측수행일자(YYYYMMDD)
     * @param predictionCriteriaDt 예측기준일자(YYYYMMDD)
     * @return byte[] 엑셀 파일 데이터
     */
    public byte[] getForecastDailyRecordsFileDownload(String predictionFulfillDt,
        String predictionCriteriaDt, String marketIndexAnalysisTargetCd, String mnemonicCd)
        throws IOException {
        LocalDate fulfillDate = DateUtil.parseDate(predictionFulfillDt);
        LocalDate criteriaDate = DateUtil.parseDate(predictionCriteriaDt);
        if (fulfillDate == null || criteriaDate == null) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "일자 형식이 올바르지 않습니다.");
        }

        String whereClause = String.join(" AND ",
            "mrkt_idx_anly_tgt_cd = '" + marketIndexAnalysisTargetCd + "'",
            "mnmnc_cd = '" + mnemonicCd + "'",
            "prdc_fulf_dt = '" + predictionFulfillDt + "'",
            "prdc_crtr_dt = '" + predictionCriteriaDt + "'");

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource("midb")
                .tableName("tmm_bztgtprdcrsltdatam")
                .columns(String.join(", ",
                    "prdc_crtr_dt",
                    "prdc_rslt_dt",
                    "prdc_rslt_val"))
                .whereClause(whereClause)
                .orderByColumns("prdc_rslt_dt ASC")
                .build());

        return createForecastDailyRecordExcel(criteriaDate, rows);
    }

    private List<ForecastRecordRes> buildForecastRecordResults(
        List<Map<String, Object>> rows,
        List<ChartPredictSettingDetail> chartPredictSettingDetails,
        List<ChartPredictSettingDetail> predictDetails,
        int companyNo, Integer goodsDataSourceNo) {

        Map<Integer, GoodsDataSourceDetail> actualDetailByAssociatedNo =
            chartPredictSettingDetails.stream()
                .filter(detail -> !detail.getIsChartLinePredictVal())
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .collect(Collectors.toMap(
                    detail -> detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo(),
                    ChartPredictSettingDetail::getGoodsDataSourceDetail,
                    (existing, replacement) -> existing));
        Map<String, Integer> associatedNoByPredictKey = predictDetails.stream()
            .filter(detail -> detail.getGoodsDataSourceDetail() != null)
            .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
            .collect(Collectors.toMap(
                detail -> buildPredictKey(detail.getGoodsDataSourceDetail().getQueryConditionCntt()),
                detail -> detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo(),
                (existing, replacement) -> existing));

        Map<String, Boolean> visibilityByKey = buildForecastRecordVisibilityMap(companyNo);
        boolean applyVisibilityFilter = !visibilityByKey.isEmpty();

        Map<String, ForecastSummary> summaryCache = new HashMap<>();

        return rows.stream()
            .map(row -> {
                String marketIndexAnalysisTargetCd =
                    getStringValue(row.get(MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME));
                String predictionFulfillDt =
                    getStringValue(row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME));
                String mnemonicCd =
                    getStringValue(row.get(MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME));
                String predictionCriteriaDt =
                    getStringValue(row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME));
                String startPredictionResultDt =
                    getStringValue(row.get("startPredictionResultDt"));
                String endPredictionResultDt =
                    getStringValue(row.get("endPredictionResultDt"));
                String avgPredictionResultValStr =
                    getStringValue(row.get("avgPredictionResultVal"));
                String predictionCriteriaValStr =
                    getStringValue(row.get("predictionCriteriaVal"));

                Double predictionResultVal = toScaledDouble(avgPredictionResultValStr);
                Double predictionCriteriaVal = toScaledDouble(predictionCriteriaValStr);
                DiffResult diffResult =
                    calculateDiffResult(predictionCriteriaValStr, avgPredictionResultValStr);

                Double actualVal = null;
                Integer associatedNo = goodsDataSourceNo;
                if (associatedNo == null) {
                    associatedNo = associatedNoByPredictKey.get(
                        buildPredictKey(marketIndexAnalysisTargetCd, mnemonicCd));
                }
                GoodsDataSourceDetail actualDetail =
                    associatedNo != null ? actualDetailByAssociatedNo.get(associatedNo) : null;
                if (actualDetail != null) {
                    actualVal = getActualAverageValue(actualDetail, startPredictionResultDt,
                        endPredictionResultDt);
                }

                String directionVal = null;
                if (predictionResultVal != null && predictionCriteriaVal != null) {
                    int comparison = predictionResultVal.compareTo(predictionCriteriaVal);
                    if (comparison > 0) {
                        directionVal = "UPWARD";
                    } else if (comparison == 0) {
                        directionVal = "NEUTRAL";
                    } else {
                        directionVal = "DOWNWARD";
                    }
                }

                Boolean isHit = null;
                if (actualVal != null && predictionResultVal != null && predictionCriteriaVal != null) {
                    boolean predictionUp = predictionResultVal.compareTo(predictionCriteriaVal) > 0;
                    boolean predictionEqual = predictionResultVal.compareTo(predictionCriteriaVal) == 0;
                    boolean predictionDown = predictionResultVal.compareTo(predictionCriteriaVal) < 0;

                    boolean actualUp = actualVal.compareTo(predictionCriteriaVal) > 0;
                    boolean actualEqual = actualVal.compareTo(predictionCriteriaVal) == 0;
                    boolean actualDown = actualVal.compareTo(predictionCriteriaVal) < 0;

                    isHit = (predictionUp && actualUp)
                        || (predictionEqual && actualEqual)
                        || (predictionDown && actualDown);
                }

                String predictChartDetailExpn = null;
                if (mnemonicCd != null) {
                    MnemonicCode mnemonicCode = MnemonicCode.findByCode(mnemonicCd);
                    if (mnemonicCode != null) {
                        predictChartDetailExpn = mnemonicCode.getPredictChartDetailExpression();
                    }
                }
                ForecastSummary report = resolveForecastSummary(
                    summaryCache,
                    predictDetails,
                    marketIndexAnalysisTargetCd,
                    predictionFulfillDt,
                    mnemonicCd);

                String predictionResultDt = companyNo == 2020
                    ? formatDateRangeShort(startPredictionResultDt, endPredictionResultDt)
                    : null;

                return ForecastRecordRes.builder()
                    .marketIndexAnalysisTargetCd(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "marketIndexAnalysisTargetCd",
                        marketIndexAnalysisTargetCd))
                    .predictionFulfillDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionFulfillDt",
                        predictionFulfillDt))
                    .mnemonicCd(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "mnemonicCd", mnemonicCd))
                    .predictChartDetailExpn(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictChartDetailExpn",
                        predictChartDetailExpn))
                    .associatedGoodsDataSourceNo(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "associatedGoodsDataSourceNo",
                        goodsDataSourceNo))
                    .endPredictionResultDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "endPredictionResultDt",
                        endPredictionResultDt))
                    .startPredictionResultDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "startPredictionResultDt",
                        startPredictionResultDt))
                    .predictionResultDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultDt",
                        predictionResultDt))
                    .predictionCriteriaDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionCriteriaDt",
                        predictionCriteriaDt))
                    .predictionResultVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultVal",
                        predictionResultVal))
                    .predictionResultVal1(null)
                    .predictionResultVal2(null)
                    .predictionCriteriaVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionCriteriaVal",
                        predictionCriteriaVal))
                    .predictionResultDiffVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultDiffVal",
                        diffResult.diffValue()))
                    .predictionResultDiffRate(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultDiffRate",
                        diffResult.diffRate()))
                    .actualVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "actualVal", actualVal))
                    .actualVal2(null)
                    .report(report)
                    .directionVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "directionVal", directionVal))
                    .isHit(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "isHit", isHit))
                    .build();
            })
            .toList();
    }

    private List<ForecastRecordRes> buildForecastRecordResultsForTwoPeriodCompany(
        List<Map<String, Object>> rows,
        List<ChartPredictSettingDetail> chartPredictSettingDetails,
        List<ChartPredictSettingDetail> predictDetails,
        int companyNo, Integer goodsDataSourceNo) {

        Map<Integer, GoodsDataSourceDetail> actualDetailByAssociatedNo =
            chartPredictSettingDetails.stream()
                .filter(detail -> !detail.getIsChartLinePredictVal())
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .collect(Collectors.toMap(
                    detail -> detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo(),
                    ChartPredictSettingDetail::getGoodsDataSourceDetail,
                    (existing, replacement) -> existing));
        Map<String, Integer> associatedNoByPredictKey = predictDetails.stream()
            .filter(detail -> detail.getGoodsDataSourceDetail() != null)
            .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
            .collect(Collectors.toMap(
                detail -> buildPredictKey(detail.getGoodsDataSourceDetail().getQueryConditionCntt()),
                detail -> detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo(),
                (existing, replacement) -> existing));

        Map<String, Boolean> visibilityByKey = buildForecastRecordVisibilityMap(companyNo);
        boolean applyVisibilityFilter = !visibilityByKey.isEmpty();

        record ForecastKey(String target, String fulfillDt, String mnemonic, String criteriaDt) {}

        Map<ForecastKey, List<Map<String, Object>>> grouped = rows.stream()
            .collect(Collectors.groupingBy(row -> new ForecastKey(
                getStringValue(row.get(MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME)),
                getStringValue(row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME)),
                getStringValue(row.get(MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME)),
                getStringValue(row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME)))));

        Map<String, ForecastSummary> summaryCache = new HashMap<>();

        return grouped.entrySet().stream()
            .map(entry -> {
                ForecastKey key = entry.getKey();
                List<Map<String, Object>> groupRows = entry.getValue().stream()
                    .sorted(Comparator.comparing(
                        row -> getStringValue(row.get(MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME)),
                        Comparator.nullsLast(String::compareTo)))
                    .toList();

                String predictionResultDt1 =
                    groupRows.isEmpty()
                        ? null
                        : getStringValue(groupRows.get(0).get(
                            MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME));
                String predictionResultDt2 =
                    groupRows.size() < 2
                        ? null
                        : getStringValue(groupRows.get(1).get(
                            MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME));

                Double predictionResultVal1 = groupRows.isEmpty()
                    ? null
                    : toScaledDouble(getStringValue(groupRows.get(0).get("avgPredictionResultVal")));
                Double predictionResultVal2 = groupRows.size() < 2
                    ? null
                    : toScaledDouble(getStringValue(groupRows.get(1).get("avgPredictionResultVal")));

                Integer associatedNo = goodsDataSourceNo;
                if (associatedNo == null) {
                    associatedNo = associatedNoByPredictKey.get(
                        buildPredictKey(key.target(), key.mnemonic()));
                }
                GoodsDataSourceDetail actualDetail =
                    associatedNo != null ? actualDetailByAssociatedNo.get(associatedNo) : null;

                Double actualVal = null;
                Double actualVal2 = null;
                if (actualDetail != null) {
                    if (companyNo == 2013) {
                        LocalDate start = DateUtil.parseDate(predictionResultDt1);
                        LocalDate end = DateUtil.parseDate(predictionResultDt2);
                        if (start != null && end != null) {
                            LocalDate endOfSecond =
                                end.plusMonths(1).minusDays(1);
                            actualVal = getActualAverageValue(actualDetail,
                                DateUtil.toYmd(start),
                                DateUtil.toYmd(endOfSecond));
                        }
                    } else if (companyNo == 2015) {
                        LocalDate second = DateUtil.parseDate(predictionResultDt2);
                        if (second != null) {
                            LocalDate endOfSecond =
                                second.plusMonths(3).minusDays(1);
                            actualVal2 = getActualAverageValue(actualDetail,
                                DateUtil.toYmd(second),
                                DateUtil.toYmd(endOfSecond));
                        }
                    }
                }

                String predictionResultDt =
                    companyNo == 2013
                        ? formatMonthShort(predictionResultDt1)
                        : null;
                String startPredictionResultDt =
                    companyNo == 2013
                        ? formatMonthShort(predictionResultDt1)
                        : (companyNo == 2015 ? formatQuarterShort(predictionResultDt1) : null);
                ForecastSummary report = resolveForecastSummary(
                    summaryCache,
                    predictDetails,
                    key.target(),
                    key.fulfillDt(),
                    key.mnemonic());

                return ForecastRecordRes.builder()
                    .marketIndexAnalysisTargetCd(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "marketIndexAnalysisTargetCd",
                        key.target()))
                    .predictionFulfillDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionFulfillDt",
                        key.fulfillDt()))
                    .mnemonicCd(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "mnemonicCd", key.mnemonic()))
                    .predictChartDetailExpn(null)
                    .associatedGoodsDataSourceNo(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "associatedGoodsDataSourceNo",
                        goodsDataSourceNo))
                    .endPredictionResultDt(null)
                    .startPredictionResultDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "startPredictionResultDt",
                        startPredictionResultDt))
                    .predictionResultDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultDt",
                        predictionResultDt))
                    .predictionCriteriaDt(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionCriteriaDt",
                        key.criteriaDt()))
                    .predictionResultVal(null)
                    .predictionResultVal1(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultVal1",
                        predictionResultVal1))
                    .predictionResultVal2(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "predictionResultVal2",
                        predictionResultVal2))
                    .predictionCriteriaVal(null)
                    .predictionResultDiffVal(null)
                    .predictionResultDiffRate(null)
                    .actualVal(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "actualVal", actualVal))
                    .actualVal2(visibleValue(
                        applyVisibilityFilter, visibilityByKey, "actualVal2", actualVal2))
                    .report(report)
                    .directionVal(null)
                    .isHit(null)
                    .isScheduled(null)
                    .isNew(null)
                    .build();
            })
            .toList();
    }

    private ForecastSummary resolveForecastSummary(
        Map<String, ForecastSummary> cache,
        List<ChartPredictSettingDetail> predictDetails,
        String marketIndexAnalysisTargetCd,
        String predictionFulfillDt,
        String mnemonicCd) {
        if (predictionFulfillDt == null || mnemonicCd == null) {
            return null;
        }
        String cacheKey =
            marketIndexAnalysisTargetCd + "|" + predictionFulfillDt + "|" + mnemonicCd;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        ChartPredictSettingDetail matchedDetail = predictDetails.stream()
            .filter(detail -> Boolean.TRUE.equals(detail.getIsChartLinePredictVal()))
            .filter(detail -> detail.getGoodsDataSourceDetail() != null)
            .filter(detail -> {
                String query = detail.getGoodsDataSourceDetail().getQueryConditionCntt();
                return query != null && query.contains(mnemonicCd);
            })
            .findFirst()
            .orElse(null);
        if (matchedDetail == null) {
            cache.put(cacheKey, null);
            return null;
        }

        GoodsDataSourceDetail goodsDataSourceDetail = matchedDetail.getGoodsDataSourceDetail();
        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
        List<DashboardChartSummaryMap> chartSummaries =
            forecastMapper.chartSummary(queryWhere, predictionFulfillDt, null, null);
        if (chartSummaries == null || chartSummaries.isEmpty()) {
            cache.put(cacheKey, null);
            return null;
        }
        DashboardChartSummaryMap matchedSummary = chartSummaries.stream()
            .filter(summary -> Objects.equals(marketIndexAnalysisTargetCd,
                summary.getMarketIndexAnalysisTargetCd()))
            .filter(summary -> Objects.equals(mnemonicCd, summary.getMnemonicCd()))
            .findFirst()
            .orElse(null);
        DashboardChartSummaryMap decodedSummary = decodeSummaryPath(matchedSummary);
        ForecastSummary report = decodedSummary != null ? ForecastSummary.of(decodedSummary) : null;
        cache.put(cacheKey, report);
        return report;
    }

    private String formatMonth(String startYmd, String endYmd) {
        String start = toMonthLabel(startYmd);
        String end = toMonthLabel(endYmd);
        if (start == null && end == null) {
            return null;
        }
        if (start != null && end != null && !start.equals(end)) {
            return start + " - " + end;
        }
        return start != null ? start : end;
    }

    private String formatMonthShort(String ymd) {
        LocalDate date = DateUtil.parseDate(ymd);
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern("yy-MM"));
    }

    private String formatQuarterShort(String ymd) {
        LocalDate date = DateUtil.parseDate(ymd);
        if (date == null) {
            return null;
        }
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        int year = date.getYear() % 100;
        return String.format("%02d-%dQ", year, quarter);
    }

    private String formatDateRangeShort(String startYmd, String endYmd) {
        String start = toShortDateLabel(startYmd);
        String end = toShortDateLabel(endYmd);
        if (start == null && end == null) {
            return null;
        }
        if (start != null && end != null && !start.equals(end)) {
            return start + " - " + end;
        }
        return start != null ? start : end;
    }

    private String toShortDateLabel(String ymd) {
        LocalDate date = DateUtil.parseDate(ymd);
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern("yy/MM/dd"));
    }


    private String toMonthLabel(String ymd) {
        LocalDate date = DateUtil.parseDate(ymd);
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private byte[] createForecastRecordExcel(int companyNo,
        List<ForecastRecordRes> records) throws IOException {
        List<ForecastRecordHeaderRes> headers = buildForecastRecordHeaders(companyNo);
        List<ForecastRecordHeaderRes> visibleHeaders = headers.stream()
            .filter(h -> h.isVisible() == null || h.isVisible())
            .toList();

        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("forecast_records");

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            for (int col = 0; col < visibleHeaders.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(visibleHeaders.get(col).headerText());
            }

            for (ForecastRecordRes record : records) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                for (ForecastRecordHeaderRes header : visibleHeaders) {
                    Cell cell = row.createCell(col++);
                    Object value = getForecastRecordFieldValue(record, header.columnKey());
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else if (value instanceof Boolean bool) {
                        cell.setCellValue(bool.toString());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createEmptyForecastRecordExcel(int companyNo) throws IOException {
        return createForecastRecordExcel(companyNo, List.of());
    }

    private byte[] createForecastDailyRecordExcel(LocalDate criteriaDate,
        List<Map<String, Object>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("daily_forecast");

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"주차", "기준일자", "예측일자", "예측 가격", "적요"};
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.M.d");
            for (Map<String, Object> row : rows) {
                String criteriaYmd = getStringValue(row.get("prdc_crtr_dt"));
                String resultYmd = getStringValue(row.get("prdc_rslt_dt"));
                String resultVal = getStringValue(row.get("prdc_rslt_val"));

                LocalDate criteria = DateUtil.parseDate(criteriaYmd);
                LocalDate result = DateUtil.parseDate(resultYmd);

                String weekLabel = null;
                if (criteria != null && result != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(criteria, result);
                    long week = days / 7 + 1;
                    if (week < 1) {
                        week = 1;
                    }
                    weekLabel = week + "W";
                }

                String criteriaFormatted = criteria != null ? criteria.format(dateFormatter) : null;
                String resultFormatted = result != null ? result.format(dateFormatter) : null;
                Double price = toScaledDouble(resultVal);

                Row dataRow = sheet.createRow(rowIndex++);
                int col = 0;
                dataRow.createCell(col++).setCellValue(weekLabel != null ? weekLabel : "");
                dataRow.createCell(col++).setCellValue(criteriaFormatted != null ? criteriaFormatted : "");
                dataRow.createCell(col++).setCellValue(resultFormatted != null ? resultFormatted : "");
                if (price != null) {
                    dataRow.createCell(col++).setCellValue(price);
                } else {
                    dataRow.createCell(col++).setCellValue("");
                }
                dataRow.createCell(col).setCellValue("");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Object getForecastRecordFieldValue(ForecastRecordRes record, String columnKey) {
        return switch (columnKey) {
            case "marketIndexAnalysisTargetCd" -> record.marketIndexAnalysisTargetCd();
            case "predictionFulfillDt" -> record.predictionFulfillDt();
            case "mnemonicCd" -> record.mnemonicCd();
            case "predictChartDetailExpn" -> record.predictChartDetailExpn();
            case "associatedGoodsDataSourceNo" -> record.associatedGoodsDataSourceNo();
            case "endPredictionResultDt" -> record.endPredictionResultDt();
            case "startPredictionResultDt" -> record.startPredictionResultDt();
            case "predictionResultDt" -> record.predictionResultDt() != null
                ? record.predictionResultDt()
                : toPredictionResultRange(record.startPredictionResultDt(),
                    record.endPredictionResultDt());
            case "predictionCriteriaDt" -> record.predictionCriteriaDt();
            case "predictionResultVal" -> record.predictionResultVal();
            case "predictionResultVal1" -> record.predictionResultVal1();
            case "predictionResultVal2" -> record.predictionResultVal2();
            case "predictionCriteriaVal" -> record.predictionCriteriaVal();
            case "predictionResultDiffVal" -> record.predictionResultDiffVal();
            case "predictionResultDiffRate" -> record.predictionResultDiffRate();
            case "actualVal" -> record.actualVal();
            case "actualVal2" -> record.actualVal2();
            case "directionVal" -> record.directionVal();
            case "isHit" -> record.isHit();
            case "isScheduled" -> record.isScheduled();
            case "isNew" -> record.isNew();
            default -> null;
        };
    }

    private String toPredictionResultRange(String start, String end) {
        boolean hasStart = start != null && !start.isBlank();
        boolean hasEnd = end != null && !end.isBlank();
        if (!hasStart && !hasEnd) {
            return null;
        }
        if (hasStart && hasEnd) {
            return start + " - " + end;
        }
        return hasStart ? start : end;
    }

    public ForecastTableRes getForecastTable(int customerCompanyNo, Integer goodsDataSourceNo,
        String startDate,
        String endDate, String order, int page, int size) {
        ForecastTableData tableData = buildForecastTableData(customerCompanyNo, goodsDataSourceNo,
            startDate, endDate, order);
        List<List<Object>> rows = tableData.rows();
        int totalElements = rows.size();
        Pageable pageable = PageRequest.of(page - 1, size);
        int fromIndex = Math.min((page - 1) * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<List<Object>> pagedRows = rows.subList(fromIndex, toIndex);
        PaginationRes<List<Object>> pagedTableDataList =
            PaginationRes.toRes(new PageImpl<>(pagedRows, pageable, totalElements));

        return ForecastTableRes.builder()
            .startDate(startDate)
            .endDate(endDate)
            .goodsDataSourceNo(goodsDataSourceNo)
            .order(tableData.sortOrder().name())
            .tableHeaderList(tableData.headerList())
            .tableDataList(pagedTableDataList)
            .build();
    }

    public byte[] getForecastTableFileDownload(int customerCompanyNo, Integer goodsDataSourceNo,
        String startDate,
        String endDate, String order) throws IOException {
        ForecastTableData tableData = buildForecastTableData(customerCompanyNo, goodsDataSourceNo,
            startDate, endDate, order);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("forecast_table");

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            for (int col = 0; col < tableData.headerList().size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(tableData.headerList().get(col));
            }

            for (List<Object> rowData : tableData.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData.get(col);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ForecastTableData buildForecastTableData(int customerCompanyNo,
        Integer goodsDataSourceNo, String startDate,
        String endDate, String order) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                customerCompanyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", customerCompanyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(
            customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();
        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .filter(detail -> goodsDataSourceNo.equals(
                    detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
                .toList();
        }

        LocalDate start = DateUtil.parseDate(startDate);
        LocalDate end = DateUtil.parseDate(endDate);
        if (start == null || end == null || start.isAfter(end)) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "조회기간이 올바르지 않습니다.");
        }

        AxisUnit axisUnit = axisUnitFrom(chartPredictSetting.getDefaultPeriodUnitSeparateCd());
        List<ForecastChartRes.ForecastChartDateItem> dateList = buildDateList(start, end, axisUnit);

        GoodsDataSourceDetail[] actualGoodsDataSourceDetail = {null};
        Map<Integer, ForecastChart> chartMap = new HashMap<>();

        processChartData(
            chartPredictSettingDetails,
            permissionRes,
            customerCompanyNo,
            start,
            end,
            dateList,
            chartPredictSetting,
            actualGoodsDataSourceDetail,
            chartMap,
            axisUnit.label,
            String.format("%s_%s", startDate, endDate),
            end);

        List<Map<Integer, ForecastChart>> associatedCharts = chartMap.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .values().stream()
            .map(HashMap::new)
            .collect(Collectors.toList());

        List<ForecastChart> finalCharts = associatedCharts.stream()
            .flatMap(map -> map.values().stream())
            .map(chart -> {
                ForecastChartLine actualLines = chart.actualChartLines() != null
                    ? chart.actualChartLines()
                    : ForecastChartLine.of(null, null, null, null, null);
                ForecastChartLine forecastLines = chart.forecastChartLines() != null
                    ? chart.forecastChartLines()
                    : ForecastChartLine.of(null, null, null, null, null);
                return new ForecastChart(
                    chart.chartName(),
                    chart.chartLineDescription(),
                    chart.chartLineType(),
                    actualLines,
                    forecastLines);
            })
            .sorted(Comparator
                .comparing(
                    ForecastChart::chartLineType,
                    Comparator.nullsLast(
                        Comparator.comparing(ChartPredictLineType::getCode,
                            Comparator.nullsLast(Comparator.naturalOrder()))))
                .thenComparing(c -> c.actualChartLines().goodsDataSourceNo() == null,
                    Comparator.nullsLast(Comparator.comparing(b -> !b)))
                .thenComparing(
                    ForecastChart::chartLineDescription,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        List<String> headerList = new ArrayList<>();
        headerList.add("Date");
        List<ForecastTableColumn> columns = new ArrayList<>();
        for (ForecastChart chart : finalCharts) {
            String name = chart.chartName() != null ? chart.chartName() : chart.chartLineDescription();
            headerList.add(String.format("%s Forecast", name));
            headerList.add(String.format("%s Actual", name));
            columns.add(
                new ForecastTableColumn(chart.forecastChartLines(), chart.actualChartLines()));
        }

        Map<ForecastChartLine, Map<String, Double>> lineValueMap = new LinkedHashMap<>();
        for (ForecastTableColumn column : columns) {
            if (column.actualLine != null) {
                lineValueMap.put(column.actualLine, toValueMap(column.actualLine));
            }
            if (column.forecastLine != null) {
                lineValueMap.put(column.forecastLine, toValueMap(column.forecastLine));
            }
        }

        List<List<Object>> rows = new ArrayList<>();
        for (ForecastChartRes.ForecastChartDateItem item : dateList) {
            List<Object> values = new ArrayList<>();
            values.add(item.label());
            for (ForecastTableColumn column : columns) {
                values.add(getValue(column.actualLine, item.label(), lineValueMap));
                values.add(getValue(column.forecastLine, item.label(), lineValueMap));
            }
            rows.add(values);
        }

        TableSortOrder sortOrder = TableSortOrder.from(order);
        if (sortOrder == TableSortOrder.NEWEST) {
            Collections.reverse(rows);
        }

        return new ForecastTableData(headerList, rows, sortOrder);
    }

    private DashboardChartDataTableType calculateDashboardChartDataTableType(
        List<ChartPredictSettingDetail> chartPredictSettingDetails) {
        DashboardChartDataTableType dataTableType = DashboardChartDataTableType.FORECAST_SINGLE_LAYER;
        List<ChartPredictSettingDetail> chartPredictSettingDetailPredictList = chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .toList();
        if (chartPredictSettingDetailPredictList.size() >= 2) {
            Integer associateDataSource = null;
            for (ChartPredictSettingDetail predict : chartPredictSettingDetailPredictList) {
                if (predict.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                    .equals(predict.getGoodsDataSourceDetail().getGoodsDataSourceNo())) {
                    continue;
                }
                if (associateDataSource == null) {
                    associateDataSource = predict.getAssociatedGoodsDataSourceDetail()
                        .getGoodsDataSourceNo();
                    continue;
                }
                if (!associateDataSource.equals(
                    predict.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo())) {
                    dataTableType = DashboardChartDataTableType.FORECAST_DUAL_LAYER;
                }
            }
        }
        return dataTableType;
    }

    private List<ForecastChartData> processChartData(
        List<ChartPredictSettingDetail> chartPredictSettingDetails,
        PermissionRes permissionRes,
        Integer customerCompanyNo,
        LocalDate startDate,
        LocalDate endDate,
        List<ForecastChartRes.ForecastChartDateItem> dateList,
        ChartPredictSetting chartPredictSetting,
        GoodsDataSourceDetail[] actualGoodsDataSourceDetail,
        Map<Integer, ForecastChart> chartMap,
        String xAxisUnit,
        String period,
        LocalDate baseDate) {
        return chartPredictSettingDetails.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ChartPredictSettingDetail::getIsChartLinePredictVal))
            .map(chartPredictSettingDetail -> {
                GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();
                GoodsData goodsData = goodsDataSource.getGoodsData();
                Goods goods = goodsData.getGoods();

                boolean isPermitted = permissionRes.getGoodsDownloadableMap(customerCompanyNo)
                    .getOrDefault(goods.getGoodsNo(), false);

                ChartPredictDataType chartPredictDataType;
                boolean isPredict = Boolean.TRUE.equals(chartPredictSettingDetail.getIsChartLinePredictVal());
                boolean isFuture = Boolean.TRUE.equals(chartPredictSettingDetail.getIsFuture());
                if (isFuture) {
                    chartPredictDataType = ChartPredictDataType.FUTURE;
                } else if (isPredict) {
                    chartPredictDataType = ChartPredictDataType.FORECAST;
                    isPermitted = true;
                } else {
                    chartPredictDataType = ChartPredictDataType.ACTUAL;
                    if (actualGoodsDataSourceDetail[0] == null) {
                        actualGoodsDataSourceDetail[0] = goodsDataSource;
                    }
                }

                String[] tableColumnList = Arrays.stream(
                        goodsDataSource.getTableColumnListNm().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
                String dateColumnName = tableColumnList[MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX];
                String defaultDataColumnName = tableColumnList[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

                LocalDate alignedEndDate = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
                    case W -> endDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                    case D -> endDate;
                    default -> endDate.with(TemporalAdjusters.lastDayOfMonth());
                };
                LocalDate alignedStartDate = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
                    case W -> startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    case D -> startDate;
                    default -> startDate.withDayOfMonth(1);
                };

                String cacheKey = getForecastChartCacheKey(
                    chartPredictSettingDetail.getChartPredictSettingDetailNo(),
                    xAxisUnit,
                    period,
                    DateUtil.toYmd(baseDate));

                List<ForecastChartRawData> periodAverages = null;
                LocalDate lastUpdated = null;
                ForecastDataSource dashboardDataSource = null;
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    try {
                        ForecastDataCache dashboardDataCache = objectMapper.readValue(cachedData,
                            ForecastDataCache.class);
                        periodAverages = dashboardDataCache.periodAverages();
                        lastUpdated = dashboardDataCache.lastUpdated();
                        dashboardDataSource = dashboardDataCache.dataSource();
                    } catch (Exception e) {
                        redisTemplate.delete(cacheKey);
                    }
                }

                if (periodAverages == null) {
                    List<Map<String, Object>> rawData =
                        isPredict
                            ? getForecastDataRows(goodsDataSource, dateColumnName, alignedStartDate,
                            alignedEndDate)
                            : getActualDataRows(goodsDataSource, dateColumnName, alignedStartDate,
                                alignedEndDate);
                    List<ForecastChartRes.ForecastChartDateItem> effectiveDateList = dateList;
                    if (AxisUnit.ONE_DAY.label.equalsIgnoreCase(xAxisUnit)) {
                        effectiveDateList = buildDateListFromRawData(rawData, dateColumnName);
                    }

                    if (isPredict && !rawData.isEmpty()) {
                        String latestPredictDate = rawData.stream()
                            .map(row -> (String) row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME))
                            .filter(Objects::nonNull)
                            .max(String::compareTo)
                            .orElse(null);
                        if (latestPredictDate != null) {
                            lastUpdated = DateUtil.parseDate(latestPredictDate);
                        }
                        dashboardDataSource = ForecastDataSource.of(goodsDataSource,
                            chartPredictSettingDetail);
                    }

                    periodAverages = calculatePeriodAverages(
                        effectiveDateList,
                        rawData,
                        dateColumnName,
                        defaultDataColumnName,
                        isPredict);

                    ForecastDataCache dashboardDataCache = ForecastDataCache.of(periodAverages,
                        lastUpdated, dashboardDataSource);
                    saveForecastChartCache(cacheKey, dashboardDataCache);
                }

                ForecastChartLine dashboardChartLine = ForecastChartLine.of(isPermitted,
                    chartPredictDataType,
                    goodsDataSource.getGoodsDataSourceNo(), periodAverages,
                    chartPredictSettingDetail.getPredictChartDetailExpn());

                Integer goodsDataSourceNo = chartPredictSettingDetail.getAssociatedGoodsDataSourceDetail()
                    .getGoodsDataSourceNo();
                String mnemonicName = MnemonicCode.findByCode(extractMnemonicCode(
                    chartPredictSettingDetail.getGoodsDataSourceDetail().getQueryConditionCntt(),
                    Boolean.TRUE.equals(chartPredictSettingDetail.getIsChartLinePredictVal()))).getPredictChartDetailExpression();
                ForecastChart dashboardChart = chartMap.compute(goodsDataSourceNo,
                    (key, existingChart) -> {
                        if (existingChart == null) {
                            return ForecastChart.builder()
                                .chartName(mnemonicName)
                                .chartLineDescription(chartPredictSettingDetail.getPredictChartDetailExpn())
                                .chartLineType(chartPredictSettingDetail.getChartPredictLineSeparateCd())
                                .actualChartLines(isPredict ? null : dashboardChartLine)
                                .forecastChartLines(isPredict ? dashboardChartLine : null)
                                .build();
                        }
                        return ForecastChart.builder()
                            .chartName(existingChart.chartName() != null
                                ? existingChart.chartName()
                                : mnemonicName)
                            .chartLineDescription(existingChart.chartLineDescription() != null
                                ? existingChart.chartLineDescription()
                                : chartPredictSettingDetail.getPredictChartDetailExpn())
                            .chartLineType(existingChart.chartLineType() != null
                                ? existingChart.chartLineType()
                                : chartPredictSettingDetail.getChartPredictLineSeparateCd())
                            .actualChartLines(isPredict
                                ? existingChart.actualChartLines()
                                : dashboardChartLine)
                            .forecastChartLines(isPredict
                                ? dashboardChartLine
                                : existingChart.forecastChartLines())
                            .build();
                    });

                return ForecastChartData.of(lastUpdated, dashboardDataSource,
                    actualGoodsDataSourceDetail[0], dashboardChart);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private List<ForecastChartRes.ForecastChartDateItem> buildDateListFromRawData(
        List<Map<String, Object>> rawData, String dateColumnName) {
        if (rawData == null || rawData.isEmpty() || dateColumnName == null) {
            return List.of();
        }
        return rawData.stream()
            .map(row -> DateUtil.parseDate(row.get(dateColumnName)))
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .map(date -> ForecastChartRes.ForecastChartDateItem.builder()
                .label(DateTimeFormatter.ofPattern("MM/dd").format(date))
                .startDate(DateUtil.toYmd(date))
                .endDate(DateUtil.toYmd(date))
                .unit(AxisUnit.ONE_DAY.label)
                .build())
            .toList();
    }

    public List<ForecastChartRawData> calculatePeriodAverages(
        List<ForecastChartRes.ForecastChartDateItem> dateList,
        List<Map<String, Object>> rawData, String dateColumnName, String defaultDataColumnName,
        boolean isPredict) {
        List<ForecastChartRawData> periodAverages = new ArrayList<>();

        for (ForecastChartRes.ForecastChartDateItem item : dateList) {
            LocalDate rangeStart;
            LocalDate rangeEnd;
            if (item.startDate() == null || item.endDate() == null) {
                LocalDate day = DateUtil.parseDate(item.label());
                rangeStart = day;
                rangeEnd = day;
            } else {
                rangeStart = DateUtil.parseDate(item.startDate());
                rangeEnd = DateUtil.parseDate(item.endDate());
            }

            List<Double> values = new ArrayList<>();
            String latestPredictDate = null;
            int matchedRows = 0;
            for (Map<String, Object> row : rawData) {
                LocalDate rowDate = DateUtil.parseDate(row.get(dateColumnName));
                if (rowDate == null || rowDate.isBefore(rangeStart) || rowDate.isAfter(rangeEnd)) {
                    continue;
                }
                matchedRows++;
                if (isPredict) {
                    String rowPredictDate = getStringValue(row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME));
                    if (rowPredictDate != null && (latestPredictDate == null
                        || rowPredictDate.compareTo(latestPredictDate) > 0)) {
                        latestPredictDate = rowPredictDate;
                    }
                }
                Object val = row.get(defaultDataColumnName);
                Double valueData;
                if (val == null) {
                    valueData = null;
                } else if (val instanceof Double d) {
                    valueData = d;
                } else if (val instanceof BigDecimal bd) {
                    valueData = bd.doubleValue();
                } else {
                    try {
                        valueData = Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        valueData = null;
                    }
                }
                values.add(valueData);
            }

            String label = item.label();
            String resultDateLabel = label;
            String resultStartDate = switch (item.unit()) {
                case "1M" -> DateUtil.toYmd(rangeEnd.withDayOfMonth(1));
                case "1Q" -> {
                    int startMonth = ((rangeEnd.getMonthValue() - 1) / 3) * 3 + 1;
                    yield DateUtil.toYmd(LocalDate.of(rangeEnd.getYear(), startMonth, 1));
                }
                case "1Y" -> DateUtil.toYmd(LocalDate.of(rangeEnd.getYear(), 1, 1));
                case "1W" -> DateUtil.toYmd(
                    rangeEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                default -> DateUtil.toYmd(rangeStart);
            };
            if (AxisUnit.ONE_DAY.label.equals(item.unit())) {
                LocalDate labelDate = DateUtil.parseDate(label);
                if (labelDate != null) {
                    resultDateLabel = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
                        .format(labelDate);
                }
            }
            if (isPredict) {
                log.debug("Forecast predictDate by range. label={}, rangeStart={}, rangeEnd={}, matchedRows={}, latestPredictDate={}",
                    item.label(), rangeStart, rangeEnd, matchedRows, latestPredictDate);
            }
            String predictDate = isPredict ? latestPredictDate : null;
            ForecastChartRawData dashboardChartRawData;
            if (values.isEmpty()) {
                if (isPredict) {
                    dashboardChartRawData = ForecastChartRawData.ofForecast(resultDateLabel,
                        resultStartDate, predictDate,
                        null);
                } else {
                    dashboardChartRawData = ForecastChartRawData.ofActual(resultDateLabel,
                        resultStartDate, null);
                }
            } else {
                double average = values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                BigDecimal bd = BigDecimal.valueOf(average)
                    .setScale(2, RoundingMode.HALF_UP);
                if (isPredict) {
                    dashboardChartRawData = ForecastChartRawData.ofForecast(resultDateLabel,
                        resultStartDate, predictDate,
                        bd.doubleValue());
                } else {
                    dashboardChartRawData = ForecastChartRawData.ofActual(resultDateLabel,
                        resultStartDate, bd.doubleValue());
                }
            }
            periodAverages.add(dashboardChartRawData);
        }
        return periodAverages;
    }

    private List<LocalDate> generateGraphDates(LocalDate startDate, LocalDate endDate,
        ChartPredictSetting chartPredictSetting) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
            case W -> {
                int diff = DayOfWeek.WEDNESDAY.getValue() - startDate.getDayOfWeek().getValue();
                yield startDate.plusDays(diff);
            }
            case D -> startDate;
            default -> startDate.withDayOfMonth(1);
        };

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
                case W -> current.plusWeeks(1);
                case D -> current.plusDays(1);
                default -> current.plusMonths(1).withDayOfMonth(1);
            };
        }
        return dates;
    }

    private List<ForecastChartRawData> calculateGraphAverages(List<LocalDate> periodDates,
        List<Map<String, Object>> rawData, String dateColumnName, String defaultDataColumnName,
        ChartPredictSetting chartPredictSetting) {
        List<ForecastChartRawData> periodAverages = new ArrayList<>();

        DateTimeFormatter yyyyMMFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<Double>> periodValues = new TreeMap<>();
        for (Map<String, Object> row : rawData) {
            LocalDate localDate = DateUtil.parseDate(row.get(dateColumnName));
            if (localDate == null) {
                continue;
            }
            String period = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
                case W -> {
                    int diff = DayOfWeek.WEDNESDAY.getValue() - localDate.getDayOfWeek().getValue();
                    yield DateUtil.toYmdHyphen(localDate.plusDays(diff));
                }
                case D -> DateUtil.toYmdHyphen(localDate);
                default -> DateUtil.toYyyyMm(localDate);
            };

            Object val = row.get(defaultDataColumnName);
            Double valueData;
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
                    valueData = null;
                }
            }

            periodValues.computeIfAbsent(period, k -> new ArrayList<>()).add(valueData);
        }

        for (LocalDate periodDate : periodDates) {
            String periodStr = switch (chartPredictSetting.getDefaultPeriodUnitSeparateCd()) {
                case W -> {
                    int diff = DayOfWeek.WEDNESDAY.getValue() - periodDate.getDayOfWeek().getValue();
                    yield DateUtil.toYmdHyphen(periodDate.plusDays(diff));
                }
                case D -> DateUtil.toYmdHyphen(periodDate);
                default -> periodDate.format(yyyyMMFormatter);
            };
            List<Double> values = periodValues.getOrDefault(periodStr, Collections.emptyList());

            ForecastChartRawData chartRawData;
            if (values.isEmpty()) {
                chartRawData = ForecastChartRawData.ofActual(periodStr, null);
            } else {
                double average = values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                BigDecimal bd = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
                chartRawData = ForecastChartRawData.ofActual(periodStr, bd.doubleValue());
            }
            periodAverages.add(chartRawData);
        }
        return periodAverages;
    }

    private List<Map<String, Object>> getForecastDataRows(
        GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName,
        LocalDate startDate, LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null
            || endDate == null) {
            return Collections.emptyList();
        }

        String endDateValue = DateUtil.toYmd(endDate);
        String startDateValue = DateUtil.toYmd(startDate);

        String dbQueryConditionCntt = goodsDataSourceDetail.getQueryConditionCntt();
        String dbColumnList = goodsDataSourceDetail.getTableColumnListNm();

        String pattern = "([a-zA-Z_]+)\\s*=\\s*'([^']*)'\\s*and\\s*([a-zA-Z_]+)\\s*=\\s*'([^']*)'";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(dbQueryConditionCntt);

        String key1;
        String value1;
        String key2;
        String value2;

        if (matcher.find()) {
            key1 = matcher.group(1);
            value1 = matcher.group(2);
            key2 = matcher.group(3);
            value2 = matcher.group(4);
        } else {
            throw new IllegalArgumentException("Invalid condition format: " + dbQueryConditionCntt);
        }

        String columns = String.format("%s, %s", dbColumnList,
            MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
        String tableName = "tmm_bztgtprdcrsltdatam";
        String whereClause = String.format(
            "%s BETWEEN '%s' AND '%s' AND %s = '%s' AND %s = '%s'",
            dateColumnName, startDateValue, endDateValue,
            key1, value1,
            key2, value2);
        String orderByColumns = String.format("%s, %s", dateColumnName,
            MARKET_DATA_PREDICT_DATE_COLUMN_NAME);

        return mybatisDynamicQueryService.getDynamicForecast(
            DynamicDataDetailReq.ofOrderBy(goodsDataSourceDetail.getTargetDbNm(), tableName,
                columns, whereClause, orderByColumns));
    }

    private List<Map<String, Object>> getActualDataRows(GoodsDataSourceDetail goodsDataSourceDetail,
        String dateColumnName,
        LocalDate startDate, LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null
            || endDate == null) {
            return Collections.emptyList();
        }

        SchemaMetadata meta = mybatisDynamicQueryService.getMeta(
            DynamicDataReq.ofMetaQuery(goodsDataSourceDetail));
        Pair<String, String> range = DateBindingUtil.range(meta, startDate, endDate);
        String whereClause = CombineUtil.createBetweenClause(dateColumnName, range.getLeft(),
            range.getRight());

        return mybatisDynamicQueryService.getDynamicData(
            DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }

    private String getForecastChartCacheKey(Integer chartPredictSettingDetailNo, String xAxisUnit,
        String period, String baseDate) {
        return String.format(FORECAST_CHART_CACHE_KEY, chartPredictSettingDetailNo, xAxisUnit,
            period, baseDate);
    }

    private void saveForecastChartCache(String key, ForecastDataCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue()
                .set(key, json, Duration.ofSeconds(FORECAST_CHART_CACHE_TTL));
        } catch (Exception e) {
            // ignore serialization failure for cache
        }
    }

    private AxisUnit axisUnitFrom(PeriodUnitSeparateCd unit) {
        if (unit == null) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "기간 단위가 올바르지 않습니다.");
        }
        return switch (unit) {
            case D -> AxisUnit.ONE_DAY;
            case W -> AxisUnit.ONE_WEEK;
            case M -> AxisUnit.ONE_MONTH;
            case Q -> AxisUnit.ONE_QUARTER;
            case Y -> AxisUnit.ONE_YEAR;
        };
    }

    private Map<String, Double> toValueMap(ForecastChartLine line) {
        if (line == null || line.rawData() == null) {
            return Collections.emptyMap();
        }
        Map<String, Double> map = new HashMap<>();
        for (ForecastChartRawData raw : line.rawData()) {
            map.put(raw.resultDate(), raw.value());
        }
        return map;
    }

    private Double getValue(ForecastChartLine line, String label,
        Map<ForecastChartLine, Map<String, Double>> lineValueMap) {
        if (line == null) {
            return null;
        }
        Map<String, Double> values = lineValueMap.getOrDefault(line, Collections.emptyMap());
        return values.get(label);
    }

    /**
     * Forecast report 상세 조회
     *
     * @param companyNo                   회사 번호
     * @param marketIndexAnalysisTargetCd 시장지수분석대상코드
     * @param predictionFulfillDt         예측수행일자
     * @param mnemonicCd                  MNEMONIC코드
     * @param predictionCriteriaDt        예측기준일자
     * @return ForecastReportDetailRes
     */
    public ForecastReportDetailRes getForecastReportDetail(int companyNo,
        String marketIndexAnalysisTargetCd,
        String predictionFulfillDt, String mnemonicCd, String predictionCriteriaDt) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(
            customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        for (ChartPredictSettingDetail detail : chartPredictSettingDetails) {
            if (Boolean.FALSE.equals(detail.getIsChartLinePredictVal())) {
                continue;
            }
            GoodsDataSourceDetail goodsDataSourceDetail = detail.getGoodsDataSourceDetail();
            if (goodsDataSourceDetail == null
                || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                continue;
            }
            if (!goodsDataSourceDetail.getQueryConditionCntt().contains(mnemonicCd)) {
                continue;
            }

            DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

            String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
            List<DashboardChartSummaryMap> chartSummaries =
                forecastMapper.chartSummary(queryWhere, predictionFulfillDt, null, null);
            DashboardChartSummaryMap decodedSummary = null;
            if (chartSummaries != null && !chartSummaries.isEmpty()) {
                DashboardChartSummaryMap matchedSummary = chartSummaries.stream()
                    .filter(summary -> marketIndexAnalysisTargetCd.equals(
                        summary.getMarketIndexAnalysisTargetCd()))
                    .filter(summary -> mnemonicCd.equals(summary.getMnemonicCd()))
                    .findFirst()
                    .orElse(null);
                if (matchedSummary == null) {
                    continue;
                }
                decodedSummary = decodeSummaryPath(matchedSummary);
            }
            String predictChartDetailExpn = null;
            MnemonicCode mnemonicCode = MnemonicCode.findByCode(mnemonicCd);
            if (mnemonicCode != null) {
                predictChartDetailExpn = mnemonicCode.getPredictChartDetailExpression();
            }
            AvailableForecastKey forecastKey = new AvailableForecastKey(
                predictionFulfillDt,
                predictionCriteriaDt,
                mnemonicCd,
                marketIndexAnalysisTargetCd,
                predictChartDetailExpn,
                detail.getAssociatedGoodsDataSourceDetail() != null
                    ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                    : null);
            ForecastResultValue resultValue = getForecastResultValue(goodsDataSourceDetail,
                forecastKey);
            if (resultValue == null) {
                continue;
            }

            DiffResult diffResult = calculateDiffResult(resultValue.predictionCriteriaVal(),
                resultValue.predictionResultVal());
            Double avgPredictionResultVal = toScaledDouble(resultValue.predictionResultVal());
            Double predictionCriteriaVal = toScaledDouble(resultValue.predictionCriteriaVal());
            Double minPredictionResultVal = null;
            Double maxPredictionResultVal = null;
            if (resultValue.predictionResults() != null) {
                for (ForecastResultPoint point : resultValue.predictionResults()) {
                    if (point == null || point.predictionResultVal() == null) {
                        continue;
                    }
                    try {
                        BigDecimal value = new BigDecimal(point.predictionResultVal());
                        if (minPredictionResultVal == null
                            || value.compareTo(BigDecimal.valueOf(minPredictionResultVal)) < 0) {
                            minPredictionResultVal =
                                value.setScale(2, RoundingMode.HALF_UP).doubleValue();
                        }
                        if (maxPredictionResultVal == null
                            || value.compareTo(BigDecimal.valueOf(maxPredictionResultVal)) > 0) {
                            maxPredictionResultVal =
                                value.setScale(2, RoundingMode.HALF_UP).doubleValue();
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore invalid numeric data
                    }
                }
            }
            String endPredictionResultDt = resultValue.endPredictionResultDt();
            if (endPredictionResultDt == null && decodedSummary != null) {
                endPredictionResultDt = decodedSummary.getPredictionResultDt();
            }
            String explanatoryTitleContent =
                decodedSummary != null ? decodedSummary.getExplanatoryTitleContent() : null;
            String explanatorySummaryContent =
                decodedSummary != null ? decodedSummary.getExplanatorySummaryContent() : null;
            String explanatoryFilePath =
                decodedSummary != null ? decodedSummary.getExplanatoryFilePath() : null;

            return ForecastReportDetailRes.builder()
                .marketIndexAnalysisTargetCd(marketIndexAnalysisTargetCd)
                .predictionFulfillDt(predictionFulfillDt)
                .mnemonicCd(mnemonicCd)
                .predictChartDetailExpn(forecastKey.predictChartDetailExpn())
                .associatedGoodsDataSourceNo(forecastKey.associatedGoodsDataSourceNo())
                .endPredictionResultDt(endPredictionResultDt)
                .startPredictionResultDt(resultValue.startPredictionResultDt())
                .predictionCriteriaDt(resultValue.predictionCriteriaDt())
                .avgPredictionResultVal(avgPredictionResultVal)
                .minPredictionResultVal(minPredictionResultVal)
                .maxPredictionResultVal(maxPredictionResultVal)
                .predictionCriteriaVal(predictionCriteriaVal)
                .predictionResultDiffVal(diffResult.diffValue())
                .predictionResultDiffRate(diffResult.diffRate())
                .isScheduled(null)
                .explanatoryTitleContent(explanatoryTitleContent)
                .explanatorySummaryContent(explanatorySummaryContent)
                .explanatoryFilePath(explanatoryFilePath)
                .build();
        }

        throw AppErrorException.of(Error.RSC_NOT_FOUND, "Forecast report 상세 조회 실패");
    }

    /**
     * Forecast 차트 주요 요인 분석 카테고리 조회
     *
     * @param companyNo         회사 번호
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate       예측 날짜
     * @return ForecastChartFactorCategoryRes
     */
    public ForecastChartFactorCategoryRes getChartFactorCategory(int companyNo, int goodsDataSourceNo, String predictDate) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
            .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
        List<String> explanatoryLevelNms = forecastMapper.chartFactorCategory(queryWhere, predictDate, null);

        Integer associatedGoodsDataSourceNo = findAssociatedGoodsDataSourceNo(customerCompany, goodsDataSourceNo);
        ForecastChartFactorCategoryRes detailRes =
            buildChartFactorCategoryDetailRes(goodsDataSourceDetail, queryWhere, predictDate,
                associatedGoodsDataSourceNo);

        if (detailRes == null) {
            return ForecastChartFactorCategoryRes.of(explanatoryLevelNms);
        }

        return ForecastChartFactorCategoryRes.builder()
            .explanatoryLevelNms(explanatoryLevelNms)
            .marketIndexAnalysisTargetCd(detailRes.marketIndexAnalysisTargetCd())
            .predictionFulfillDt(detailRes.predictionFulfillDt())
            .mnemonicCd(detailRes.mnemonicCd())
            .predictChartDetailExpn(detailRes.predictChartDetailExpn())
            .associatedGoodsDataSourceNo(detailRes.associatedGoodsDataSourceNo())
            .endPredictionResultDt(detailRes.endPredictionResultDt())
            .startPredictionResultDt(detailRes.startPredictionResultDt())
            .predictionCriteriaDt(detailRes.predictionCriteriaDt())
            .avgPredictionResultVal(detailRes.avgPredictionResultVal())
            .predictionCriteriaVal(detailRes.predictionCriteriaVal())
            .predictionResultDiffVal(detailRes.predictionResultDiffVal())
            .predictionResultDiffRate(detailRes.predictionResultDiffRate())
            .isScheduled(detailRes.isScheduled())
            .build();
    }

    private Integer findAssociatedGoodsDataSourceNo(CustomerCompany customerCompany,
        int goodsDataSourceNo) {
        if (customerCompany == null) {
            return null;
        }

        ChartSettingAndDetails chartPredictSettingDetailRes =
            chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            return null;
        }

        return chartPredictSettingDetailRes.chartPredictSettingDetails().stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .filter(detail -> detail.getGoodsDataSourceDetail() != null)
            .filter(detail -> goodsDataSourceNo
                == detail.getGoodsDataSourceDetail().getGoodsDataSourceNo())
            .map(detail -> detail.getAssociatedGoodsDataSourceDetail() != null
                ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                : null)
            .findFirst()
            .orElse(null);
    }

    private ForecastChartFactorCategoryRes buildChartFactorCategoryDetailRes(
        GoodsDataSourceDetail goodsDataSourceDetail, String queryWhere, String predictDate,
        Integer associatedGoodsDataSourceNo) {
        AvailableForecastKey forecastKey = getForecastKeyForChartFactorCategory(
            goodsDataSourceDetail, predictDate, associatedGoodsDataSourceNo);
        if (forecastKey == null) {
            return null;
        }

        ForecastResultValue resultValue = getForecastResultValue(goodsDataSourceDetail, forecastKey);
        DiffResult diffResult = resultValue != null
            ? calculateDiffResult(resultValue.predictionCriteriaVal(),
                resultValue.predictionResultVal())
            : DiffResult.empty();
        Double avgPredictionResultVal = resultValue != null
            ? toScaledDouble(resultValue.predictionResultVal())
            : null;
        Double predictionCriteriaVal = resultValue != null
            ? toScaledDouble(resultValue.predictionCriteriaVal())
            : null;
        String endPredictionResultDt = resultValue != null ? resultValue.endPredictionResultDt() : null;
        String startPredictionResultDt = resultValue != null ? resultValue.startPredictionResultDt() : null;

        if (endPredictionResultDt == null) {
            DashboardChartSummaryMap summary = findChartSummary(queryWhere, predictDate,
                forecastKey.marketIndexAnalysisTargetCd(), forecastKey.mnemonicCd());
            if (summary != null) {
                endPredictionResultDt = summary.getPredictionResultDt();
            }
        }

        return ForecastChartFactorCategoryRes.builder()
            .marketIndexAnalysisTargetCd(forecastKey.marketIndexAnalysisTargetCd())
            .predictionFulfillDt(forecastKey.predictionFulfillDt())
            .mnemonicCd(forecastKey.mnemonicCd())
            .predictChartDetailExpn(forecastKey.predictChartDetailExpn())
            .associatedGoodsDataSourceNo(forecastKey.associatedGoodsDataSourceNo())
            .endPredictionResultDt(endPredictionResultDt)
            .startPredictionResultDt(startPredictionResultDt)
            .predictionCriteriaDt(
                resultValue != null ? resultValue.predictionCriteriaDt() : forecastKey.predictionCriteriaDt())
            .avgPredictionResultVal(avgPredictionResultVal)
            .predictionCriteriaVal(predictionCriteriaVal)
            .predictionResultDiffVal(diffResult.diffValue())
            .predictionResultDiffRate(diffResult.diffRate())
            .isScheduled(null)
            .build();
    }

    private AvailableForecastKey getForecastKeyForChartFactorCategory(
        GoodsDataSourceDetail goodsDataSourceDetail, String predictDate,
        Integer associatedGoodsDataSourceNo) {
        if (goodsDataSourceDetail == null || predictDate == null) {
            return null;
        }

        String whereClause = buildForecastBaseWhereClause(
            goodsDataSourceDetail.getQueryConditionCntt(), predictDate);
        String columns = String.join(", ",
            MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
            MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME);
        String orderBy = String.format("%s DESC", MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.ofLimitAndOrderBy(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), columns, whereClause, "1", orderBy));

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row = rows.get(0);
        String predictionCriteriaDt = getStringValue(
            row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME));
        String mnemonicCd = getStringValue(row.get(MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME));
        String marketIndexAnalysisTargetCd = getStringValue(
            row.get(MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME));

        String predictChartDetailExpn = null;
        if (mnemonicCd != null) {
            MnemonicCode mnemonicCode = MnemonicCode.findByCode(mnemonicCd);
            if (mnemonicCode != null) {
                predictChartDetailExpn = mnemonicCode.getPredictChartDetailExpression();
            }
        }

        return new AvailableForecastKey(predictDate, predictionCriteriaDt, mnemonicCd,
            marketIndexAnalysisTargetCd, predictChartDetailExpn, associatedGoodsDataSourceNo);
    }

    private String buildForecastBaseWhereClause(String baseWhereClause, String fulfillDate) {
        List<String> clauses = new ArrayList<>();
        if (baseWhereClause != null && !baseWhereClause.isBlank()) {
            clauses.add(baseWhereClause);
        }
        clauses.add(String.format("%s = '%s'", MARKET_DATA_PREDICT_DATE_COLUMN_NAME, fulfillDate));
        return String.join(" AND ", clauses);
    }

    private DashboardChartSummaryMap findChartSummary(String queryWhere, String predictDate,
        String marketIndexAnalysisTargetCd, String mnemonicCd) {
        if (queryWhere == null || predictDate == null) {
            return null;
        }
        List<DashboardChartSummaryMap> chartSummaries =
            forecastMapper.chartSummary(queryWhere, predictDate, null, null);
        if (chartSummaries == null || chartSummaries.isEmpty()) {
            return null;
        }
        DashboardChartSummaryMap matchedSummary = chartSummaries.stream()
            .filter(summary -> Objects.equals(marketIndexAnalysisTargetCd,
                summary.getMarketIndexAnalysisTargetCd()))
            .filter(summary -> Objects.equals(mnemonicCd, summary.getMnemonicCd()))
            .findFirst()
            .orElse(null);
        return decodeSummaryPath(matchedSummary);
    }

    /**
     * Forecast 차트 주요 요인 분석 조회
     *
     * @param companyNo 회사 번호
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @return ForecastChartFactorRes
     */
    public ForecastChartFactorRes getChartFactors(int companyNo, int goodsDataSourceNo, String predictDate,
            String explanatoryLevelNm) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
            .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        List<ForecastChartFactorMap> chartPlusFactorsMap = forecastMapper.chartFactors(
            queryWhere, explanatoryLevelNm, predictDate, null, CHART_EXPLANATORY_PLUS_DIRECTION,
            CHART_EXPLANATORY_TOP_RANK_LIMIT);

        List<ForecastChartFactorMap> chartMinusFactorsMap = forecastMapper.chartFactors(
            queryWhere, explanatoryLevelNm, predictDate, null, CHART_EXPLANATORY_MINUS_DIRECTION,
            CHART_EXPLANATORY_TOP_RANK_LIMIT);

        List<ForecastChartFactor> chartPlusFactors = chartPlusFactorsMap.stream()
            .map(ForecastChartFactor::of)
            .toList();

        List<ForecastChartFactor> chartMinusFactors = chartMinusFactorsMap.stream()
            .map(ForecastChartFactor::of)
            .toList();

        ForecastChartFactorExplanatoryMap chartFactorExplanatory = forecastMapper
            .chartFactorExplanatory(queryWhere, explanatoryLevelNm, predictDate)
            .orElse(null);

        String summary = chartFactorExplanatory != null ? chartFactorExplanatory.getExplanatoryDetailCntt() : null;

        return ForecastChartFactorRes.of(explanatoryLevelNm, summary, chartPlusFactors, chartMinusFactors);
    }

    /**
     * Forecast 차트 주요 요인 분석 라인 그래프 조회
     *
     * @param companyNo 회사 번호
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @param date 조회할 년월(선택)
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @param predictionInputMnemonicCd 예측 입력 MNEMONIC코드
     * @param marketIndexAnalysisTargetCd 시장지수분석대상코드
     * @param predictionInputMnemonicDetailCd 예측 입력 명칭 상세코드
     * @return ForecastChartFactorGraphRes
     */
    public ForecastChartFactorGraphRes getChartFactorGraph(int companyNo, int goodsDataSourceNo,
            String predictDate, String date, String explanatoryLevelNm,
            String predictionInputMnemonicCd, String marketIndexAnalysisTargetCd,
            String predictionInputMnemonicDetailCd) {
        LocalDate baseDate = date == null || date.isEmpty() ? LocalDate.now() : DateUtil.fromYearMonth(date);

        ChartPredictSetting chartPredictSetting = chartPredictSettingRepository
            .findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND, "차트 예측 설정 조회 실패"));

        String currentDate = DateUtil.toYmd(LocalDate.now());
        customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
            .findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND, "GoodsDataSourceDetail not found"));

        DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());
        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        ForecastChartFactorExplanatoryMap chartFactorGraphExplanatory = forecastMapper
            .chartFactorGraphExplanatory(queryWhere, explanatoryLevelNm, predictDate, predictionInputMnemonicCd)
            .orElse(null);

        String summary = chartFactorGraphExplanatory != null ? chartFactorGraphExplanatory.getExplanatoryDetailCntt() : null;

        ForecastChartFactorGraphMap chartFactorGraph = forecastMapper
            .chartFactorGraph(marketIndexAnalysisTargetCd, predictionInputMnemonicDetailCd)
            .orElse(null);

        Integer actualGoodsDataSourceNo = chartFactorGraph != null ? chartFactorGraph.getGoodsDataSourceNo() : null;

        GoodsDataSourceDetail actualGoodsDataSourceDetail = goodsDataSourceDetailRepository
            .findByGoodsDataSourceNoAndIsDeletedFalse(actualGoodsDataSourceNo)
            .orElse(null);

        if (actualGoodsDataSourceDetail != null) {
            GoodsData actualGoodsData = actualGoodsDataSourceDetail.getGoodsData();
            Goods actualGoods = actualGoodsData.getGoods();

            boolean isPermitted = permissionRes.getGoodsViewableMap(companyNo)
                .getOrDefault(actualGoods.getGoodsNo(), false);

            String[] actualTableColumnList = actualGoodsDataSourceDetail.getTableColumnListNm().split(",");
            String actualDateColumnName = actualTableColumnList[MARKET_DATA_DATE_COLUMN_INDEX];
            String actualDefaultDataColumnName = actualTableColumnList[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            LocalDate endDate = baseDate
                .plus(
                    chartPredictSetting.getPredictPeriodMonthsCnt(),
                    chartPredictSetting.getPredictPeriodUnitSeparateCd().getTemporalUnit())
                .with(TemporalAdjusters.lastDayOfMonth());

            LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH).withDayOfMonth(1);

            List<Map<String, Object>> graphRawData = getActualDataRows(actualGoodsDataSourceDetail, actualDateColumnName,
                startDate, endDate);

            List<LocalDate> periodDates = generateGraphDates(startDate, endDate, chartPredictSetting);
            List<ForecastChartRawData> periodAverages = isPermitted
                ? calculateGraphAverages(periodDates, graphRawData, actualDateColumnName,
                    actualDefaultDataColumnName, chartPredictSetting)
                : null;

            ForecastChartFactorGraphMetadata graphMetadata =
                ForecastChartFactorGraphMetadata.of(actualGoodsDataSourceDetail, chartPredictSetting);
            ForecastChartFactorGraphDatasource graphDatasource =
                ForecastChartFactorGraphDatasource.of(actualGoodsDataSourceDetail, isPermitted);

            return ForecastChartFactorGraphRes.of(summary, graphMetadata, periodAverages, graphDatasource);
        }

        ForecastChartFactorGraphMetadata graphMetadata = ForecastChartFactorGraphMetadata.of(chartPredictSetting);
        return ForecastChartFactorGraphRes.of(summary, graphMetadata);
    }

    private String getStringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * Forecast 리포트 목록 조회
     *
     * @param companyNo         회사 번호
     * @param goodsDataSourceNo 데이터 소스 번호(선택)
     * @return List<ForecastReportRes>
     */
    public List<ForecastReportRes> getForecastReports(int companyNo, Integer goodsDataSourceNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(
            customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                .filter(detail -> goodsDataSourceNo.equals(
                    detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
                .toList();
        }
        List<AvailableForecastKey> availableDates = getAvailableDates(chartPredictSettingDetails);

        List<ForecastReportRes> results = new ArrayList<>();
        for (AvailableForecastKey forecastKey : availableDates) {
            results.addAll(getForecastReportByDate(chartPredictSettingDetails, forecastKey));
        }

        return results;
    }

    /**
     * Forecast report 파일 다운로드
     *
     * @param companyNo  회사 번호
     * @param predictDate 예측 날짜(YYYYMMDD)
     * @param zipFileName 파일명
     * @return FileDownloadRes
     */
    public FileDownloadRes getForecastReportFileDownload(int companyNo, String predictDate,
        String zipFileName) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(
                companyNo, currentDate)
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        if (predictDate == null || predictDate.isBlank()) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "예측 날짜가 없습니다.");
        }

        ChartSettingAndDetails chartPredictSettingDetailRes =
            chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null
            || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, ERR_CHART_NOT_CONFIGURED);
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails =
            chartPredictSettingDetailRes.chartPredictSettingDetails();

        boolean isDownloadable =
            isDownloadableForecastReportFile(chartPredictSettingDetails, companyNo);
        if (!isDownloadable) {
            throw AppErrorException.of(Error.AUTH_ACCESS_DENIED,
                String.format("다운로드 권한이 없습니다. %s", companyNo));
        }

        List<ForecastReport> reports =
            getForecastReportsForDownload(chartPredictSettingDetails, predictDate);

        if (reports == null || reports.isEmpty()) {
            log.warn("No forecast reports found for companyNo: {}, predictDate: {}",
                companyNo, predictDate);
            throw AppErrorException.of(Error.RSC_NOT_FOUND, "Forecast 리포트 데이터가 없습니다.");
        }

        List<String> filePaths = reports.stream()
            .filter(Objects::nonNull)
            .map(ForecastReport::explanatoryFilePath)
            .filter(Objects::nonNull)
            .toList();

        if (filePaths.isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, "다운로드 가능한 파일이 없습니다.");
        }

        return fileService.directDownloadFiles(filePaths, zipFileName);
    }

    private List<ForecastRecordHeaderRes> buildForecastRecordHeaders(int companyNo) {
        List<UiComponentSetting> overrideSettings = mergeForecastRecordSettings(companyNo);

        return overrideSettings.stream()
            .sorted(Comparator
                .comparing((UiComponentSetting setting) -> setting.getOrderNo() == null)
                .thenComparing(UiComponentSetting::getOrderNo, Comparator.nullsLast(Integer::compareTo)))
            .map(setting -> new ForecastRecordHeaderRes(
                setting.getItemKey(),
                setting.getLabelText(),
                setting.getIsVisible(),
                setting.getOrderNo()
            ))
            .toList();
    }

    private List<ForecastRecordHeaderRes> buildForecastRecordHeadersForCompany(int companyNo) {
        List<UiComponentSetting> settings =
            uiComponentSettingRepository.findOrdered(
                companyNo, FORECAST_RECORD_SCREEN_CODE, FORECAST_RECORD_COMPONENT_TYPE,
                FORECAST_RECORD_COMPONENT_CODE);

        return settings.stream()
            .sorted(Comparator
                .comparing((UiComponentSetting setting) -> setting.getOrderNo() == null)
                .thenComparing(UiComponentSetting::getOrderNo,
                    Comparator.nullsLast(Integer::compareTo)))
            .map(setting -> new ForecastRecordHeaderRes(
                setting.getItemKey(),
                setting.getLabelText(),
                setting.getIsVisible(),
                setting.getOrderNo()
            ))
            .toList();
    }

    private Map<String, Boolean> buildForecastRecordVisibilityMap(int companyNo) {
        List<UiComponentSetting> mergedSettings = mergeForecastRecordSettings(companyNo);
        return mergedSettings.stream()
            .collect(Collectors.toMap(
                UiComponentSetting::getItemKey,
                UiComponentSetting::getIsVisible,
                (existing, replacement) -> existing
            ));
    }

    private List<UiComponentSetting> mergeForecastRecordSettings(int companyNo) {
        List<UiComponentSetting> defaultSettings =
            uiComponentSettingRepository
                .findOrdered(
                    0, FORECAST_RECORD_SCREEN_CODE, FORECAST_RECORD_COMPONENT_TYPE,
                    FORECAST_RECORD_COMPONENT_CODE);
        List<UiComponentSetting> companySettings =
            companyNo == 0
                ? List.of()
                : uiComponentSettingRepository
                    .findOrdered(
                        companyNo, FORECAST_RECORD_SCREEN_CODE, FORECAST_RECORD_COMPONENT_TYPE,
                        FORECAST_RECORD_COMPONENT_CODE);

        Map<String, UiComponentSetting> mergedByKey = new HashMap<>();
        for (UiComponentSetting setting : defaultSettings) {
            mergedByKey.put(setting.getItemKey(), setting);
        }
        for (UiComponentSetting setting : companySettings) {
            mergedByKey.put(setting.getItemKey(), setting);
        }
        return new ArrayList<>(mergedByKey.values());
    }

    private boolean isVisibleOrNull(Boolean isVisible) {
        return isVisible == null || isVisible;
    }

    private Boolean getModelAccuracyVisible(int companyNo) {
        Optional<UiComponentSetting> setting =
            uiComponentSettingRepository
                .findFirstByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndItemKeyAndIsDeletedFalse(
                    companyNo,
                    FORECAST_RECORD_SCREEN_CODE,
                    FORECAST_MODEL_ACCURACY_COMPONENT_TYPE,
                    FORECAST_MODEL_ACCURACY_COMPONENT_CODE,
                    FORECAST_MODEL_ACCURACY_ITEM_KEY);
        return setting.filter(
            uiComponentSetting -> isVisibleOrNull(uiComponentSetting.getIsVisible())).isPresent();
    }

    private LocalDate calculateBaseDate(LocalDate today, Integer periodCount,
        PeriodUnitSeparateCd periodUnit) {
        if (today == null) {
            return null;
        }
        if (periodCount == null || periodUnit == null || periodCount <= 0) {
            return today;
        }
        return today.plus(periodCount, periodUnit.getTemporalUnit());
    }

    private Double getActualAverageValue(GoodsDataSourceDetail actualDetail,
        String startPredictionResultDt, String endPredictionResultDt) {
        if (actualDetail == null || startPredictionResultDt == null || endPredictionResultDt == null) {
            return null;
        }

        String[] tableColumnList = Arrays.stream(actualDetail.getTableColumnListNm().split(","))
            .map(String::trim)
            .toArray(String[]::new);
        if (tableColumnList.length <= MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX) {
            return null;
        }

        String dateColumnName = tableColumnList[MARKET_DATA_DATE_COLUMN_INDEX];
        String defaultDataColumnName = tableColumnList[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

        DataSourceContextHolder.setDataSourceKey(actualDetail.getTargetDbNm());

        String baseWhere = actualDetail.getQueryConditionCntt();
        String dateRangeWhere = String.format("%s BETWEEN '%s' AND '%s'", dateColumnName,
            startPredictionResultDt, endPredictionResultDt);
        String whereClause = (baseWhere == null || baseWhere.isBlank())
            ? dateRangeWhere
            : baseWhere + " AND " + dateRangeWhere;

        String columns = "AVG(" + defaultDataColumnName + ") as actualVal";

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource(actualDetail.getTargetDbNm())
                .tableName(actualDetail.getTargetTableNm())
                .columns(columns)
                .whereClause(whereClause)
                .build());

        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> firstRow = rows.get(0);
        if (firstRow == null) {
            return null;
        }
        String avgValue = getStringValue(firstRow.get("actualVal"));
        return toScaledDouble(avgValue);
    }

    private String buildPredictKey(String queryConditionCntt) {
        if (queryConditionCntt == null || queryConditionCntt.isBlank()) {
            return null;
        }
        String marketIndexAnalysisTargetCd = null;
        String mnemonicCd = null;

        Pattern marketPattern = Pattern.compile(
            MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME + "\\s*=\\s*'([^']+)'",
            Pattern.CASE_INSENSITIVE);
        Matcher marketMatcher = marketPattern.matcher(queryConditionCntt);
        if (marketMatcher.find()) {
            marketIndexAnalysisTargetCd = marketMatcher.group(1);
        }

        Pattern mnemonicPattern = Pattern.compile(
            MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME + "\\s*=\\s*'([^']+)'",
            Pattern.CASE_INSENSITIVE);
        Matcher mnemonicMatcher = mnemonicPattern.matcher(queryConditionCntt);
        if (mnemonicMatcher.find()) {
            mnemonicCd = mnemonicMatcher.group(1);
        }

        if (marketIndexAnalysisTargetCd == null || mnemonicCd == null) {
            return null;
        }
        return buildPredictKey(marketIndexAnalysisTargetCd, mnemonicCd);
    }

    private String buildPredictKey(String marketIndexAnalysisTargetCd, String mnemonicCd) {
        if (marketIndexAnalysisTargetCd == null || mnemonicCd == null) {
            return null;
        }
        return marketIndexAnalysisTargetCd + "|" + mnemonicCd;
    }

    private String extractMnemonicCode(String queryConditionCntt, boolean isPredictDetail) {
        if (queryConditionCntt == null || queryConditionCntt.isBlank()) {
            return null;
        }
        String columnName = isPredictDetail
            ? MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME
            : "code";
        Pattern mnemonicPattern = Pattern.compile(
            columnName + "\\s*=\\s*'([^']+)'",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = mnemonicPattern.matcher(queryConditionCntt);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<ForecastReport> getForecastReportsForDownload(
        List<ChartPredictSettingDetail> chartPredictSettingDetails, String fulfillDate) {
        return chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .flatMap(chartPredictSettingDetail -> {
                GoodsDataSourceDetail goodsDataSourceDetail =
                    chartPredictSettingDetail.getGoodsDataSourceDetail();
                if (goodsDataSourceDetail == null
                    || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                    return Stream.empty();
                }

                DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

                String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
                List<DashboardChartSummaryMap> summaries =
                    forecastMapper.chartSummary(queryWhere, fulfillDate, null, null);

                return summaries != null
                    ? summaries.stream().map(this::decodeSummaryPath)
                    : Stream.empty();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    summary -> summary.getMarketIndexAnalysisTargetCd() + "|"
                        + summary.getPredictionFulfillDt() + "|"
                        + summary.getMnemonicCd() + "|"
                        + summary.getPredictionResultDt(),
                    summary -> summary,
                    (existing, replacement) -> existing
                ),
                map -> map.values().stream()
                    .map(ForecastReport::of)
                    .toList()));
    }

    private boolean isDownloadableForecastReportFile(
        List<ChartPredictSettingDetail> chartPredictSettingDetails, int companyNo) {
        PermissionRes permissionRes = authorizationService.getPermissions();

        return chartPredictSettingDetails.stream()
            .filter(detail -> detail.getGoodsDataSourceDetail().getIsPredictData())
            .allMatch(detail -> {
                GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                    .findByGoodsDataSourceNoAndIsDeletedFalse(
                        detail.getGoodsDataSourceDetail().getGoodsDataSourceNo())
                    .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND,
                        String.format("데이터소스 조회 실패: %s",
                            detail.getGoodsDataSourceDetail().getGoodsDataSourceNo())));
                GoodsData goodsData = goodsDataSourceDetail.getGoodsData();
                Goods goods = goodsData.getGoods();
                return permissionRes.getGoodsDownloadableMap(companyNo)
                    .getOrDefault(goods.getGoodsNo(), false);
            });
    }

    /**
     * 마지막 Update Date 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getLastUpdateDateData(
        GoodsDataSourceDetail goodsDataSourceDetail) {

        if (goodsDataSourceDetail == null) {
            return Collections.emptyList();
        }

        String orderByColumns = String.format("%s DESC",
            MiForecastService.MARKET_DATA_PREDICT_DATE_COLUMN_NAME);

        return mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.ofLimitAndOrderBy(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(),
                MiForecastService.MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                goodsDataSourceDetail.getQueryConditionCntt(),
                DASHBOARD_CHART_LAST_UPDATE_DATE_LIMIT, orderByColumns));
    }

    /**
     * 조회 가능한 날짜 목록 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @return List<AvailableForecastKey>
     */
    private List<AvailableForecastKey> getAvailableDates(
        List<ChartPredictSettingDetail> chartPredictSettingDetails) {
        return chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .flatMap(detail -> {
                GoodsDataSourceDetail goodsDataSource = detail.getGoodsDataSourceDetail();
                if (goodsDataSource == null) {
                    return Stream.empty();
                }

                Integer associatedGoodsDataSourceNo =
                    detail.getAssociatedGoodsDataSourceDetail() != null
                        ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                        : null;
                List<Map<String, Object>> rawData = getDateDataRows(goodsDataSource);
                return rawData.stream()
                    .map(row -> {
                        Object fulfillDateValue = row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                        Object criteriaDateValue = row.get(
                            MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);
                        if (fulfillDateValue == null || criteriaDateValue == null) {
                            return null;
                        }
                        String fulfillDate = String.valueOf(fulfillDateValue);
                        String criteriaDate = String.valueOf(criteriaDateValue);
                        String mnemonicCd = getStringValue(
                            row.get(MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME));
                        String marketIndexAnalysisTargetCd =
                            getStringValue(
                                row.get(MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME));
                        String predictChartDetailExpn = null;
                        if (mnemonicCd != null) {
                            MnemonicCode mnemonicCode = MnemonicCode.findByCode(mnemonicCd);
                            if (mnemonicCode != null) {
                                predictChartDetailExpn = mnemonicCode.getPredictChartDetailExpression();
                            }
                        }
                        return new AvailableForecastKey(fulfillDate, criteriaDate, mnemonicCd,
                            marketIndexAnalysisTargetCd,
                            predictChartDetailExpn, associatedGoodsDataSourceNo);
                    })
                    .filter(Objects::nonNull);
            })
            .sorted(Comparator
                .comparing(AvailableForecastKey::predictionFulfillDt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AvailableForecastKey::associatedGoodsDataSourceNo,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    /**
     * Forecast 리포트 데이터 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param forecastKey                조회 날짜 정보
     * @return List<ForecastReportRes>
     */
    private List<ForecastReportRes> getForecastReportByDate(
        List<ChartPredictSettingDetail> chartPredictSettingDetails,
        AvailableForecastKey forecastKey) {
        return chartPredictSettingDetails.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .filter(detail -> Objects.equals(
                detail.getAssociatedGoodsDataSourceDetail() != null
                    ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                    : null,
                forecastKey.associatedGoodsDataSourceNo()))
            .flatMap(detail -> {
                GoodsDataSourceDetail goodsDataSourceDetail = detail.getGoodsDataSourceDetail();
                if (goodsDataSourceDetail == null
                    || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                    return Stream.empty();
                }

                DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

                ForecastResultValue resultValue = getForecastResultValue(goodsDataSourceDetail,
                    forecastKey);
                if (resultValue == null) {
                    return Stream.empty();
                }

                DiffResult diffResult = calculateDiffResult(resultValue.predictionCriteriaVal(),
                    resultValue.predictionResultVal());
                Double predictionResultVal = toScaledDouble(resultValue.predictionResultVal());
                Double predictionCriteriaVal = toScaledDouble(resultValue.predictionCriteriaVal());
                return Stream.of(ForecastReportRes.builder()
                    .marketIndexAnalysisTargetCd(forecastKey.marketIndexAnalysisTargetCd())
                    .predictionFulfillDt(forecastKey.predictionFulfillDt())
                    .mnemonicCd(forecastKey.mnemonicCd())
                    .predictChartDetailExpn(forecastKey.predictChartDetailExpn())
                    .associatedGoodsDataSourceNo(forecastKey.associatedGoodsDataSourceNo())
                    .predictionCriteriaDt(resultValue.predictionCriteriaDt())
                    .endPredictionResultDt(resultValue.endPredictionResultDt())
                    .startPredictionResultDt(resultValue.startPredictionResultDt())
                    .predictionResultVal(predictionResultVal)
                    .predictionCriteriaVal(predictionCriteriaVal)
                    .predictionResultDiffVal(diffResult.diffValue())
                    .predictionResultDiffRate(diffResult.diffRate())
                    .predictionResultList(toPredictionResultPoints(resultValue.predictionResults()))
                    .build());
            })
            .toList();
    }

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다. 데이터소스 상세 정보와 날짜 컬럼명, 기준일 컬럼명을 기반으로 데이터를 필터링하여 조회한다. 조건에 맞는
     * 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getDateDataRows(GoodsDataSourceDetail goodsDataSourceDetail) {
        if (goodsDataSourceDetail == null) {
            return Collections.emptyList();
        }

        int startDateLength = 1;
        int endDateLength = 6;
        String dateColumn = String.format(
            "MAX(%s) as %s, MAX(%s) as %s, MAX(%s) as %s, MAX(%s) as %s",
            MARKET_DATA_PREDICT_DATE_COLUMN_NAME, MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME, MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
            MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME,
            MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME);
        String groupBy = "SUBSTRING(%s, %d, %d)".formatted(MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
            startDateLength, endDateLength);
        String orderBy = String.format("%s DESC", groupBy);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.builder()
            .targetDataSource(goodsDataSourceDetail.getTargetDbNm())
            .tableName(goodsDataSourceDetail.getTargetTableNm())
            .columns(dateColumn)
            .whereClause(goodsDataSourceDetail.getQueryConditionCntt())
            .groupByColumns(groupBy)
            .orderByColumns(orderBy)
            .limit(FORECAST_AVAILABLE_DATE_LIMIT)
            .build());
    }

    private ForecastResultValue getForecastResultValue(GoodsDataSourceDetail goodsDataSourceDetail,
        AvailableForecastKey forecastKey) {
        if (goodsDataSourceDetail == null || forecastKey == null) {
            return null;
        }
        if (forecastKey.predictionFulfillDt() == null
            || forecastKey.predictionCriteriaDt() == null) {
            return null;
        }

        String whereClause = buildForecastResultWhereClause(
            goodsDataSourceDetail.getQueryConditionCntt(),
            forecastKey.predictionFulfillDt(), forecastKey.predictionCriteriaDt());
        String columns = String.join(", ",
            MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME,
            MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
            MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME);

        List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicRawQuery(
            DynamicDataDetailReq.builder()
                .targetDataSource(goodsDataSourceDetail.getTargetDbNm())
                .tableName(goodsDataSourceDetail.getTargetTableNm())
                .columns(columns)
                .whereClause(whereClause)
                .orderByColumns(
                    String.format("%s ASC", MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME))
                .build());

        if (rows.isEmpty()) {
            return null;
        }

        List<ForecastResultPoint> predictionResults = new ArrayList<>();
        String startPredictionResultDt = null;
        String endPredictionResultDt = null;
        String predictionCriteriaDt = null;
        BigDecimal predictionCriteriaVal = null;
        BigDecimal predictionResultSum = BigDecimal.ZERO;
        int predictionResultCount = 0;

        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            String resultDt = getStringValue(row.get(MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME));
            String resultVal = getStringValue(
                row.get(MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME));
            String criteriaDt = getStringValue(
                row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME));
            String criteriaVal = getStringValue(
                row.get(MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME));

            if (resultDt != null) {
                if (startPredictionResultDt == null) {
                    startPredictionResultDt = resultDt;
                }
                endPredictionResultDt = resultDt;
            }

            if (criteriaDt != null
                && (predictionCriteriaDt == null
                    || criteriaDt.compareTo(predictionCriteriaDt) > 0)) {
                predictionCriteriaDt = criteriaDt;
            }

            if (criteriaVal != null) {
                try {
                    BigDecimal criteria = new BigDecimal(criteriaVal);
                    if (predictionCriteriaVal == null
                        || criteria.compareTo(predictionCriteriaVal) > 0) {
                        predictionCriteriaVal = criteria;
                    }
                } catch (NumberFormatException ignored) {
                    // ignore invalid numeric data
                }
            }

            if (resultVal != null) {
                try {
                    predictionResultSum = predictionResultSum.add(new BigDecimal(resultVal));
                    predictionResultCount++;
                } catch (NumberFormatException ignored) {
                    // ignore invalid numeric data
                }
            }

            predictionResults.add(new ForecastResultPoint(resultDt, resultVal));
        }

        String avgPredictionResultVal = null;
        if (predictionResultCount > 0) {
            avgPredictionResultVal = predictionResultSum
                .divide(BigDecimal.valueOf(predictionResultCount), 6, RoundingMode.HALF_UP)
                .toPlainString();
        }

        return new ForecastResultValue(
            endPredictionResultDt,
            startPredictionResultDt,
            predictionCriteriaDt,
            avgPredictionResultVal,
            predictionCriteriaVal != null ? predictionCriteriaVal.toPlainString() : null,
            predictionResults);
    }

    private String buildForecastResultWhereClause(String baseWhereClause, String fulfillDate,
        String criteriaDate) {
        List<String> clauses = new ArrayList<>();
        if (baseWhereClause != null && !baseWhereClause.isBlank()) {
            clauses.add(baseWhereClause);
        }
        clauses.add(String.format("%s = '%s'", MARKET_DATA_PREDICT_DATE_COLUMN_NAME, fulfillDate));
        clauses.add(String.format("%s = '%s'", MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
            criteriaDate));
        return String.join(" AND ", clauses);
    }

    private LocalDate calculateStartDate(LocalDate today, TotalPeriod totalPeriod) {
        return switch (totalPeriod) {
            case ONE_WEEK -> today.minusWeeks(1).plusDays(1);
            case ONE_MONTH -> today.minusMonths(1).plusDays(1);
            case THREE_MONTHS -> today.minusMonths(3).plusDays(1);
            case SIX_MONTHS -> today.minusMonths(6).plusDays(1);
            case YEAR_TO_DATE -> LocalDate.of(today.getYear(), 1, 1);
            case ONE_YEAR -> today.minusYears(1).plusDays(1);
            case TWO_YEARS -> today.minusYears(2).plusDays(1);
            case ALL -> today.minusYears(10).plusDays(1);
        };
    }

    private void validatePeriodLength(LocalDate startDate, LocalDate endDate, AxisUnit axisUnit) {
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long unitDays = getUnitDays(endDate, axisUnit);
        if (periodDays <= unitDays) {
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER,
                String.format("전체기간(%d일)은 x축 단위(%d일)보다 길어야 합니다.", periodDays, unitDays));
        }
    }

    private long getUnitDays(LocalDate endDate, AxisUnit axisUnit) {
        return switch (axisUnit) {
            case ONE_DAY -> 1;
            case ONE_WEEK -> 7;
            case ONE_MONTH -> endDate.lengthOfMonth();
            case ONE_QUARTER -> {
                int startMonth = ((endDate.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate quarterStart = LocalDate.of(endDate.getYear(), startMonth, 1);
                LocalDate quarterEnd = LocalDate.of(endDate.getYear(), startMonth + 2, 1)
                    .with(TemporalAdjusters.lastDayOfMonth());
                yield java.time.temporal.ChronoUnit.DAYS.between(quarterStart, quarterEnd) + 1;
            }
            case ONE_YEAR -> endDate.lengthOfYear();
        };
    }

    private List<ForecastChartRes.ForecastChartDateItem> buildDateList(LocalDate startDate,
        LocalDate endDate,
        AxisUnit axisUnit) {
        List<ForecastChartRes.ForecastChartDateItem> dates = new ArrayList<>();
        if (axisUnit == AxisUnit.ONE_DAY) {
            LocalDate cursor = startDate;
            java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("MM/dd");
            while (!cursor.isAfter(endDate)) {
                dates.add(ForecastChartRes.ForecastChartDateItem.builder()
                    .label(formatter.format(cursor))
                    .startDate(DateUtil.toYmd(cursor))
                    .endDate(DateUtil.toYmd(cursor))
                    .unit(axisUnit.label)
                    .build());
                cursor = cursor.plusDays(1);
            }
            return dates;
        }

        LocalDate cursorEnd = endDate;
        while (!cursorEnd.isBefore(startDate)) {
            LocalDate periodStart = getPeriodStart(cursorEnd, axisUnit);
            LocalDate actualStart = periodStart.isBefore(startDate) ? startDate : periodStart;
            LocalDate actualEnd = cursorEnd;
            String label = formatAxisLabel(actualEnd, axisUnit);

            dates.add(ForecastChartRes.ForecastChartDateItem.builder()
                .label(label)
                .startDate(DateUtil.toYmd(actualStart))
                .endDate(DateUtil.toYmd(actualEnd))
                .unit(axisUnit.label)
                .build());

            cursorEnd = periodStart.minusDays(1);
        }

        Collections.reverse(dates);
        return dates;
    }

    private LocalDate getPeriodStart(LocalDate date, AxisUnit axisUnit) {
        return switch (axisUnit) {
            case ONE_WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case ONE_MONTH -> date.withDayOfMonth(1);
            case ONE_QUARTER -> {
                int startMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
                yield LocalDate.of(date.getYear(), startMonth, 1);
            }
            case ONE_YEAR -> LocalDate.of(date.getYear(), 1, 1);
            case ONE_DAY -> date;
        };
    }

    private String formatAxisLabel(LocalDate date, AxisUnit axisUnit) {
        return switch (axisUnit) {
            case ONE_DAY -> DateUtil.toYmd(date);
            case ONE_WEEK -> {
                java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
                int week = date.get(weekFields.weekOfWeekBasedYear());
                int year = date.get(weekFields.weekBasedYear());
                yield String.format("W%d %d", week, year);
            }
            case ONE_MONTH -> {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(
                    "yy/MM");
                yield formatter.format(date);
            }
            case ONE_QUARTER -> {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                int year = date.getYear() % 100;
                yield String.format("%02d-%dQ", year, quarter);
            }
            case ONE_YEAR -> String.valueOf(date.getYear());
        };
    }

    private List<ForecastReportRes.PredictionResultPoint> toPredictionResultPoints(
        List<ForecastResultPoint> predictionResults) {
        if (predictionResults == null || predictionResults.isEmpty()) {
            return Collections.emptyList();
        }
        return predictionResults.stream()
            .map(point -> ForecastReportRes.PredictionResultPoint.builder()
                .predictionResultDt(point.predictionResultDt())
                .predictionResultVal(toScaledDouble(point.predictionResultVal()))
                .build())
            .toList();
    }

    private DiffResult calculateDiffResult(String criteriaValue, String resultValue) {
        if (criteriaValue == null || resultValue == null) {
            return DiffResult.empty();
        }
        try {
            BigDecimal criteria = new BigDecimal(criteriaValue);
            BigDecimal result = new BigDecimal(resultValue);
            if (criteria.compareTo(BigDecimal.ZERO) == 0) {
                return DiffResult.empty();
            }
            BigDecimal diff = criteria.subtract(result);
            BigDecimal rate = diff.divide(criteria, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal scaledDiff = diff.setScale(2, RoundingMode.HALF_UP);
            return new DiffResult(scaledDiff.doubleValue(), rate.doubleValue());
        } catch (NumberFormatException e) {
            return DiffResult.empty();
        }
    }

    private Double toScaledDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private DashboardChartSummaryMap decodeSummaryPath(DashboardChartSummaryMap summary) {
        if (summary == null || summary.getExplanatoryFilePath() == null) {
            return summary;
        }
        try {
            String decodedPath = URLDecoder.decode(summary.getExplanatoryFilePath(),
                StandardCharsets.UTF_8);
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
            return summary;
        }
    }

    private enum TableSortOrder {
        NEWEST,
        OLDEST;

        private static TableSortOrder from(String value) {
            if (value == null || value.isBlank()) {
                return NEWEST;
            }
            for (TableSortOrder order : values()) {
                if (order.name().equalsIgnoreCase(value)) {
                    return order;
                }
            }
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "정렬 순서 값이 올바르지 않습니다.");
        }
    }

    private enum AxisUnit {
        ONE_DAY("1D"),
        ONE_WEEK("1W"),
        ONE_MONTH("1M"),
        ONE_QUARTER("1Q"),
        ONE_YEAR("1Y");

        private final String label;

        AxisUnit(String label) {
            this.label = label;
        }

        private static AxisUnit from(String value) {
            for (AxisUnit unit : values()) {
                if (unit.label.equalsIgnoreCase(value)) {
                    return unit;
                }
            }
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "x축 단위가 올바르지 않습니다.");
        }
    }

    private enum TotalPeriod {
        ONE_WEEK("1W"),
        ONE_MONTH("1M"),
        THREE_MONTHS("3M"),
        SIX_MONTHS("6M"),
        YEAR_TO_DATE("YTD"),
        ONE_YEAR("1Y"),
        TWO_YEARS("2Y"),
        ALL("ALL");

        private final String label;

        TotalPeriod(String label) {
            this.label = label;
        }

        private static TotalPeriod from(String value) {
            for (TotalPeriod period : values()) {
                if (period.label.equalsIgnoreCase(value)) {
                    return period;
                }
            }
            throw AppErrorException.of(Error.REQ_INVALID_PARAMETER, "전체기간 값이 올바르지 않습니다.");
        }
    }

    private record LineValueStrategy(String mode, String cutoff) {

        private static LineValueStrategy defaultStrategy() {
            return new LineValueStrategy("DEFAULT", "TODAY");
        }

        private boolean isPastActualFuturePredict() {
            return LINE_STRATEGY_PAST_ACTUAL_FUTURE_PREDICT.equalsIgnoreCase(mode);
        }
    }

    private record ForecastRecordHeaderDefault(String columnKey, String headerText, int orderNo) {
    }

    private record ForecastTableColumn(ForecastChartLine actualLine,
                                       ForecastChartLine forecastLine) {

    }

    private record ForecastTableData(List<String> headerList, List<List<Object>> rows,
                                     TableSortOrder sortOrder) {

    }

    private record AvailableForecastKey(String predictionFulfillDt, String predictionCriteriaDt,
                                        String mnemonicCd,
                                        String marketIndexAnalysisTargetCd,
                                        String predictChartDetailExpn,
                                        Integer associatedGoodsDataSourceNo) {

    }

    private record DiffResult(Double diffValue, Double diffRate) {

        private static DiffResult empty() {
            return new DiffResult(null, null);
        }
    }

    private record ForecastResultValue(String endPredictionResultDt, String startPredictionResultDt,
                                       String predictionCriteriaDt, String predictionResultVal,
                                       String predictionCriteriaVal,
                                       List<ForecastResultPoint> predictionResults) {

    }

    private record ForecastResultPoint(String predictionResultDt, String predictionResultVal) {

    }
}
