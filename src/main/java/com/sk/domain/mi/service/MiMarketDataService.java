package com.sk.domain.mi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
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
import com.sk.common.utils.CombineUtil;
import com.sk.common.utils.DateBindingUtil;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataCache;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryCache;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryRes;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeRange;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeRes;
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
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
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

    private static final int MARKET_DATA_CACHE_TTL = 1; // 마켓데이터 캐시 유효 기간 (1초)
    //TODO: 캐시 유효 기간 수정 필요(테스트를 위해 임시로 1초로 설정)
    // private static final int MARKET_DATA_CACHE_TTL = 3600; // 마켓데이터 캐시 유효 기간 (1시간)

    private static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스
    private static final int MARKET_DATA_START_DATE_YEAR = 2; // 마켓데이터 원시 데이터 조회 기준 날짜 (N년 전)
    private static final int MARKET_DATA_START_DATE_MONTH = 2; // 마켓데이터 원시 데이터 조회 기준 날짜 (N달 전)
    private static final int MARKET_DATA_MONTHLY_START_DATE_MONTH = 11; // 마켓데이터 월별 데이터 시작 날짜 (N달 전)
    private static final String MARKET_DATA_INDEX_START_DATE_LIMIT = "1"; // 원시데이터 시작 날짜 계산을 위한 LIMIT 조건
    private static final String MARKET_DATA_INTERVAL = "1M"; // 월별 평균 계산을 위한 기간

    private static final String MARKET_DATA_CACHE_KEY = "market_data:setting:%d";
    private static final String MARKET_DATA_SUMMARY_CACHE_KEY = "market_data_summary:setting:%d";

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final MiGoodsDataInquiryUserService miGoodsDataInquiryUserService;
    private final MarketDataChartSettingRepository marketDataChartSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final MiGoodsPermissionService miGoodsPermissionService;
    private final AuthorizationService authorizationService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final EntityManager em;

    /**
     * 마켓 데이터 조회
     *
     * @param customerCompanyNo 고객사 번호
     * @param authentication 인증 정보
     * @return 마켓 데이터 목록
     */
    public List<MiMarketDataRes> getMarketDatas(Integer companyNo) {
        // String START_DATE = DateUtil.toYmdHyphen(LocalDate.now().minusYears(MARKET_DATA_START_DATE_YEAR)); // 2년 전
        LocalDate START_DATE = LocalDate.now().minusYears(MARKET_DATA_START_DATE_YEAR);
        LocalDate END_DATE = LocalDate.now();

        // 고객사 조회
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalseAndIsUsedTrue(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
        // throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "사용자 권한 조회 실패");
        // }
        // Role role = Role.valueOf(jwtToken.getToken().getClaimAsString(Claim.ROLE.getCode()));

        // // 권한 조회
        // PermissionRes permissionRes = switch (role) {
        // case USER -> {
        // Integer userNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.USER_NO.getCode()));
        // yield miGoodsPermissionService.getUserGoodsPermissions(userNo);
        // }
        // case ADMIN, API_SUB_ADMIN -> miGoodsPermissionService.getAccessAdminGoodsPermissions();
        // case API_PLUS_OPERATOR -> {
        // Integer operatorCompanyNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.COMPANY_NO.getCode()));
        // yield miGoodsPermissionService.getPlusOperatorGoodsPermissions(operatorCompanyNo);
        // }
        // default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        // };

        PermissionRes permissionRes = authorizationService.getPermissions();

        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

        return marketDataChartSettings.parallelStream().map(marketDataChartSetting -> {
            GoodsDataSourceDetail mainGoodsDataSource = marketDataChartSetting.getGoodsDataSourceDetail();
            GoodsData mainGoodsData = mainGoodsDataSource.getGoodsData();
            Goods mainGoods = mainGoodsData.getGoods();

            // 권한 조회
            Boolean isPermitted = permissionRes.getGoodsViewableMap(companyNo).getOrDefault(mainGoods.getGoodsNo(), false);

            String cacheKey = getMarketDataCacheKey(marketDataChartSetting.getMarketDataSettingNo());

            // 캐시 조회
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                try {
                    MiMarketDataCache miMarketDataCache = objectMapper.readValue(cachedData, MiMarketDataCache.class);

                    // 권한이 없는 경우 메타데이터까지만 제공
                    if (!isPermitted) {
                        // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
                        MarketMetaData marketMetaData = MarketMetaData.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource, null,
                                MARKET_DATA_INTERVAL, miMarketDataCache.marketMetaData().indexStartDt(),
                                miMarketDataCache.marketMetaData().lastValue(), miMarketDataCache.marketMetaData().lastUpdateDt());

                        return MiMarketDataRes.of(isPermitted, marketMetaData);
                    }

                    // 권한이 있는 경우 모든 데이터 제공
                    return MiMarketDataRes.of(isPermitted, miMarketDataCache.marketMetaData(),
                            miMarketDataCache.mainChart(), miMarketDataCache.compareCharts());
                } catch (Exception e) {
                    // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                    redisTemplate.delete(cacheKey);
                    log.error("마켓데이터 캐시 조회 실패 : {}", e.getMessage());
                }
            }

            // 칼럼 목록 조회
            String[] tableColumnListNm = Arrays.stream(mainGoodsDataSource.getTableColumnListNm().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            // 날짜 칼럼
            String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
            // 기본 데이터 칼럼
            String defaultDataColumnName = tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            // 원시 데이터 조회
            List<Map<String, Object>> rawData = getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE, END_DATE);

            // 원시 데이터가 없는 경우 권한이 없는 것으로 처리
            if (rawData.isEmpty()) {
                isPermitted = false;

                MarketMetaData marketMetaData = MarketMetaData.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource, MARKET_DATA_INTERVAL);
                MarketDataChart mainChart = MarketDataChart.of(Collections.emptyList());
                List<MarketDataChart> compareCharts = Collections.emptyList();

                MiMarketDataCache miMarketDataCache = MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
                saveToMarketDataCache(cacheKey, miMarketDataCache);

                return MiMarketDataRes.of(isPermitted, marketMetaData, mainChart, compareCharts);
            }

            // 마지막 데이터 값
            LocalDate lastUpdateDt = DateUtil.parseDate(rawData.get(rawData.size() - 1).get(dateColumnName).toString());

            // 마지막 데이터 값
            Double lastValue = Double.parseDouble(rawData.get(rawData.size() - 1).get(defaultDataColumnName).toString());
            BigDecimal bd = BigDecimal.valueOf(lastValue).setScale(2, RoundingMode.HALF_UP);
            lastValue = bd.doubleValue();

            // Index Start date 데이터 조회
            LocalDate indexStartDate;
            List<Map<String, Object>> indexStartDateData = getIndexStartDateData(mainGoodsDataSource, dateColumnName);
            if (!indexStartDateData.isEmpty()) {
                String rawDate = indexStartDateData.get(0).get(dateColumnName).toString();
                indexStartDate = DateUtil.parseDate(rawDate);
            } else {
                indexStartDate = LocalDate.now().minusYears(MARKET_DATA_START_DATE_YEAR);
            }

            // 월별 데이터로 변환
            List<MarketDataChartRawData> monthlyData = convertToMonthlyData(rawData, dateColumnName, defaultDataColumnName,
                    LocalDate.now().minusMonths(MARKET_DATA_MONTHLY_START_DATE_MONTH));

            MarketDataChart mainChart = MarketDataChart.of(monthlyData);

            // 계산 하고자 하는 기간 변화 유형(YOY, MOM, WOW)
            Set<CalculatePeriodChangeType> calculatePeriodChangeTypes = Set.of(CalculatePeriodChangeType.MOM, CalculatePeriodChangeType.WOW,
                    CalculatePeriodChangeType.YOY);
            // 기간 변화 유형별 계산
            CalculatePeriodChangeRes calculatePeriodChangeRes = calculatePeriodChange(rawData, dateColumnName, defaultDataColumnName,
                    calculatePeriodChangeTypes);

            // 비교군 데이터소스 조회
            List<DataSourceGroupMapping> dataSourceGroupMappings = dataSourceGroupMappingRepository
                    .findByMainGoodsDataSourceDetailAndIsDeletedFalse(mainGoodsDataSource);

            // 비교군 데이터소스 조회
            List<MarketDataChart> compareCharts = new ArrayList<>();
            if (dataSourceGroupMappings != null && !dataSourceGroupMappings.isEmpty()) {
                for (DataSourceGroupMapping dataSourceGroupMapping : dataSourceGroupMappings) {
                    GoodsDataSourceDetail compareGoodsDataSource = dataSourceGroupMapping.getCompareGoodsDataSourceDetail();

                    String[] compareTableColumnListNm = compareGoodsDataSource.getTableColumnListNm().split(",");
                    // 날짜 칼럼
                    String compareDateColumnName = compareTableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
                    // 기본 데이터 칼럼
                    String compareDefaultDataColumnName = compareTableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

                    // 비교군 원시 데이터 조회
                    List<Map<String, Object>> compareRawData = getMarketDataRows(compareGoodsDataSource, compareDateColumnName, START_DATE, END_DATE);
                    // 비교군 월별 데이터로 변환
                    List<MarketDataChartRawData> compareChart = convertToMonthlyData(compareRawData, compareDateColumnName,
                            compareDefaultDataColumnName, LocalDate.now().minusMonths(MARKET_DATA_MONTHLY_START_DATE_MONTH));
                    compareCharts.add(MarketDataChart.of(compareChart));
                }
            }

            // 마켓데이터 메타데이터 생성
            MarketMetaData marketMetaData = MarketMetaData.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource, calculatePeriodChangeRes,
                    MARKET_DATA_INTERVAL, indexStartDate,
                    lastValue, lastUpdateDt);

            // 캐시 저장
            MiMarketDataCache miMarketDataCache = MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
            saveToMarketDataCache(cacheKey, miMarketDataCache);

            // 권한이 없는 경우 메타데이터까지만 제공
            if (!isPermitted) {
                // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
                MarketMetaData marketMetaDataNoPermitted = MarketMetaData.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource, null,
                        MARKET_DATA_INTERVAL, indexStartDate,
                        lastValue, lastUpdateDt);

                return MiMarketDataRes.of(isPermitted, marketMetaDataNoPermitted);
            }

            return MiMarketDataRes.of(isPermitted, marketMetaData, mainChart, compareCharts);

        }) // 차트 정렬
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    Short rankA = a.marketMetaData().chartMarkRank();
                    Short rankB = b.marketMetaData().chartMarkRank();

                    if (rankA == null && rankB == null)
                        return 0;
                    if (rankA == null)
                        return 1;
                    if (rankB == null)
                        return -1;

                    return rankA.compareTo(rankB);
                })
                .collect(Collectors.toList());
    }

    /**
     * 마켓 데이터 요약 조회 (그래프 제외)
     *
     * @param customerCompanyNo 고객사 번호
     * @return 마켓 데이터 목록
     */
    public List<MiMarketDataSummaryRes> getMarketDataSummary(Integer customerCompanyNo) {
        // String START_DATE = DateUtil.toYmdHyphen(LocalDate.now().minusMonths(MARKET_DATA_START_DATE_MONTH)); // 2달 전
        LocalDate START_DATE = LocalDate.now().minusMonths(MARKET_DATA_START_DATE_MONTH);
        LocalDate END_DATE = LocalDate.now();

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalseAndIsUsedTrue(customerCompanyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", customerCompanyNo)));

        PermissionRes permissionRes = authorizationService.getPermissions();

        // 차트 설정 조회
        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

        return marketDataChartSettings.parallelStream().map(marketDataChartSetting -> {
            GoodsDataSourceDetail mainGoodsDataSource = marketDataChartSetting.getGoodsDataSourceDetail();
            GoodsData mainGoodsData = marketDataChartSetting.getGoodsDataSourceDetail().getGoodsData();
            Goods mainGoods = mainGoodsData.getGoods();

            // 권한 조회
            Boolean isPermitted = permissionRes.getGoodsViewableMap(customerCompanyNo).getOrDefault(mainGoods.getGoodsNo(), false);

            String cacheKey = getMarketDataSummaryCacheKey(marketDataChartSetting.getMarketDataSettingNo());

            // 캐시 조회
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                try {
                    MiMarketDataSummaryCache miMarketDataSummaryCache = objectMapper.readValue(cachedData, MiMarketDataSummaryCache.class);

                    // 권한이 없는 경우 메타데이터까지만 제공
                    if (!isPermitted) {
                        // 마켓데이터 메타데이터 생성
                        MarketMetaDataSummary marketMetaDataSummary = MarketMetaDataSummary.of(marketDataChartSetting, mainGoodsData,
                                mainGoodsDataSource,
                                null, MARKET_DATA_INTERVAL, miMarketDataSummaryCache.marketMetaDataSummary().indexStartDt(),
                                miMarketDataSummaryCache.marketMetaDataSummary().lastValue(),
                                miMarketDataSummaryCache.marketMetaDataSummary().lastUpdateDt());

                        return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
                    }

                    return MiMarketDataSummaryRes.of(isPermitted, miMarketDataSummaryCache.marketMetaDataSummary());
                } catch (Exception e) {
                    // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                    redisTemplate.delete(cacheKey);
                    log.error("마켓데이터 요약 캐시 조회 실패 : {}", e.getMessage());
                }
            }

            // 칼럼 목록 조회
            String[] tableColumnListNm = Arrays.stream(mainGoodsDataSource.getTableColumnListNm().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            // 날짜 칼럼
            String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
            // 기본 데이터 칼럼
            String defaultDataColumnName = tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            // 원시 데이터 조회
            List<Map<String, Object>> rawData = getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE, END_DATE);

            // 원시 데이터가 없는 경우 권한이 없는 것으로 처리
            if (rawData.isEmpty()) {
                isPermitted = false;

                MarketMetaDataSummary marketMetaDataSummary = MarketMetaDataSummary.of(marketDataChartSetting);

                MiMarketDataSummaryCache miMarketDataSummaryCache = MiMarketDataSummaryCache.of(marketMetaDataSummary);
                saveToMarketDataSummaryCache(cacheKey, miMarketDataSummaryCache);

                return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
            }

            // 마지막 데이터 날짜 값
            LocalDate lastUpdateDt = DateUtil.parseDate(rawData.get(rawData.size() - 1).get(dateColumnName));

            // 마지막 데이터 값
            Double lastValue = Double.parseDouble(rawData.get(rawData.size() - 1).get(defaultDataColumnName).toString());
            BigDecimal bd = BigDecimal.valueOf(lastValue).setScale(2, RoundingMode.HALF_UP);
            lastValue = bd.doubleValue();

            // Index Start date 데이터 조회
            LocalDate indexStartDate;
            List<Map<String, Object>> indexStartDateData = getIndexStartDateData(mainGoodsDataSource, dateColumnName);
            if (!indexStartDateData.isEmpty()) {
                String rawDate = indexStartDateData.get(0).get(dateColumnName).toString();
                indexStartDate = DateUtil.parseDate(rawDate);
            } else {
                indexStartDate = LocalDate.now().minusYears(MARKET_DATA_START_DATE_YEAR);
            }

            // DOD, MOM, WOW 계산
            Set<CalculatePeriodChangeType> calculatePeriodChangeTypes = Set.of(CalculatePeriodChangeType.DOD, CalculatePeriodChangeType.MOM,
                    CalculatePeriodChangeType.WOW);
            CalculatePeriodChangeRes calculatePeriodChangeRes = calculatePeriodChange(rawData, dateColumnName, defaultDataColumnName,
                    calculatePeriodChangeTypes);

            // 마켓데이터 메타데이터 생성
            MarketMetaDataSummary marketMetaDataSummary = MarketMetaDataSummary.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource,
                    calculatePeriodChangeRes,
                    MARKET_DATA_INTERVAL, indexStartDate,
                    lastValue, lastUpdateDt);

            MiMarketDataSummaryCache miMarketDataSummaryCache = MiMarketDataSummaryCache.of(marketMetaDataSummary);
            saveToMarketDataSummaryCache(cacheKey, miMarketDataSummaryCache);

            // 권한이 없는 경우 메타데이터까지만 제공
            if (!isPermitted) {
                // 마켓데이터 메타데이터 생성
                MarketMetaDataSummary marketMetaDataSummaryNoPermitted = MarketMetaDataSummary.of(marketDataChartSetting, mainGoodsData,
                        mainGoodsDataSource,
                        null, MARKET_DATA_INTERVAL, indexStartDate, lastValue, lastUpdateDt);

                return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummaryNoPermitted);
            }

            return MiMarketDataSummaryRes.of(isPermitted, marketMetaDataSummary);
        })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    Short rankA = a.marketMetaDataSummary().chartMarkRank();
                    Short rankB = b.marketMetaDataSummary().chartMarkRank();

                    if (rankA == null && rankB == null)
                        return 0;
                    if (rankA == null)
                        return 1;
                    if (rankB == null)
                        return -1;
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
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @param startDate             데이터 범위 시작일 (포함, yyyy-MM-dd 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional
    private List<Map<String, Object>> getMarketDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, LocalDate startDate,
            LocalDate endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null) {
            return Collections.emptyList();
        }

        // 날짜 컬럼 스키마 조회
        SchemaMetadata meta = mybatisDynamicQueryService.getMeta(DynamicDataReq.ofMetaQuery(goodsDataSourceDetail));
        Pair<String, String> dateRange = DateBindingUtil.range(meta, startDate, endDate);

        String whereClause = CombineUtil.createBetweenClause(dateColumnName, dateRange.getLeft(), dateRange.getRight());

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }

    /**
     * Index Start Date 데이터 조회
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        날짜 컬럼명
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional
    private List<Map<String, Object>> getIndexStartDateData(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {

        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofLimit(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), dateColumnName, goodsDataSourceDetail.getQueryConditionCntt(),
                MARKET_DATA_INDEX_START_DATE_LIMIT));
    }

    /**
     * 월별 평균 계산 및 DTO 생성
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @param startDate 월별 평균 계산 기준 날짜
     * @return 월별 평균 계산 및 DTO 생성
     */
    private List<MarketDataChartRawData> convertToMonthlyData(List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName,
            LocalDate startDate) {
        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        // startDate를 1년 전 다음 달 첫 번째 날로 조정 (예: 2024-06-01)
        LocalDate adjustedStartDate = startDate.withDayOfMonth(1);
        // endDate를 이번 달 첫 날로 설정 (예: 2025-05-01)
        LocalDate endDate = LocalDate.now().withDayOfMonth(1);
        // 오늘의 일자 (예: 13)
        int todayDayOfMonth = LocalDate.now().getDayOfMonth();

        // 월별로 데이터 그룹화
        Map<String, List<Map<String, Object>>> monthlyGroupedData = new TreeMap<>();
        for (Map<String, Object> row : rawData) {
            LocalDate date = DateUtil.parseDate(row.get(dateColumnName));

            // e.g. 2024-06-01 ~ 2025-05-31 범위 내 데이터만 처리
            if (date.isBefore(adjustedStartDate) || date.isAfter(endDate.withDayOfMonth(endDate.lengthOfMonth()))) {
                continue;
            }

            String yearMonth = DateUtil.toYearMonth(date); // YYYYMM (예: 202406)

            monthlyGroupedData.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(row);
        }

        // 월별 평균 계산 및 DTO 생성
        List<MarketDataChartRawData> monthlyData = new ArrayList<>();
        LocalDate currentMonth = adjustedStartDate;
        while (!currentMonth.isAfter(endDate)) {
            String yearMonth = DateUtil.toYearMonth(currentMonth); // 예: "202406"
            List<Map<String, Object>> monthData = monthlyGroupedData.getOrDefault(yearMonth, Collections.emptyList());

            // 대표 날짜 선택
            String representativeDate;
            if (monthData.isEmpty()) {
                // 데이터 없는 경우: 오늘의 일자 또는 월의 마지막 날
                int dayOfMonth = Math.min(todayDayOfMonth, currentMonth.lengthOfMonth());
                representativeDate = DateUtil.toYmdHyphen(currentMonth.withDayOfMonth(dayOfMonth));
            } else {
                // 데이터 있는 경우: 기존 로직
                representativeDate = selectRepresentativeDate(monthData, dateColumnName, yearMonth, todayDayOfMonth);
            }

            // 평균 계산
            Double value = null;
            if (!monthData.isEmpty()) {
                double sum = 0.0;
                int count = 0;
                for (Map<String, Object> row : monthData) {
                    Object val = row.get(defaultDataColumnName);
                    if (val != null) {
                        try {
                            sum += Double.parseDouble(val.toString());
                            count++;
                        } catch (NumberFormatException e) {
                            log.warn("Invalid number format: {}, skipping value", val, e);
                        }
                    }
                }
                if (count > 0) {
                    BigDecimal bd = BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP);
                    value = bd.doubleValue();
                }
            }

            monthlyData.add(MarketDataChartRawData.builder()
                    .date(representativeDate)
                    .value(value)
                    .build());

            currentMonth = currentMonth.plusMonths(1);
        }
        return monthlyData;
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
    private CalculatePeriodChangeRes calculatePeriodChange(
            List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName,
            Set<CalculatePeriodChangeType> calculatePeriodChangeTypes) {
        if (rawData == null || rawData.isEmpty() || calculatePeriodChangeTypes == null || calculatePeriodChangeTypes.isEmpty()) {
            return CalculatePeriodChangeRes.ofEmpty();
        }

        // 오늘
        LocalDate today = LocalDate.now();
        // 어제
        LocalDate yesterday = today.minusDays(1);

        // 이번 주
        LocalDate thisWeekStart = today.minusDays(today.getDayOfWeek().getValue() % 7); // 일요일
        LocalDate thisWeekEnd = today;
        // 지난 주
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        // 이번 달
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd = thisMonthStart.withDayOfMonth(thisMonthStart.lengthOfMonth());
        // 지난 달
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());

        // 이번 연도
        LocalDate thisYearStart = today.withMonth(1).withDayOfMonth(1);
        LocalDate thisYearEnd = thisYearStart.withMonth(12).withDayOfMonth(31);
        // 지난 연도
        LocalDate lastYearStart = thisYearStart.minusYears(1);
        LocalDate lastYearEnd = lastYearStart.withMonth(12).withDayOfMonth(31);

        // 기간별 날짜 범위 정의
        Map<CalculatePeriodChangeType, CalculatePeriodChangeRange> calculatePeriodChangeRanges = Map.of(
                CalculatePeriodChangeType.DOD, new CalculatePeriodChangeRange(
                        today, today, yesterday, yesterday),

                CalculatePeriodChangeType.WOW, new CalculatePeriodChangeRange(
                        thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd),

                CalculatePeriodChangeType.MOM, new CalculatePeriodChangeRange(
                        thisMonthStart, thisMonthEnd, lastMonthStart, lastMonthEnd),

                CalculatePeriodChangeType.YOY, new CalculatePeriodChangeRange(
                        thisYearStart, thisYearEnd, lastYearStart, lastYearEnd));

        // 계산 결과 저장
        Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> calculatePeriodChangeValues = calculatePeriodChangeTypes.stream()
                .filter(calculatePeriodChangeRanges::containsKey)
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> {
                            CalculatePeriodChangeRange range = calculatePeriodChangeRanges.get(type);
                            return calculatePeriodChangeValue(
                                    rawData, dateColumnName, defaultDataColumnName,
                                    range.currentStart(),
                                    range.currentEnd(),
                                    range.previousStart(),
                                    range.previousEnd());
                        }));

        return CalculatePeriodChangeRes.of(calculatePeriodChangeValues.get(CalculatePeriodChangeType.DOD),
                calculatePeriodChangeValues.get(CalculatePeriodChangeType.WOW),
                calculatePeriodChangeValues.get(CalculatePeriodChangeType.MOM),
                calculatePeriodChangeValues.get(CalculatePeriodChangeType.YOY));
    }

    /**
     * 기간 변화 수치, 변화율 계산
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @param currentStartDate 현재 기간 시작일
     * @param currentEndDate 현재 기간 종료일
     * @param previousStartDate 이전 기간 시작일
     * @param previousEndDate 이전 기간 종료일
     * @return 변화 수치, 변화율
     */
    private CalculatePeriodChangeValue calculatePeriodChangeValue(
            List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName,
            LocalDate currentStartDate,
            LocalDate currentEndDate,
            LocalDate previousStartDate,
            LocalDate previousEndDate) {
        double currentSum = 0.0;
        int currentCount = 0;
        double previousSum = 0.0;
        int previousCount = 0;

        for (Map<String, Object> row : rawData) {
            Object value = row.get(defaultDataColumnName);
            if (value == null) {
                continue;
            }

            double val;
            try {
                val = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid number format: {}, skipping", value, e);
                continue;
            }

            LocalDate date = DateUtil.parseDate(row.get(dateColumnName));

            if (!date.isBefore(currentStartDate) && !date.isAfter(currentEndDate)) {
                currentSum += val;
                currentCount++;
            } else if (!date.isBefore(previousStartDate) && !date.isAfter(previousEndDate)) {
                previousSum += val;
                previousCount++;
            }

        }

        // 데이터가 없으면 null 반환
        if (currentCount == 0 || previousCount == 0) {
            return CalculatePeriodChangeValue.ofEmpty();
        }

        double currentAvg = currentSum / currentCount;
        double previousAvg = previousSum / previousCount;

        // 변화율 계산: ((현재 평균 - 이전 평균) / 이전 평균) * 100
        double changeRateRaw = ((currentAvg - previousAvg) / previousAvg) * 100;
        BigDecimal changeRateBd = BigDecimal.valueOf(changeRateRaw).setScale(2, RoundingMode.HALF_UP); // formatting
        double changeRate = changeRateBd.doubleValue();

        // 변화 수치 계산: 현재 평균 - 이전 평균
        double changeValueRaw = currentAvg - previousAvg;
        BigDecimal changeValueBd = BigDecimal.valueOf(changeValueRaw).setScale(2, RoundingMode.HALF_UP); // formatting
        double changeValue = changeValueBd.doubleValue();

        return CalculatePeriodChangeValue.of(changeRate, changeValue);
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
            int userNo,
            Role role,
            int page,
            int size,
            String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        String select = """
                tp.prvd_no           AS providerNo,
                tg.gds_no            AS goodsNo,
                tgd.gds_data_no      AS goodsDataNo,
                tgd.gds_data_nm      AS goodsDataNm,
                tgsrc.gds_data_src_no AS goodsDataSrcNo,
                tgsrc.gds_data_src_nm AS goodsDataSrcNm,
                tgsrc.mod_dtm        AS modifyDt,
                tgsrc.tgt_db_nm      AS targetDbNm,
                tgsrc.tgt_tbl_nm     AS targetTableNm,
                tgsrc.qry_cdtn_cntt  AS queryConditionCntt,
                tgsrc.tbl_colm_list_nm AS tableColumnListNm
                """;

        String where = """
                tc.cust_cpny_no = :companyNo
                AND tc.del_yn   = 'N' AND tc.use_yn   = 'Y'
                AND tg.del_yn   = 'N' AND tg.use_yn   = 'Y'
                AND tp.del_yn   = 'N' AND tp.use_yn   = 'Y'
                AND tgd.del_yn  = 'N'
                AND tgd.gds_data_mark_yn  = 'Y'
                AND tgsrc.del_yn= 'N'
                """;

        if (StringUtils.hasText(keyword)) {
            where += " AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)";
        }

        String contentSql = """
                SELECT %s
                FROM midb.tam_custcpnygdsmappl  tc
                JOIN midb.tmd_gdsm           tg    ON tg.gds_no         = tc.gds_no
                JOIN midb.tmd_prvdm          tp    ON tp.prvd_no        = tg.prvd_no
                JOIN midb.tmd_gdsdatam       tgd   ON tgd.gds_no        = tg.gds_no
                JOIN midb.tmd_gdsdatasrcd    tgsrc ON tgsrc.gds_data_no = tgd.gds_data_no AND tgsrc.gds_data_use_mnagt_cd = 'backend'
                WHERE %s
                ORDER BY tgd.gds_data_nm ASC, tgsrc.gds_data_src_nm ASC
                """.formatted(select, where);

        String countSql = """
                SELECT COUNT(*) AS totalCount
                FROM midb.tam_custcpnygdsmappl  tc
                JOIN midb.tmd_gdsm           tg    ON tg.gds_no         = tc.gds_no
                JOIN midb.tmd_prvdm          tp    ON tp.prvd_no        = tg.prvd_no
                JOIN midb.tmd_gdsdatam       tgd   ON tgd.gds_no        = tg.gds_no
                JOIN midb.tmd_gdsdatasrcd    tgsrc ON tgsrc.gds_data_no = tgd.gds_data_no AND tgsrc.gds_data_use_mnagt_cd = 'backend'
                WHERE %s
                """.formatted(where);

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

            boolean isViewable = isDataViewable(companyNo, userNo, role, goodsNo);

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    boolean isDataViewable(int companyNo, int userNo, Role role, int goodsNo) {

        if (companyNo <= 0 || userNo <= 0 || goodsNo <= 0 || role == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Invalid parameters for data viewability check");
        }

        switch (role) {
            case ADMIN -> {
                PermissionRes permissionRes = miGoodsPermissionService.getAccessAdminGoodsPermissions();
                return permissionRes.getGoodsViewableMap(companyNo).getOrDefault(goodsNo, Boolean.FALSE);
            }

            case USER -> {
                PermissionRes permissionRes = miGoodsPermissionService.getUserGoodsPermissions(userNo);
                return permissionRes.getGoodsViewableMap(companyNo).getOrDefault(goodsNo, Boolean.FALSE);
            }

            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        }
    }

    private record LastValueDto(
            Double value,
            LocalDateTime modDtm) {
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
                    FROM %s.%s
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
