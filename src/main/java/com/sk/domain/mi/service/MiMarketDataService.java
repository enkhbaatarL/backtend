package com.sk.domain.mi.service;

import com.sk.domain.mi.service.custom.CustomRegistry;
import com.sk.domain.mi.service.period.PeriodCalculator;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataCache;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryCache;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryRes;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChart;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaData;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaDataSummary;
import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import com.sk.domain.shared.repository.master.tcm.MarketDataChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiMarketDataService {

    public static final int MARKET_DATA_CACHE_TTL = 1; // 마켓데이터 캐시 유효 기간 (1초)
    // TODO: 캐시 유효 기간 수정 필요(테스트를 위해 임시로 1초로 설정)
    // private static final int MARKET_DATA_CACHE_TTL = 3600; // 마켓데이터 캐시 유효 기간 (1시간)

    public static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    public static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스
    public static final int MAIN_MARKET_DATA_START_DATE_MONTH = 13; // 마켓데이터 원시 데이터 조회 기준 날짜 (N달 전)
    private static final int MARKET_DATA_START_DATE_MONTH = 13; // 마켓데이터 원시 데이터 조회 기준 날짜 (N달 전)
    public static final int MARKET_DATA_MONTHLY_START_DATE_MONTH = 11; // 마켓데이터 월별 데이터 시작 날짜 (N달 전)
    private static final String MARKET_DATA_INDEX_START_DATE_LIMIT = "1"; // 원시데이터 시작 날짜 계산을 위한 LIMIT 조건
    private static final String MARKET_DATA_INTERVAL = "1M"; // 월별 평균 계산을 위한 기간

    private static final String MARKET_DATA_CACHE_KEY = "market_data:setting:%d";
    private static final String MARKET_DATA_SUMMARY_CACHE_KEY = "market_data_summary:setting:%d";
    private static final String MARKET_DATA_LAST_UPDATE_DATE_COLUMN_NAME = "mod_dtm"; // 마지막 업데이트 날짜 컬럼명

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final MiGoodsDataInquiryUserService miGoodsDataInquiryUserService;
    private final MarketDataChartSettingRepository marketDataChartSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final AuthorizationService authorizationService;
    private final CustomRegistry customRegistry;
    private final MiMarketDynamicQueryService miMarketDynamicQueryService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final EntityManager em;

    private final Map<String, PeriodCalculator> periodCalculators;
    private Map<CalculatePeriodChangeType, PeriodCalculator> periodCalculatorMap;

    @PostConstruct
    public void init(){
        periodCalculatorMap = new HashMap<>();
        for(PeriodCalculator periodCalculator : periodCalculators.values()){
            periodCalculatorMap.put(periodCalculator.getCalculatePeriodChangeType(), periodCalculator);
        }
    }

    /**
     * 마켓 데이터 조회
     *
     * @return 마켓 데이터 목록
     */
    public List<MiMarketDataRes> getMarketDatas(Integer companyNo) {
        LocalDate START_DATE = LocalDate.now().minusMonths(MAIN_MARKET_DATA_START_DATE_MONTH);
        LocalDate END_DATE = LocalDate.now();

        // 고객사 조회
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // 권한 조회
        PermissionRes permissionRes = authorizationService.getPermissions();

        // 차트 설정 조회
        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

    return marketDataChartSettings.parallelStream()
        .map(
            marketDataChartSetting -> {
              GoodsDataSourceDetail mainGoodsDataSource =
                  marketDataChartSetting.getGoodsDataSourceDetail();
              GoodsData mainGoodsData = mainGoodsDataSource.getGoodsData();
              Goods mainGoods = mainGoodsData.getGoods();

              // 권한 조회
              Boolean isPermitted =
                  permissionRes
                      .getGoodsViewableMap(companyNo)
                      .getOrDefault(mainGoods.getGoodsNo(), false);

              String cacheKey =
                  getMarketDataCacheKey(marketDataChartSetting.getMarketDataSettingNo());

              // 캐시 조회
              String cachedData = redisTemplate.opsForValue().get(cacheKey);
              if (cachedData != null) {
                try {
                  MiMarketDataCache miMarketDataCache =
                      objectMapper.readValue(cachedData, MiMarketDataCache.class);

                  // 권한이 없는 경우 메타데이터까지만 제공
                  if (!isPermitted) {
                    // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
                    MarketMetaData marketMetaData =
                        MarketMetaData.of(
                            marketDataChartSetting.getChartMarkRank(),
                            mainGoodsData,
                            mainGoodsDataSource,
                            new HashMap<>(),
                            MARKET_DATA_INTERVAL,
                            miMarketDataCache.marketMetaData().indexStartDt(),
                            miMarketDataCache.marketMetaData().lastValue(),
                            miMarketDataCache.marketMetaData().lastUpdateDt(),
                            null,
                            miMarketDataCache.marketMetaData().max(),
                            miMarketDataCache.marketMetaData().min());

                    return MiMarketDataRes.of(isPermitted, marketMetaData);
                  }

                  // 권한이 있는 경우 모든 데이터 제공
                  return MiMarketDataRes.of(
                      isPermitted,
                      miMarketDataCache.marketMetaData(),
                      miMarketDataCache.mainChart(),
                      miMarketDataCache.compareCharts());
                } catch (Exception e) {
                  // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                  redisTemplate.delete(cacheKey);
                  log.error("마켓데이터 캐시 조회 실패 : {}", e.getMessage());
                }
              }

              // 칼럼 목록 조회
              String[] tableColumnListNm =
                  Arrays.stream(mainGoodsDataSource.getTableColumnListNm().split(","))
                      .map(String::trim)
                      .toArray(String[]::new);
              // 날짜 칼럼
              String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
              // 기본 데이터 칼럼
              String defaultDataColumnName =
                  tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

              // 원시 데이터 조회
              List<Map<String, Object>> rawData =
                  miMarketDynamicQueryService.getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE, END_DATE);

              // Index Start date 데이터 조회
              LocalDate indexStartDate = null;
              List<Map<String, Object>> indexStartDateData =
                  getIndexStartDateData(mainGoodsDataSource, dateColumnName);
              if (indexStartDateData != null && !indexStartDateData.isEmpty()) {
                Map<String, Object> indexStartDateDataMap = indexStartDateData.get(0);
                if (indexStartDateDataMap != null) {
                  Object indexStartDateDataValue = indexStartDateDataMap.get(dateColumnName);
                  if (indexStartDateDataValue != null) {
                    String rawDate = indexStartDateDataValue.toString();
                    indexStartDate = DateUtil.parseDate(rawDate);
                  }
                }
              }

              // 마지막 업데이트 날짜 값
              LocalDate lastUpdateDt = null;
              List<Map<String, Object>> lastUpdateDtData =
                  getLastUpdateDtData(
                      mainGoodsDataSource, MARKET_DATA_LAST_UPDATE_DATE_COLUMN_NAME);
              if (lastUpdateDtData != null && !lastUpdateDtData.isEmpty()) {
                Map<String, Object> lastUpdateDtDataMap = lastUpdateDtData.get(0);
                if (lastUpdateDtDataMap != null) {
                  Object lastUpdateDtDataValue =
                      lastUpdateDtDataMap.get(MARKET_DATA_LAST_UPDATE_DATE_COLUMN_NAME);
                  if (lastUpdateDtDataValue != null) {
                    String rawDate = lastUpdateDtDataValue.toString();
                    lastUpdateDt = DateUtil.parseDate(rawDate);
                  }
                }
              }

              // 원시 데이터가 없는 경우 권한이 없는 것으로 처리
              if (rawData.isEmpty()) {
                MarketMetaData marketMetaData =
                    MarketMetaData.of(
                        marketDataChartSetting.getChartMarkRank(),
                        mainGoodsData,
                        mainGoodsDataSource,
                        MARKET_DATA_INTERVAL,
                        indexStartDate,
                        lastUpdateDt);
                MarketDataChart mainChart = MarketDataChart.of(Collections.emptyList());
                List<MarketDataChart> compareCharts = Collections.emptyList();

                MiMarketDataCache miMarketDataCache =
                    MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
                saveToMarketDataCache(cacheKey, miMarketDataCache);

                return MiMarketDataRes.of(isPermitted, marketMetaData, mainChart, compareCharts);
              }

              // 가장 최근 Raw 전날 Data 대비 가장 최근 Raw Data 계산 (증감률, 둘다 null이 아니라는 가정), 증감률 계산
              List<Map<String, Object>> recentChangeRate =
                  getRecentChangeRate(mainGoodsDataSource, dateColumnName, defaultDataColumnName);
              Double recentChangeRateValue = null;
              if (recentChangeRate.size() == 2) {
                // 최근 값
                Map<String, Object> toData = recentChangeRate.get(0);
                // 이전 값
                Map<String, Object> fromData = recentChangeRate.get(1);

                if (fromData.get(defaultDataColumnName) != null
                    && toData.get(defaultDataColumnName) != null) {
                  Double toValue = Double.parseDouble(toData.get(defaultDataColumnName).toString());
                  Double fromValue =
                      Double.parseDouble(fromData.get(defaultDataColumnName).toString());

                  // 계산식 (최근 값 - 이전 값) / 이전 값 * 100
                  Double changeRate = (toValue - fromValue) / fromValue * 100;
                  BigDecimal bd = BigDecimal.valueOf(changeRate).setScale(2, RoundingMode.HALF_UP);
                  recentChangeRateValue = bd.doubleValue();
                }
              }

              // 마지막 데이터 값
              Double lastValue = null;
              for (int i = 1; i <= rawData.size(); i++) {
                if (rawData.get(rawData.size() - i).get(defaultDataColumnName) != null) {
                  lastValue =
                      Double.parseDouble(
                          rawData.get(rawData.size() - i).get(defaultDataColumnName).toString());
                  BigDecimal bd = BigDecimal.valueOf(lastValue).setScale(2, RoundingMode.HALF_UP);
                  lastValue = bd.doubleValue();
                  break;
                }
              }

              // 월별 데이터로 변환
              List<MarketDataChartRawData> mainDashboardData =
                  MiMarketDynamicQueryService.convertToDashBoardData(
                      rawData,
                      dateColumnName,
                      defaultDataColumnName,
                      LocalDate.now().minusMonths(MARKET_DATA_MONTHLY_START_DATE_MONTH));

              MarketDataChart mainChart = MarketDataChart.of(mainDashboardData);

              Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> calculatePeriodChangeMap =
                  calculatePeriodChange(
                      mainGoodsDataSource,
                      rawData,
                      dateColumnName,
                      defaultDataColumnName,
                      marketDataChartSetting.toCalculatePeriodChangeTypes());

              // 비교군 데이터소스 조회
              List<DataSourceGroupMapping> dataSourceGroupMappings =
                  dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(
                      mainGoodsDataSource);

              // 비교군 데이터소스 조회
              List<MarketDataChart> compareCharts = new ArrayList<>();

              // 비교군 표시 안함
              if (true) {
                // if (dataSourceGroupMappings != null && !dataSourceGroupMappings.isEmpty()) {
                for (DataSourceGroupMapping dataSourceGroupMapping : dataSourceGroupMappings) {
                  GoodsDataSourceDetail compareGoodsDataSource =
                      dataSourceGroupMapping.getCompareGoodsDataSourceDetail();

                  String[] compareTableColumnListNm =
                      compareGoodsDataSource.getTableColumnListNm().split(",");
                  // 날짜 칼럼
                  String compareDateColumnName =
                      compareTableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
                  // 기본 데이터 칼럼
                  String compareDefaultDataColumnName =
                      compareTableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

                  // 비교군 원시 데이터 조회
                  List<Map<String, Object>> compareRawData =
                      miMarketDynamicQueryService.getMarketDataRows(
                          compareGoodsDataSource, compareDateColumnName, START_DATE, END_DATE);
                  // 비교군 데시보드형 데이터로 변환
                  List<MarketDataChartRawData> compareChart =
                      MiMarketDynamicQueryService.convertToDashBoardData(
                          compareRawData,
                          compareDateColumnName,
                          compareDefaultDataColumnName,
                          LocalDate.now().minusMonths(MARKET_DATA_MONTHLY_START_DATE_MONTH));

                  if (!Objects.equals(
                      mainGoodsDataSource.getGoodsData().getGoodsDataNo(),
                      compareGoodsDataSource.getGoodsData().getGoodsDataNo())) {
                    // 다른 데이터 이면, 그래프의 상하위가 비슷하게 변경
                    Double mainMax =
                        mainDashboardData.stream()
                            .filter(r -> r.value() != null)
                            .max(Comparator.comparing(MarketDataChartRawData::value))
                            .orElse(MarketDataChartRawData.of(null, null))
                            .value();

                    Double mainMin =
                        mainDashboardData.stream()
                            .filter(r -> r.value() != null)
                            .min(Comparator.comparing(MarketDataChartRawData::value))
                            .orElse(MarketDataChartRawData.of(null, null))
                            .value();

                    Double currentMax =
                        compareChart.stream()
                            .filter(r -> r.value() != null)
                            .max(Comparator.comparing(MarketDataChartRawData::value))
                            .orElse(MarketDataChartRawData.of(null, null))
                            .value();

                    Double currentMin =
                        compareChart.stream()
                            .filter(r -> r.value() != null)
                            .min(Comparator.comparing(MarketDataChartRawData::value))
                            .orElse(MarketDataChartRawData.of(null, null))
                            .value();

                    if (mainMax != null
                        && mainMin != null
                        && currentMax != null
                        && currentMin != null) {
                      compareChart =
                          compareChart.stream()
                              .map(
                                  r -> {
                                    if (r.value() == null) return r;
                                    return MarketDataChartRawData.of(
                                        r.date(),
                                        (r.value() - currentMin)
                                                * (mainMax - mainMin)
                                                / (currentMax - currentMin)
                                            + mainMin);
                                  })
                              .toList();
                    }
                  }
                  compareCharts.add(MarketDataChart.of(compareChart));
                }
              }

              // max, min
              MarketDataChartRawData min =
                  Collections.min(
                      mainDashboardData, Comparator.comparingDouble(MarketDataChartRawData::value));
              MarketDataChartRawData max =
                  Collections.max(
                      mainDashboardData, Comparator.comparingDouble(MarketDataChartRawData::value));

              // 마켓데이터 메타데이터 생성
              MarketMetaData marketMetaData =
                  MarketMetaData.of(
                      marketDataChartSetting.getChartMarkRank(),
                      mainGoodsData,
                      mainGoodsDataSource,
                      calculatePeriodChangeMap,
                      MARKET_DATA_INTERVAL,
                      indexStartDate,
                      lastValue,
                      lastUpdateDt,
                      recentChangeRateValue,
                      max,
                      min);

              // 캐시 저장
              MiMarketDataCache miMarketDataCache =
                  MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
              saveToMarketDataCache(cacheKey, miMarketDataCache);

              // 권한이 없는 경우 메타데이터까지만 제공
              if (!isPermitted) {
                // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
                MarketMetaData marketMetaDataNoPermitted =
                    MarketMetaData.of(
                        marketDataChartSetting.getChartMarkRank(),
                        mainGoodsData,
                        mainGoodsDataSource,
                        new HashMap<>(),
                        MARKET_DATA_INTERVAL,
                        indexStartDate,
                        lastValue,
                        lastUpdateDt,
                        null,
                        null,
                        null);

                return MiMarketDataRes.of(isPermitted, marketMetaDataNoPermitted);
              }

              return MiMarketDataRes.of(isPermitted, marketMetaData, mainChart, compareCharts);
            }) // 차트 정렬
        .filter(Objects::nonNull)
        .sorted(
            (a, b) -> {
              Short rankA = a.marketMetaData().chartMarkRank();
              Short rankB = b.marketMetaData().chartMarkRank();

              if (rankA == null && rankB == null) return 0;
              if (rankA == null) return 1;
              if (rankB == null) return -1;

              return rankA.compareTo(rankB);
            })
        .collect(Collectors.toList());
    }

    public Object getMarketDataCustom(Integer companyNo, String customId){
        return customRegistry.getCustomChartMaker(customId).getDashboardChart(companyNo);
    }

    /**
     * 마켓 데이터 요약 조회 (그래프 제외)
     *
     * @param customerCompanyNo 고객사 번호
     * @return 마켓 데이터 목록
     */
    public List<MiMarketDataSummaryRes> getMarketDataSummary(Integer customerCompanyNo) {
        LocalDate START_DATE = LocalDate.now().minusMonths(MARKET_DATA_START_DATE_MONTH);
        LocalDate END_DATE = LocalDate.now();

        // 고객사 조회
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(customerCompanyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", customerCompanyNo)));

        // 권한 조회
        PermissionRes permissionRes = authorizationService.getPermissions();

        // 차트 설정 조회
        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

    return marketDataChartSettings.parallelStream()
        .map(
            marketDataChartSetting -> {
              GoodsDataSourceDetail mainGoodsDataSource =
                  marketDataChartSetting.getGoodsDataSourceDetail();
              GoodsData mainGoodsData =
                  marketDataChartSetting.getGoodsDataSourceDetail().getGoodsData();
              Goods mainGoods = mainGoodsData.getGoods();

              // 권한 조회
              Boolean isPermitted =
                  permissionRes
                      .getGoodsViewableMap(customerCompanyNo)
                      .getOrDefault(mainGoods.getGoodsNo(), false);

              String cacheKey =
                  getMarketDataSummaryCacheKey(marketDataChartSetting.getMarketDataSettingNo());

              // 캐시 조회
              String cachedData = redisTemplate.opsForValue().get(cacheKey);
              if (cachedData != null) {
                try {
                  MiMarketDataSummaryCache miMarketDataSummaryCache =
                      objectMapper.readValue(cachedData, MiMarketDataSummaryCache.class);

                  // 권한이 없는 경우 메타데이터까지만 제공
                  if (!isPermitted) {
                    // 마켓데이터 메타데이터 생성
                    MarketMetaDataSummary marketMetaDataSummary =
                        MarketMetaDataSummary.of(
                            marketDataChartSetting,
                            mainGoodsData,
                            mainGoodsDataSource,
                            new HashMap<>(),
                            MARKET_DATA_INTERVAL,
                            miMarketDataSummaryCache.marketMetaDataSummary().indexStartDt(),
                            miMarketDataSummaryCache.marketMetaDataSummary().lastValue(),
                            miMarketDataSummaryCache.marketMetaDataSummary().lastUpdateDt());

                    return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
                  }

                  return MiMarketDataSummaryRes.of(
                      isPermitted, miMarketDataSummaryCache.marketMetaDataSummary());
                } catch (Exception e) {
                  // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                  redisTemplate.delete(cacheKey);
                  log.error("마켓데이터 요약 캐시 조회 실패 : {}", e.getMessage());
                }
              }

              // 칼럼 목록 조회
              String[] tableColumnListNm =
                  Arrays.stream(mainGoodsDataSource.getTableColumnListNm().split(","))
                      .map(String::trim)
                      .toArray(String[]::new);
              // 날짜 칼럼
              String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
              // 기본 데이터 칼럼
              String defaultDataColumnName =
                  tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

              // 원시 데이터 조회
              List<Map<String, Object>> rawData =
                  miMarketDynamicQueryService.getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE, END_DATE);

              // Index Start date 데이터 조회
              LocalDate indexStartDate = null;
              List<Map<String, Object>> indexStartDateData =
                  getIndexStartDateData(mainGoodsDataSource, dateColumnName);
              if (indexStartDateData != null && !indexStartDateData.isEmpty()) {
                Map<String, Object> indexStartDateDataMap = indexStartDateData.get(0);
                if (indexStartDateDataMap != null) {
                  Object indexStartDateDataValue = indexStartDateDataMap.get(dateColumnName);
                  if (indexStartDateDataValue != null) {
                    String rawDate = indexStartDateDataValue.toString();
                    indexStartDate = DateUtil.parseDate(rawDate);
                  }
                }
              }

              // 마지막 업데이트 날짜 값
              LocalDate lastUpdateDt = null;
              List<Map<String, Object>> lastUpdateDtData =
                  getLastUpdateDtData(
                      mainGoodsDataSource, MARKET_DATA_LAST_UPDATE_DATE_COLUMN_NAME);
              if (lastUpdateDtData != null && !lastUpdateDtData.isEmpty()) {
                Map<String, Object> lastUpdateDtDataMap = lastUpdateDtData.get(0);
                if (lastUpdateDtDataMap != null) {
                  Object lastUpdateDtDataValue =
                      lastUpdateDtDataMap.get(MARKET_DATA_LAST_UPDATE_DATE_COLUMN_NAME);
                  if (lastUpdateDtDataValue != null) {
                    String rawDate = lastUpdateDtDataValue.toString();
                    lastUpdateDt = DateUtil.parseDate(rawDate);
                  }
                }
              }

              // 원시 데이터가 없는 경우 권한이 없는 것으로 처리
              if (rawData.isEmpty()) {
                MarketMetaDataSummary marketMetaDataSummary =
                    MarketMetaDataSummary.of(marketDataChartSetting);

                MiMarketDataSummaryCache miMarketDataSummaryCache =
                    MiMarketDataSummaryCache.of(marketMetaDataSummary);
                saveToMarketDataSummaryCache(cacheKey, miMarketDataSummaryCache);

                return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
              }

              // 마지막 데이터 값
              Double lastValue = null;
              for (int i = 1; i <= rawData.size(); i++) {
                if (rawData.get(rawData.size() - i).get(defaultDataColumnName) != null) {
                  lastValue =
                      Double.parseDouble(
                          rawData.get(rawData.size() - i).get(defaultDataColumnName).toString());
                  BigDecimal bd = BigDecimal.valueOf(lastValue).setScale(2, RoundingMode.HALF_UP);
                  lastValue = bd.doubleValue();
                  break;
                }
              }

              // DOD, MOM, WOW 계산
              //            Set<CalculatePeriodChangeType> calculatePeriodChangeTypes =
              // Set.of(CalculatePeriodChangeType.DOD, CalculatePeriodChangeType.MOM,
              //                    CalculatePeriodChangeType.WOW);
              //            Map<CalculatePeriodChangeType, CalculatePeriodChangeValue>
              // calculatePeriodChangeMap = calculatePeriodChange(rawData, dateColumnName,
              // defaultDataColumnName,
              //                    calculatePeriodChangeTypes);

              Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> calculatePeriodChangeMap =
                  calculatePeriodChange(
                      mainGoodsDataSource,
                      rawData,
                      dateColumnName,
                      defaultDataColumnName,
                      marketDataChartSetting.toCalculatePeriodChangeTypes());

              // 마켓데이터 메타데이터 생성
              MarketMetaDataSummary marketMetaDataSummary =
                  MarketMetaDataSummary.of(
                      marketDataChartSetting,
                      mainGoodsData,
                      mainGoodsDataSource,
                      calculatePeriodChangeMap,
                      MARKET_DATA_INTERVAL,
                      indexStartDate,
                      lastValue,
                      lastUpdateDt);

              MiMarketDataSummaryCache miMarketDataSummaryCache =
                  MiMarketDataSummaryCache.of(marketMetaDataSummary);
              saveToMarketDataSummaryCache(cacheKey, miMarketDataSummaryCache);

              // 권한이 없는 경우 메타데이터까지만 제공
              if (!isPermitted) {
                // 마켓데이터 메타데이터 생성
                MarketMetaDataSummary marketMetaDataSummaryNoPermitted =
                    MarketMetaDataSummary.of(
                        marketDataChartSetting,
                        mainGoodsData,
                        mainGoodsDataSource,
                        new HashMap<>(),
                        MARKET_DATA_INTERVAL,
                        indexStartDate,
                        lastValue,
                        lastUpdateDt);

                return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummaryNoPermitted);
              }

              return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
            })
        .filter(Objects::nonNull)
        .sorted(
            (a, b) -> {
              Short rankA = a.marketMetaDataSummary().chartMarkRank();
              Short rankB = b.marketMetaDataSummary().chartMarkRank();

              if (rankA == null && rankB == null) return 0;
              if (rankA == null) return 1;
              if (rankB == null) return -1;
              return rankA.compareTo(rankB);
            })
        .collect(Collectors.toList());
    }

    /**
     * 마켓데이터 캐시 키 생성
     *
     * @param marketDataSettingNo 마켓데이터 설정 번호
     * @return 캐시 키
     */
    private String getMarketDataCacheKey(Integer marketDataSettingNo) {
        return String.format(MARKET_DATA_CACHE_KEY, marketDataSettingNo);
    }

    /**
     * 마켓데이터 요약 캐시 키 생성
     *
     * @param marketDataSettingNo 마켓데이터 설정 번호
     * @return 캐시 키
     */
    private String getMarketDataSummaryCacheKey(Integer marketDataSettingNo) {
        return String.format(MARKET_DATA_SUMMARY_CACHE_KEY, marketDataSettingNo);
    }

    /**
     * Redis에 MarketData 캐시 저장
     *
     * @param key 캐시 키
     * @param cache 캐시 객체
     */
    private void saveToMarketDataCache(String key, MiMarketDataCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(MARKET_DATA_CACHE_TTL));
        } catch (Exception e) {
            log.error("Failed to serialize and save cache for key: {}", key, e);
        }
    }

    /**
     * Redis에 MarketDataSummary 캐시 저장
     *
     * @param key 캐시 키
     * @param cache 캐시 객체
     */
    private void saveToMarketDataSummaryCache(String key, MiMarketDataSummaryCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(MARKET_DATA_CACHE_TTL));
        } catch (Exception e) {
            log.error("Failed to serialize and save cache for key: {}", key, e);
        }
    }

    /**
     * Index Start Date 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getIndexStartDateData(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {

        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        String selectColumns = String.format("""
                MIN(%s) AS %s
                """, dateColumnName, dateColumnName);

        String whereClause = goodsDataSourceDetail.getQueryConditionCntt() != null ? String.format("""
                %s
                AND del_yn = 'N'
                """, goodsDataSourceDetail.getQueryConditionCntt()) : String.format("""
                del_yn = 'N'
                """);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofLimit(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), selectColumns, whereClause,
                MARKET_DATA_INDEX_START_DATE_LIMIT));
    }

    /**
     * Index Start Date 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getLastUpdateDtData(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {

        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        String selectColumns = String.format("""
                MAX(%s) AS %s
                """, dateColumnName, dateColumnName);

        String whereClause = goodsDataSourceDetail.getQueryConditionCntt() != null ? String.format("""
                %s
                AND del_yn = 'N'
                """, goodsDataSourceDetail.getQueryConditionCntt()) : String.format("""
                del_yn = 'N'
                """);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofLimit(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), selectColumns, whereClause,
                MARKET_DATA_INDEX_START_DATE_LIMIT));
    }

    /**
     * 증감률 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    private List<Map<String, Object>> getRecentChangeRate(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName,
            String defaultDataColumnName) {
        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        String fromQuery = String.format("""
            (
                SELECT
                    %s
                    , DENSE_RANK() OVER (ORDER BY %s DESC) AS date_rank
                    , ROW_NUMBER() OVER (PARTITION BY %s ORDER BY %s DESC, %s DESC) AS rn
                FROM %s
                WHERE del_yn = 'N'
            """, goodsDataSourceDetail.getTableColumnListNm(), dateColumnName, dateColumnName, dateColumnName, defaultDataColumnName,
            goodsDataSourceDetail.getTargetTableNm());
        if (goodsDataSourceDetail.getQueryConditionCntt() != null) {
            fromQuery = fromQuery + String.format("""
                      AND %s
                """, goodsDataSourceDetail.getQueryConditionCntt());
        }
        fromQuery = fromQuery + """
                ) AS table_alias
                """;

        String whereClause = String.format("""
                date_rank <= 2 AND rn = 1
                        """);

        String orderByQuery = String.format("""
                %s DESC, %s DESC
                """, dateColumnName, defaultDataColumnName);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofOrderBy(goodsDataSourceDetail.getTargetDbNm(), fromQuery,
                goodsDataSourceDetail.getTableColumnListNm(), whereClause, orderByQuery));
    }

    /**
     * 월별 데이터에서 대표 날짜를 선택
     * - 기준: 오늘의 일자와 같은 날짜 (예: 2024-10-13)
     * - 데이터가 없으면 오늘 일자보다 빠른 날짜 중 가장 가까운 날짜
     * - 1일까지 데이터가 없으면 데이터가 있는 가장 빠른 날짜
     *
     * @param monthData 월별 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param yearMonth YYYYMM (예: 202410)
     * @param todayDayOfMonth 오늘의 일자 (예: 13)
     * @return 대표 날짜 (yyyy-MM-dd, 예: 2024-10-13)
     */
    private String selectRepresentativeDate(List<Map<String, Object>> monthData, String dateColumnName, String yearMonth, int todayDayOfMonth) {
        // 해당 월의 최대 일자 (예: 2024-10-31)
        LocalDate monthStart = LocalDate.parse(yearMonth + "01", DateUtil.YYYYMMDD);
        int maxDayOfMonth = monthStart.lengthOfMonth();
        // 기준 일자: 오늘의 일자와 같은 일자, 최대 일자 초과 시 조정
        int targetDay = Math.min(todayDayOfMonth, maxDayOfMonth);

        // 날짜별 데이터 맵 생성
        Map<LocalDate, Map<String, Object>> dateDataMap = new TreeMap<>();
        for (Map<String, Object> row : monthData) {
            LocalDate date = DateUtil.parseDate(row.get(dateColumnName));
            dateDataMap.put(date, row);
        }

        // 기준 날짜부터 역순으로 확인 (targetDay, targetDay-1, ...)
        for (int day = targetDay; day >= 1; day--) {
            LocalDate candidateDate = monthStart.withDayOfMonth(day);
            if (dateDataMap.containsKey(candidateDate)) {
                return candidateDate.format(DateUtil.YYYY_MM_DD); // 예: 2024-10-13
            }
        }

        // 1일까지 데이터가 없으면 가장 빠른 날짜 선택
        if (!dateDataMap.isEmpty()) {
            LocalDate earliestDate = dateDataMap.keySet().stream()
                    .min(LocalDate::compareTo)
                    .orElse(monthStart);
            return earliestDate.format(DateUtil.YYYY_MM_DD); // 예: 2024-10-05
        }

        // 데이터가 없으면 월의 첫 날 반환
        return monthStart.format(DateUtil.YYYY_MM_DD); // 예: 2024-10-01
    }

    /**
     * 기간 변화 수치, 변화율 계산
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @param calculatePeriodChangeTypes 기간 변화 유형
     * @return 기간 변화 수치, 변화율 계산 결과
     */
    public Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> calculatePeriodChange(
            GoodsDataSourceDetail mainGoodsDataSource,
            List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName,
            Set<CalculatePeriodChangeType> calculatePeriodChangeTypes) {

        // 순서 중요
        Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> result = new LinkedHashMap<>();
        for(CalculatePeriodChangeType calculatePeriodChangeType : calculatePeriodChangeTypes){
            PeriodCalculator periodCalculator = periodCalculatorMap.get(calculatePeriodChangeType);
            CalculatePeriodChangeValue calculatePeriodChangeValue = periodCalculator.calculatePeriodChange(mainGoodsDataSource, rawData, dateColumnName, defaultDataColumnName);
            if(calculatePeriodChangeValue == null) continue;
            result.put(calculatePeriodChangeType, calculatePeriodChangeValue);
        }
        return result;
    }


    /**
     * Market Data - Data Catalog 목록 조회
     *
     * @param companyNo 고객사 번호
     * @param page      페이지 번호
     * @param size      페이지 사이즈
     * @param keyword   검색어
     * @return PaginationRes<DataCatalogRes>
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaginationRes<DataCatalogRes> getDataCatalog(
            int companyNo,
            int page,
            int size,
            String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        String select = """
                tp.prvd_no AS providerNo,
                tg.gds_no AS goodsNo,
                tgd.gds_data_no AS goodsDataNo,
                tgd.gds_data_nm AS goodsDataNm,
                tgsrc.gds_data_src_no AS goodsDataSrcNo,
                tgsrc.gds_data_src_nm AS goodsDataSrcNm,
                tgsrc.mod_dtm AS modifyDt,
                tgsrc.tgt_db_nm AS targetDbNm,
                tgsrc.tgt_tbl_nm AS targetTableNm,
                tgsrc.qry_cdtn_cntt AS queryConditionCntt,
                tgsrc.tbl_colm_list_nm AS tableColumnListNm
                """;

        String where = """
                tcp.del_yn = 'N' AND tcp.use_yn = 'Y' AND tcp.cust_cpny_no = :companyNo
                """;

        if (StringUtils.hasText(keyword)) {
            where += " AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)";
        }

        String contentSql = """
                SELECT %s
                FROM midb.tam_custcpnyprvdmappl tcp
                JOIN midb.tam_custcpnygdsmappl tcg ON tcg.cust_cpny_no = tcp.cust_cpny_no AND tcg.del_yn = 'N' AND tcg.use_yn = 'Y'
                JOIN midb.tmd_prvdm tp ON tp.prvd_no = tcp.prvd_no AND tp.del_yn = 'N' AND tp.use_yn = 'Y'
                JOIN midb.tmd_gdsm tg ON tg.gds_no = tcg.gds_no AND tg.prvd_no = tcp.prvd_no AND tg.del_yn = 'N' AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tcg.gds_no AND tgd.gds_no = tcg.gds_no AND tgd.del_yn = 'N' AND tgd.gds_data_mark_yn = 'Y'
                JOIN midb.tmd_gdsdatasrcd tgsrc ON tgsrc.gds_data_no = tgd.gds_data_no AND tgsrc.del_yn = 'N' AND tgsrc.gds_data_use_mnagt_cd = 'backend'
                WHERE %s
                ORDER BY tgd.gds_data_nm ASC, tgsrc.gds_data_src_nm ASC
                """
                .formatted(select, where);

        String countSql = """
                SELECT COUNT(*) AS totalCount
                FROM midb.tam_custcpnyprvdmappl tcp
                JOIN midb.tam_custcpnygdsmappl tcg ON tcg.cust_cpny_no = tcp.cust_cpny_no AND tcg.del_yn = 'N' AND tcg.use_yn = 'Y'
                JOIN midb.tmd_prvdm tp ON tp.prvd_no = tcp.prvd_no AND tp.del_yn = 'N' AND tp.use_yn = 'Y'
                JOIN midb.tmd_gdsm tg ON tg.gds_no = tcg.gds_no AND tg.prvd_no = tcp.prvd_no AND tg.del_yn = 'N' AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tcg.gds_no AND tgd.gds_no = tcg.gds_no AND tgd.del_yn = 'N' AND tgd.gds_data_mark_yn = 'Y'
                JOIN midb.tmd_gdsdatasrcd tgsrc ON tgsrc.gds_data_no = tgd.gds_data_no AND tgsrc.del_yn = 'N' AND tgsrc.gds_data_use_mnagt_cd = 'backend'
                WHERE %s
                """
                .formatted(where);

        Query contentQuery = entityManager.createNativeQuery(contentSql)
                .setParameter("companyNo", companyNo)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size);

        Query countQuery = entityManager.createNativeQuery(countSql)
                .setParameter("companyNo", companyNo);

        if (StringUtils.hasText(keyword)) {
            contentQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
            countQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        PermissionRes permissionRes = authorizationService.getPermissions();

        List<DataCatalogRes> result = rows.stream().parallel().map(row -> {

            int providerNo = (Integer) row[0];
            int goodsNo = (Integer) row[1];
            int goodsDataNo = (Integer) row[2];
            String goodsDataNm = (String) row[3];
            int goodsDataSrcNo = (Integer) row[4];
            String goodsDataSrcNm = (String) row[5];
            LocalDateTime modifyDt = ((Timestamp) row[6]).toLocalDateTime();
            String targetDbNm = (String) row[7];
            String targetTableNm = (String) row[8];
            String queryConditionCntt = (String) row[9];
            String tableColumnListNm = (String) row[10];

            boolean isViewable = permissionRes.getGoodsViewableMap(companyNo).getOrDefault(goodsNo, Boolean.FALSE);

            Double lastValue = 0.0;
            LocalDateTime modDtm = modifyDt;
            if (isViewable) {
                if (targetDbNm != null && targetTableNm != null && queryConditionCntt != null && tableColumnListNm != null) {
                    // 컬럼 목록을 쉼표로 분리하고 공백 제거
                    String[] cols = Arrays.stream(tableColumnListNm.split(","))
                            .map(String::trim)
                            .toArray(String[]::new);

                    if (cols.length >= 2) {
                        // 최소 2개의 컬럼이 필요 (날짜, 기본 데이터)
                        LastValueDto lastValueDto = getLastValue(
                                targetDbNm, targetTableNm, queryConditionCntt,
                                cols[MARKET_DATA_DATE_COLUMN_INDEX], cols[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX]);

                        lastValue = lastValueDto.value();
                        modDtm = lastValueDto.modDtm();
                    }
                }
            }

            return new DataCatalogRes(
                    providerNo, goodsNo, goodsDataNo,
                    goodsDataNm, goodsDataSrcNo, goodsDataSrcNm,
                    isViewable, lastValue, modDtm);
        })
                .toList();

        return PaginationRes.toRes(PageableExecutionUtils.getPage(result, pageable, () -> total));
    }

    private record LastValueDto(
            Double value,
            LocalDateTime modDtm) {
    }

    private LastValueDto getLastValue(String targetDbNm, String targetTableNm, String queryConditionCntt, String dateColumnName,
            String valueColumnName) {

        if (!(StringUtils.hasText(targetDbNm) && StringUtils.hasText(targetTableNm) && StringUtils.hasText(queryConditionCntt)
                && StringUtils.hasText(dateColumnName) && StringUtils.hasText(valueColumnName))) {
            return new LastValueDto(0.0, null);
        }

        DataSourceContextHolder.setDataSourceKey(targetDbNm);
        try {
            String sql = String.format("""
                    SELECT %s, mod_dtm
                    FROM `%s`.%s
                    WHERE %s
                    ORDER BY %s DESC
                    LIMIT 1
                    """,
                    valueColumnName, // 컬럼
                    targetDbNm, targetTableNm, // DB.테이블
                    queryConditionCntt, // 조건식
                    dateColumnName); // 정렬

            Object result = em.createNativeQuery(sql).getSingleResult();

            Object[] row = (result instanceof Object[] arr) ? arr : new Object[] {result, null};

            Double val = row[0] != null
                    ? ((Number) row[0]).doubleValue()
                    : 0.0;
            LocalDateTime modDtm = null;
            if (row[1] != null && row[1] instanceof Timestamp ts) {
                modDtm = ts.toLocalDateTime();
            }

            return new LastValueDto(val, modDtm);
        } catch (NoResultException e) {
            // 조회 결과가 없을 때
            return new LastValueDto(0.0, null);
        } catch (Exception e) {
            // 기타 예외 처리
            return new LastValueDto(0.0, null);
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    /**
     * Market Data - Recent Viewed 최신 5개 조회
     *
     * @param userNo 사용자 번호
     * @return List<RecentViewedRes>
     */
    public List<RecentViewedRes> getRecentViewed(int companyNo, int userNo) {
        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));

        return miGoodsDataInquiryUserService.getGoodsDataInquiryUser(companyNo, userNo, user.getUserLoginId(), PageRequest.of(0, 5));
    }
}
