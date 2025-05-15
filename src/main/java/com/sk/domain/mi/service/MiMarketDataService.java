package com.sk.domain.mi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.marketData.ChangeRatesRes;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.MarketDataGraphRawData;
import com.sk.domain.mi.dto.response.marketData.MarketDataGraphRes;
import com.sk.domain.mi.dto.response.marketData.MarketMetaData;
import com.sk.domain.mi.dto.response.marketData.MiMarketDataRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.MarketDataChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiMarketDataService {

    private static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스
    private static final int MARKET_DATA_START_DATE = 2; // 마켓데이터 원시 데이터 조회 기준 날짜 (N년 전)
    private static final int MARKET_DATA_MONTHLY_START_DATE = 1; // 마켓데이터 월별 데이터 시작 날짜 (N년 전)
    private static final String MARKET_DATA_INDEX_START_DATE_LIMIT = "1"; // 원시데이터 시작 날짜 계산을 위한 LIMIT 조건
    private static final String MARKET_DATA_INTERVAL = "1M"; // 월별 평균 계산을 위한 기간

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final MiGoodsDataInquiryUserService miGoodsDataInquiryUserService;
    private final MarketDataChartSettingRepository marketDataChartSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final MiGoodsPermissionService miGoodsPermissionService;

    public List<MiMarketDataRes> getMarketDatas(Integer customerCompanyNo, Authentication authentication) {
        String START_DATE = DateUtil.toYmdHyphen(LocalDate.now().minusYears(MARKET_DATA_START_DATE)); // 2년 전

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalseAndIsUsedTrue(customerCompanyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", customerCompanyNo)));

        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "사용자 권한 조회 실패");
        }
        Role role =  Role.valueOf(jwtToken.getToken().getClaimAsString(Claim.ROLE.getCode()));

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

        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

        List<MiMarketDataRes> miMarketDataResponses = new ArrayList<>();
        for (MarketDataChartSetting marketDataChartSetting : marketDataChartSettings) {
            GoodsDataSourceDetail mainGoodsDataSource = marketDataChartSetting.getGoodsDataSourceDetail();
            GoodsData mainGoodsData = marketDataChartSetting.getGoodsDataSourceDetail().getGoodsData();
            Goods mainGoods = mainGoodsData.getGoods();

            // 권한 조회
            Boolean isPermitted = permissionRes.getGoodsViewableMap().get(mainGoods.getGoodsNo());
            if (!isPermitted) {
                miMarketDataResponses.add(MiMarketDataRes.of(isPermitted, MarketMetaData.of(marketDataChartSetting)));
                continue;
            }

            // 칼럼 목록 조회
            String[] tableColumnListNm = mainGoodsDataSource.getTableColumnListNm().split(",");
            // 날짜 칼럼
            String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
            // 기본 데이터 칼럼
            String defaultDataColumnName = tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            // 원시 데이터 조회
            List<Map<String, Object>> rawData = getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE);

            if (rawData.isEmpty()) {
                isPermitted = false;
                miMarketDataResponses.add(MiMarketDataRes.of(isPermitted, MarketMetaData.of(marketDataChartSetting)));
                continue;
            }

            // 월별 데이터로 변환
            List<MarketDataGraphRawData> monthlyData = convertToMonthlyData(rawData, dateColumnName, defaultDataColumnName,
                    LocalDate.now().minusYears(MARKET_DATA_MONTHLY_START_DATE));

            MarketDataGraphRes mainGraph = MarketDataGraphRes.of(monthlyData);

            // YOY, MOM, WOW 계산
            ChangeRatesRes changeRates = calculateChangeRates(rawData, dateColumnName, defaultDataColumnName);

            // 마지막 데이터 값
            LocalDate lastUpdateDt = DateUtil.fromIsoToLocalDate(rawData.get(rawData.size() - 1).get(dateColumnName).toString());

            // 마지막 데이터 값
            Double lastValue = Double.parseDouble(rawData.get(rawData.size() - 1).get(defaultDataColumnName).toString());
            BigDecimal bd = BigDecimal.valueOf(lastValue).setScale(2, RoundingMode.HALF_UP);
            lastValue = bd.doubleValue();

            // Index Start date 데이터 조회
            LocalDate indexStartDate;
            List<Map<String, Object>> indexStartDateData = getIndexStartDateData(mainGoodsDataSource, dateColumnName);
            if (!indexStartDateData.isEmpty()) {
                String rawDate = indexStartDateData.get(0).get(dateColumnName).toString();
                indexStartDate = DateUtil.fromIsoToLocalDate(rawDate);
            } else {
                indexStartDate = LocalDate.now().minusYears(MARKET_DATA_START_DATE);
            }

            // 비교군 데이터소스 조회
            List<DataSourceGroupMapping> dataSourceGroupMappings = dataSourceGroupMappingRepository
                    .findByCompareGoodsDataSourceDetailAndIsDeletedFalse(mainGoodsDataSource);

            // 비교군 데이터소스 조회
            List<MarketDataGraphRes> compareGraphs = new ArrayList<>();
            if (dataSourceGroupMappings != null && !dataSourceGroupMappings.isEmpty()) {
                for (DataSourceGroupMapping dataSourceGroupMapping : dataSourceGroupMappings) {
                    GoodsDataSourceDetail compareGoodsDataSource = dataSourceGroupMapping.getCompareGoodsDataSourceDetail();

                    String[] compareTableColumnListNm = compareGoodsDataSource.getTableColumnListNm().split(",");
                    // 날짜 칼럼
                    String compareDateColumnName = compareTableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
                    // 기본 데이터 칼럼
                    String compareDefaultDataColumnName = compareTableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

                    // 비교군 원시 데이터 조회
                    List<Map<String, Object>> compareRawData = getMarketDataRows(compareGoodsDataSource, compareDateColumnName, START_DATE);
                    // 비교군 월별 데이터로 변환
                    List<MarketDataGraphRawData> compareGraph = convertToMonthlyData(compareRawData, compareDateColumnName,
                            compareDefaultDataColumnName, LocalDate.now().minusYears(MARKET_DATA_MONTHLY_START_DATE));
                    compareGraphs.add(MarketDataGraphRes.of(compareGraph));
                }
            }

            MarketMetaData marketMetaData = MarketMetaData.of(marketDataChartSetting, mainGoodsData, mainGoodsDataSource, changeRates, MARKET_DATA_INTERVAL, indexStartDate,
                    lastValue, lastUpdateDt);

            miMarketDataResponses.add(MiMarketDataRes.of(true, marketMetaData, mainGraph, compareGraphs));
        }
        return miMarketDataResponses;
    }

    // Class 분리 시킬지 고민중
    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일, 종료일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @param startDate             데이터 범위 시작일 (포함, YYYYMMDD 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional
    private List<Map<String, Object>> getMarketDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate) {

        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null) {
            return Collections.emptyList();
        }

        String whereClause = "%s >= '%s'".formatted(dateColumnName, startDate);

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
    private List<MarketDataGraphRawData> convertToMonthlyData(List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName,
            LocalDate startDate) {
        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        // startDate를 1년 전 다음 달 첫 번째 날로 조정 (예: 2024-06-01)
        LocalDate adjustedStartDate = startDate.plusMonths(1).withDayOfMonth(1);
        // endDate를 이번 달 첫 날로 설정 (예: 2025-05-01)
        LocalDate endDate = LocalDate.now().withDayOfMonth(1);
        // 오늘의 일자 (예: 13)
        int todayDayOfMonth = LocalDate.now().getDayOfMonth();

        // 월별로 데이터 그룹화
        Map<String, List<Map<String, Object>>> monthlyGroupedData = new TreeMap<>();
        for (Map<String, Object> row : rawData) {
            String dateStr = row.get(dateColumnName).toString(); // 예: 2023-01-01T00:00
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateUtil.ISO_LOCAL_DATE_TIME_MINUTES);
                LocalDate date = dateTime.toLocalDate();

                // 2024-06-01 ~ 2025-05-31 범위 내 데이터만 처리
                if (date.isBefore(adjustedStartDate) || date.isAfter(endDate.withDayOfMonth(endDate.lengthOfMonth()))) {
                    continue;
                }

                String yearMonth = DateUtil.toYearMonth(dateTime); // YYYYMM (예: 202406)
                monthlyGroupedData.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(row);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format: {}, skipping row", dateStr, e);
                continue;
            }
        }

        // 최대 12개월 선택 (2024-06 ~ 2025-05)
        List<String> recentMonths = monthlyGroupedData.keySet().stream()
                .sorted() // 오름차순 (과거 -> 최신)
                .limit(12) // 최대 12개월 제한
                .collect(Collectors.toList());

        // 월별 평균 계산 및 DTO 생성
        List<MarketDataGraphRawData> monthlyData = new ArrayList<>();
        for (String yearMonth : recentMonths) {
            List<Map<String, Object>> monthData = monthlyGroupedData.get(yearMonth);

            // 평균 계산
            double sum = 0.0;
            int count = 0;
            for (Map<String, Object> row : monthData) {
                Object value = row.get(defaultDataColumnName);
                if (value != null) {
                    try {
                        sum += Double.parseDouble(value.toString());
                        count++;
                    } catch (NumberFormatException e) {
                        log.warn("Invalid number format: {}, skipping value", value, e);
                    }
                }
            }
            double average = count > 0 ? sum / count : 0.0;
            BigDecimal bd = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);

            // 대표 날짜 선택: 오늘의 일자와 같은 날짜, 없으면 이전 날짜로 fallback
            String representativeDate = selectRepresentativeDate(monthData, dateColumnName, yearMonth, todayDayOfMonth);

            monthlyData.add(MarketDataGraphRawData.builder()
                    .date(representativeDate)
                    .value(bd.doubleValue())
                    .build());
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
            String dateStr = row.get(dateColumnName).toString();
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateUtil.ISO_LOCAL_DATE_TIME_MINUTES);
                LocalDate date = dateTime.toLocalDate();
                dateDataMap.put(date, row);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format in representative date selection: {}, skipping", dateStr, e);
            }
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
     * 기간 변화율 계산
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @return 기간 변화율 계산 결과
     */
    private ChangeRatesRes calculateChangeRates(
            List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName) {
        if (rawData == null || rawData.isEmpty()) {
            return ChangeRatesRes.of(null, null, null);
        }

        LocalDate today = LocalDate.now(); // 2025-05-13
        // 이번 주
        LocalDate thisWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1); // 월요일
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

        // WOW
        Double wowChangeRate = calculatePeriodChangeRate(
                rawData, dateColumnName, defaultDataColumnName,
                thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd);

        // MOM
        Double momChangeRate = calculatePeriodChangeRate(
                rawData, dateColumnName, defaultDataColumnName,
                thisMonthStart, thisMonthEnd, lastMonthStart, lastMonthEnd);

        // YOY
        Double yoyChangeRate = calculatePeriodChangeRate(
                rawData, dateColumnName, defaultDataColumnName,
                thisYearStart, thisYearEnd, lastYearStart, lastYearEnd);

        return ChangeRatesRes.of(wowChangeRate, momChangeRate, yoyChangeRate);
    }

    /**
     * 기간 변화율 계산
     *
     * @param rawData 원시 데이터
     * @param dateColumnName 날짜 컬럼명
     * @param defaultDataColumnName 기본 데이터 컬럼명
     * @param currentStartDate 현재 기간 시작일
     * @param currentEndDate 현재 기간 종료일
     * @param previousStartDate 이전 기간 시작일
     * @param previousEndDate 이전 기간 종료일
     * @return 변화율 (%)
     */
    private Double calculatePeriodChangeRate(
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
            String dateStr = row.get(dateColumnName).toString();
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

            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateUtil.ISO_LOCAL_DATE_TIME_MINUTES);
                LocalDate date = dateTime.toLocalDate();
                if (!date.isBefore(currentStartDate) && !date.isAfter(currentEndDate)) {
                    currentSum += val;
                    currentCount++;
                } else if (!date.isBefore(previousStartDate) && !date.isAfter(previousEndDate)) {
                    previousSum += val;
                    previousCount++;
                }
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format in change rate calculation: {}, skipping", dateStr, e);
                continue;
            }
        }

        // 데이터가 없으면 null 반환
        if (currentCount == 0 || previousCount == 0) {
            return null;
        }

        double currentAvg = currentSum / currentCount;
        double previousAvg = previousSum / previousCount;

        // 변화율 계산: ((현재 평균 - 이전 평균) / 이전 평균) * 100
        double changeRate = ((currentAvg - previousAvg) / previousAvg) * 100;
        BigDecimal bd = BigDecimal.valueOf(changeRate).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
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
    public PaginationRes<DataCatalogRes> getDataCatalog(
            int companyNo,
            int page,
            int size,
            String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        StringBuilder select = new StringBuilder()
                .append(" tp.prvd_no AS providerNo,")
                .append(" tp.prvd_nm AS providerNm,")
                .append(" tg.gds_no AS goodsNo,")
                .append(" tg.gds_nm AS goodsNm,")
                .append(" tgd.gds_data_no AS goodsDataNo,")
                .append(" tgd.gds_data_nm AS goodsDataNm,")
                .append(" tgd.gds_data_expn AS goodsDataExpn,")
                .append(" tgd.mod_dtm AS modifyDt");

        StringBuilder where = new StringBuilder()
                .append(" tc.cust_cpny_no = :companyNo")
                .append(" AND tc.del_yn = 'N'")
                .append(" AND tc.use_yn = 'Y'")
                .append(" AND tg.del_yn = 'N'")
                .append(" AND tg.use_yn = 'Y'")
                .append(" AND tp.del_yn = 'N'")
                .append(" AND tp.use_yn = 'Y'")
                .append(" AND tgd.del_yn = 'N'");

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)");
        }

        String groupBy = " GROUP BY tg.prvd_no, tc.gds_no, tgd.gds_data_no ";

        String orderBy = " ORDER BY tgd.mod_dtm DESC ";

        select.append(" ");
        where.append(" ");

        String contentSql = """
                SELECT
                  """ + select + """
                FROM midb.tam_custcpnygdsmappl tc
                LEFT JOIN midb.tmd_gdsm tg ON tg.gds_no = tc.gds_no
                LEFT JOIN midb.tmd_prvdm tp on tp.prvd_no = tg.prvd_no
                LEFT JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tg.gds_no
                WHERE
                """ + where + """
                """ + groupBy + """
                """ + orderBy;

        String countSql = """
                SELECT COUNT(DISTINCT tgd.gds_data_no) AS totalCount
                FROM midb.tam_custcpnygdsmappl tc
                LEFT JOIN midb.tmd_gdsm tg ON tg.gds_no = tc.gds_no
                LEFT JOIN midb.tmd_prvdm tp on tp.prvd_no = tg.prvd_no
                LEFT JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tg.gds_no
                WHERE
                """ + where;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        Query countQuery = entityManager.createNativeQuery(countSql);

        contentQuery.setParameter("companyNo", companyNo);
        countQuery.setParameter("companyNo", companyNo);

        if (keyword != null && !keyword.isBlank()) {
            contentQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
            countQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
        }

        contentQuery.setFirstResult((page - 1) * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<DataCatalogRes> content = rows.stream()
                .map(row -> new DataCatalogRes(
                        (Integer) row[0], // providerNo
                        (String) row[1], // providerNm
                        (Integer) row[2], // goodsNo
                        (String) row[3], // goodsNm
                        (Integer) row[4], // goodsDataNo
                        (String) row[5], // goodsDataNm
                        (String) row[6], // goodsDataExpn
                        ((Timestamp) row[7]).toLocalDateTime() // modifyDt
                ))
                .collect(Collectors.toList());
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return PaginationRes.toRes(PageableExecutionUtils.getPage(content, pageable, () -> total));
    }

    /**
     * Market Data - Recent Viewed 최신 5개 조회
     *
     * @param userNo 사용자 번호
     * @return List<RecentViewedRes>
     */
    public List<RecentViewedRes> getRecentViewed(int userNo) {
        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));

        return miGoodsDataInquiryUserService.getGoodsDataInquiryUser(user.getUserLoginId(), PageRequest.of(0, 5));
    }
}
