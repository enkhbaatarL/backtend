package com.sk.domain.mi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.dashboard.DashboardInfoRes;
import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactor;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorMap;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardDataCache;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardDataSource;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChart;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartLine;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;
import com.sk.domain.mi.dto.response.dashboard.charts.metaData.DashboardMetadata;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.chart.ChartPredictDataType;
import com.sk.domain.shared.mapper.dynamic.DashboardMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;

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
    private final MiGoodsPermissionService miGoodsPermissionService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final DashboardMapper dashboardMapper;

    private final int CHART_EXPLANATORY_TOP_RANK_LIMIT = 5;
    private final String CHART_EXPLANATORY_PLUS_DIRECTION = "+";
    private final String CHART_EXPLANATORY_MINUS_DIRECTION = "-";

    private final String DASHBOARD_CHART_CACHE_KEY = "dashboard_chart:%d_based_on_date:%s";
    private static final int DASHBOARD_CHART_CACHE_TTL = 1; // 대시보드 차트 캐시 유효 기간 (1시간)

    private final int DASHBOARD_CHART_START_DATE_FROM_MONTH = 11; // 차트 데이터 조회 기준 날짜 (N달 전)
    private static final int MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX = 0; // 예측 결과 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스
    private static final String MARKET_DATA_PREDICT_DATE_COLUMN_NAME = "prdc_fulf_dt"; // 예측 수행 날짜 컬럼명

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 데이터 존재 여부 조회
     * 
     * @param companyNo
     * @return boolean
     */
    public boolean getDataExists(int companyNo) {
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> new EntityNotFoundException("CustomerCompany not found"));

        return chartPredictSettingRepository.existsByCustomerCompany(customerCompany);
    }

    /**
     * 대시보드 기본 정보 조회
     * 
     * @param companyNo
     * @return DashboardChartRes
     */
    public DashboardInfoRes getInfo(int companyNo) {

        return null;
    }

    /**
     * Latest Data 목록 조회
     * 
     * @param companyNo
     * @return List<LatestDataRes>
     */
    public List<LatestDataRes> getLatestData(int companyNo) {

        return goodsDataRepository.findLatestData(companyNo, PageRequest.of(0, 5));
    }

    /**
     * Latest Insights 목록 조회
     * 
     * @param companyNo
     * @return List<LatestInsights>
     */
    public List<LatestInsightsRes> getLatestInsights(int companyNo) {

        return goodsDataRepository.findLatestInsights(companyNo, PageRequest.of(0, 5));
    }

    /**
     * 차트 날짜 조회
     * 
     * @param page
     * @param size
     * @param companyNo
     * @return List<String>
     */
    public List<String> getChartDates(int page, int size, int companyNo) {

        return null;
    }

    /**
     * 대시보드 차트 데이터 조회
     * 
     * @param customerCompanyNo
     * @param date
     * @param authentication
     * @return
     */
    public DashboardChartRes getChart(Integer customerCompanyNo, String date, Authentication authentication) {
        final String finalDate = (date == null || date.isEmpty()) ? DateUtil.toYearMonth(LocalDate.now()) : date;

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(customerCompanyNo)
                .orElseThrow(() -> new EntityNotFoundException("CustomerCompany not found"));

        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "사용자 권한 조회 실패");
        }
        Role role = Role.valueOf(jwtToken.getToken().getClaimAsString(Claim.ROLE.getCode()));

        // 권한 조회
        PermissionRes permissionRes = switch (role) {
            case USER -> {
                Integer userNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.USER_NO.getCode()));
                yield miGoodsPermissionService.getUserGoodsPermissions(userNo);
            }
            case ADMIN, API_SUB_ADMIN -> miGoodsPermissionService.getAccessAdminGoodsPermissions();
            case API_PLUS_OPERATOR -> {
                Integer companyNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.COMPANY_NO.getCode()));
                yield miGoodsPermissionService.getPlusOperatorGoodsPermissions(companyNo);
            }
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        };

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 스레드 안전한 컬렉션 초기화
        List<LocalDate> lastUpdatedList = Collections.synchronizedList(new ArrayList<>());
        List<DashboardDataSource> dashboardDataSources = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<GoodsDataSourceDetail> actualGoodsDataSourceDetail = new AtomicReference<>();
        ConcurrentHashMap<Integer, DashboardChart> chartMap = new ConcurrentHashMap<>();

        // 차트 데이터 조회
        chartPredictSettingDetails.parallelStream().forEach(chartPredictSettingDetail -> {
            GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();
            GoodsData goodsData = goodsDataSource.getGoodsData();
            Goods goods = goodsData.getGoods();

            // 권한 조회
            Boolean isPermitted = permissionRes.getAdminGoodsViewableMap(customerCompanyNo).getOrDefault(goods.getGoodsNo(), false);

            // 차트 데이터 타입 설정
            ChartPredictDataType chartPredictDataType;
            if (chartPredictSettingDetail.getIsFuture()) {
                chartPredictDataType = ChartPredictDataType.FUTURE;
            } else if (chartPredictSettingDetail.getIsChartLinePredictVal()) {
                chartPredictDataType = ChartPredictDataType.FORECAST;

                // 정책상 예측 데이터는 무조건 권한이 있다
                isPermitted = true;
            } else {
                chartPredictDataType = ChartPredictDataType.ACTUAL;

                // Actual 데이터 소스 화폐 단위(SK on 처럼 Actual 여러개이면 Actual 모두 같은 화폐단위를 사용한다고 가정), 최초 설정만 유지
                actualGoodsDataSourceDetail.compareAndSet(null, goodsDataSource);
            }

            // 칼럼 목록 조회
            String[] tableColumnListNm = Arrays.stream(goodsDataSource.getTableColumnListNm().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            // 날짜 칼럼
            String dateColumnName = tableColumnListNm[MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_INDEX];
            // 기본 데이터 칼럼
            String defaultDataColumnName = tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            // Lasted Date 기준 (2025-01-01)
            LocalDate endDate = DateUtil.fromYearMonth(finalDate).plusMonths(chartPredictSetting.getPredictPeriodMonthsCnt())
                    .with(TemporalAdjusters.lastDayOfMonth());
            LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH);

            String cacheKey = getDashboardChartCacheKey(chartPredictSettingDetail.getChartPredictSettingDetailNo(), finalDate);

            // 캐시 조회
            String cachedData = redisTemplate.opsForValue().get(cacheKey);

            List<DashboardChartRawData> monthlyAverages = null;
            if (cachedData != null) {
                try {
                    monthlyAverages = objectMapper.readValue(cachedData, DashboardDataCache.class).monthlyAverages();
                } catch (Exception e) {
                    // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                    redisTemplate.delete(cacheKey);
                    log.error("마켓데이터 캐시 조회 실패 : {}", e.getMessage());
                }
            } else {
                // 예측 데이터와 정형데이터의 date 포맷이 다름
                // 차트 조회 종료 날짜(2025-01-01)
                String END_DATE = chartPredictSettingDetail.getIsChartLinePredictVal() ? DateUtil.toYmd(endDate) : DateUtil.toYmdHyphen(endDate);
                // 차트 조회 시작 날짜(2025-01-01)
                String START_DATE = chartPredictSettingDetail.getIsChartLinePredictVal() ? DateUtil.toYmd(startDate)
                        : DateUtil.toYmdHyphen(startDate);

                // 원시 데이터 조회 (IF절로 예측 데이터, 정형 데이터 조회 로직 분리)
                List<Map<String, Object>> rawData = new ArrayList<>();

                if (chartPredictSettingDetail.getIsChartLinePredictVal()) {
                    // 예측 데이터 조회
                    rawData = getForcastDataRows(goodsDataSource, dateColumnName, defaultDataColumnName, START_DATE, END_DATE);
                } else {
                    // 정형 + 선물 데이터 조회
                    rawData = getActualDataRows(goodsDataSource, dateColumnName, START_DATE, END_DATE);
                }

                // 예측 데이터인 경우
                if (chartPredictSettingDetail.getIsChartLinePredictVal()) {
                    // 예측 데이터가 있는 경우
                    if (!rawData.isEmpty()) {
                        // 마지막 데이터의 예측 수행 날짜 값
                        String lastUpdated = rawData.get(rawData.size() - 1).get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME).toString();
                        lastUpdatedList.add(DateUtil.fromYmd(lastUpdated));
                    }

                    dashboardDataSources.add(DashboardDataSource.of(goodsDataSource, chartPredictSettingDetail));
                }

                // 월별 날짜 생성
                List<LocalDate> monthlyDates = generateMonthlyDates(startDate, endDate);

                // 월별 평균 계산
                monthlyAverages = calculateMonthlyAverages(monthlyDates, rawData, dateColumnName, defaultDataColumnName,
                        chartPredictSettingDetail.getIsChartLinePredictVal());

                // 캐시 저장
                DashboardDataCache dashboardDataCache = DashboardDataCache.of(monthlyAverages);
                saveDashboardChartCache(cacheKey, dashboardDataCache);
            }

            DashboardChartLine dashboardChartLine = DashboardChartLine.of(isPermitted, chartPredictDataType, goodsDataSource.getGoodsDataSourceNo(),
                    monthlyAverages);

            // associatedCharts 업데이트
            Integer goodsDataSourceNo = chartPredictSettingDetail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo();
            chartMap.compute(goodsDataSourceNo, (key, existingChart) -> {
                if (existingChart == null) {
                    return DashboardChart.of(
                            chartPredictSettingDetail,
                            chartPredictSettingDetail.getIsChartLinePredictVal() ? null : dashboardChartLine,
                            chartPredictSettingDetail.getIsChartLinePredictVal() ? dashboardChartLine : null);
                } else {
                    // 기존 chartLineDescription, chartLineType 유지
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
        });

        // ConcurrentHashMap을 List<Map<Integer, DashboardChart>>로 변환
        List<Map<Integer, DashboardChart>> associatedCharts = chartMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .values().stream()
                .map(HashMap::new)
                .collect(Collectors.toList());

        // 예측 데이터 마지막 업데이트 중에 가장 날짜가 최근인 값
        LocalDate lastUpdated = lastUpdatedList.stream()
                .max(LocalDate::compareTo)
                .orElse(null);

        DashboardMetadata dashboardMetadata = DashboardMetadata.of(customerCompany.getMarketIndexAnalysisTargetCd(), lastUpdated,
                actualGoodsDataSourceDetail.get(), finalDate);

        // TODO: Response의 순서는 상관 없나??
        // associatedCharts에서 DashboardChart 리스트 추출
        List<DashboardChart> finalCharts = associatedCharts.stream()
                .flatMap(map -> map.values().stream())
                .map(chart -> {
                    DashboardChartLine actualLines = chart.actualChartLines() != null
                            ? chart.actualChartLines()
                            : DashboardChartLine.of(null, null, null, null);
                    DashboardChartLine forecastLines = chart.forecastChartLines() != null
                            ? chart.forecastChartLines()
                            : DashboardChartLine.of(null, null, null, null);
                    return new DashboardChart(
                            chart.chartLineDescription(),
                            chart.chartLineType(),
                            actualLines,
                            forecastLines);
                })
                .toList();

        return DashboardChartRes.of(dashboardMetadata, finalCharts, dashboardDataSources);
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
            String dateColumnName, String defaultDataColumnName, boolean isPredict) {
        // 결과 리스트
        List<DashboardChartRawData> monthlyAverages = new ArrayList<>();

        // DateTimeFormatter for YYYY-MM
        DateTimeFormatter yyyyMMFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // rawData를 월별로 그룹화
        Map<String, List<Double>> monthlyValues = new TreeMap<>();
        Map<String, String> monthlyPredictDates = new TreeMap<>(); // prdc_fulf_dt 저장
        for (Map<String, Object> row : rawData) {
            String yearMonth = null;
            String predictDate = null;

            // 날짜 칼럼 값이 LocalDateTime 타입인 경우
            if (row.get(dateColumnName) instanceof LocalDateTime) {
                yearMonth = DateUtil.toYyyyMm((LocalDateTime) row.get(dateColumnName));
            } else if (row.get(dateColumnName) instanceof String) {
                // 날짜 칼럼 값이 String 타입인 경우
                yearMonth = DateUtil.toYyyyMm(DateUtil.fromYmd((String) row.get(dateColumnName)));
            }

            // 예측 데이터인 경우에만 예측수행날짜가 있다
            if (isPredict) {
                predictDate = (String) row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                monthlyPredictDates.put(yearMonth, predictDate);
            }

            // 기본 데이터 칼럼 값
            Object val = row.get(defaultDataColumnName);
            Double valueData;

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
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                // 소수점 2자리
                BigDecimal bd = BigDecimal.valueOf(average)
                        .setScale(2, RoundingMode.HALF_UP);
                // 예측 데이터인 경우
                if (isPredict) {
                    dashboardChartRawData = DashboardChartRawData.ofForecast(yearMonth, null, bd.doubleValue());
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
     * 자정까지의 TTL 계산
     */
    private long getSecondsUntilMidnightForCache() {
        // LocalDateTime now = LocalDateTime.now();
        // LocalDateTime midnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        // return Duration.between(now, midnight).getSeconds();

        return Duration.ofHours(DASHBOARD_CHART_CACHE_TTL).getSeconds();
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
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(getSecondsUntilMidnightForCache()));
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
    @Transactional
    private List<Map<String, Object>> getForcastDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String valueColumnName,
            String startDate,
            String endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        String dbQueryConditionCntt = goodsDataSourceDetail.getQueryConditionCntt();
        String dbColumnListNm = goodsDataSourceDetail.getTableColumnListNm();

        // 정규 표현식 패턴을 이용해서 getTableColumnListNm 에서 칼럼, 데이터 추출
        String pattern = "([a-zA-Z_]+)\\s*=\\s*'([^']*)'\\s*and\\s*([a-zA-Z_]+)\\s*=\\s*'([^']*)'";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(dbQueryConditionCntt);

        String key1 = null;
        String value1 = null;
        String key2 = null;
        String value2 = null;

        if (matcher.matches()) {
            key1 = matcher.group(1);
            value1 = matcher.group(2);
            key2 = matcher.group(3);
            value2 = matcher.group(4);
        } else {
            throw new IllegalArgumentException("Invalid condition format: " + dbQueryConditionCntt);
        }

        String columns = String.format("%s, %s", dbColumnListNm, MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
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
                dateColumnName, startDate, endDate,
                key1, value1,
                key2, value2,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                key1, value1,
                key2, value2);
        String orderByColumns = String.format("%s", MARKET_DATA_PREDICT_DATE_COLUMN_NAME);

        return mybatisDynamicQueryService.getDynamicForcast(
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
    @Transactional
    private List<Map<String, Object>> getActualDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate,
            String endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        String whereClause = "%s >= '%s' AND %s <= '%s'".formatted(dateColumnName, startDate, dateColumnName, endDate);

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }

    /**
     * 차트 주요 요인 분석 카테고리 조회
     * 
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @return DashboardChartFactorCategoryRes
     */
    public DashboardChartFactorCategoryRes getChartFactorCategory(int companyNo, int goodsDataSourceNo, String predictDate) {
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        return DashboardChartFactorCategoryRes.of(dashboardMapper.chartFactorCategory(queryWhere, predictDate));
    }

    /**
     * 차트 주요 요인 분석 조회
     * 
     * @param goodsDataSourceNo 데이터소스 번호
     * @param predictDate 예측 날짜
     * @param explanatoryLevelNm 주요 요인 레벨명
     * @return DashboardChartFactorRes
     */
    public DashboardChartFactorRes getChartFactors(int companyNo, int goodsDataSourceNo, String predictDate, String explanatoryLevelNm) {

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> new EntityNotFoundException("CustomerCompany not found"));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        boolean isChartLinePredictVal = chartPredictSettingDetails.stream()
                .anyMatch(chartPredictSettingDetail -> {
                    GoodsDataSourceDetail goodsDataSourceDetail = chartPredictSettingDetail.getGoodsDataSourceDetail();
                    return goodsDataSourceDetail.getGoodsDataSourceNo() == goodsDataSourceNo
                            && chartPredictSettingDetail.getIsChartLinePredictVal() == true && chartPredictSettingDetail.getIsFuture() == false;
                });

        if (!isChartLinePredictVal) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("차트 예측 데이터가 없습니다. goodsDataSourceNo: %d", goodsDataSourceNo));
        }

        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
                .orElseThrow(() -> new EntityNotFoundException("GoodsDataSourceDetail not found"));

        String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();

        // 차트 주요 요인 분석 조회 (+)
        List<DashboardChartFactorMap> chartPlusFactorsMap = dashboardMapper.chartFactors(queryWhere, explanatoryLevelNm, predictDate,
                CHART_EXPLANATORY_PLUS_DIRECTION, CHART_EXPLANATORY_TOP_RANK_LIMIT);

        // 차트 주요 요인 분석 조회 (-)
        List<DashboardChartFactorMap> chartMinusFactorsMap = dashboardMapper.chartFactors(queryWhere, explanatoryLevelNm, predictDate,
                CHART_EXPLANATORY_MINUS_DIRECTION, CHART_EXPLANATORY_TOP_RANK_LIMIT);

        // Map -> Dto
        List<DashboardChartFactor> chartPlusFactors = chartPlusFactorsMap.stream()
                .map(DashboardChartFactor::of)
                .toList();

        // Map -> Dto
        List<DashboardChartFactor> chartMinusFactors = chartMinusFactorsMap.stream()
                .map(DashboardChartFactor::of)
                .toList();

        return DashboardChartFactorRes.of(explanatoryLevelNm, chartPlusFactors, chartMinusFactors);
    }
}
