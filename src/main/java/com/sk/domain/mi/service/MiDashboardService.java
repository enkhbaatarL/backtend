package com.sk.domain.mi.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.DashboardInfoRes;
import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

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

    private final int DASHBOARD_CHART_START_DATE_FROM_MONTH = 11; // 차트 데이터 조회 기준 날짜 (N달 전)
    private static final int MARKET_DATA_DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final int MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX = 1; // 그래프에 그려줄 기본 데이터 컬럼 인덱스

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
     * 대시보드 차트 데이터 조회
     * 
     * @param companyNo
     * @return
     */
    public DashboardChartRes getChart(int companyNo, String date) {
        if (date == null || date.isEmpty()) {
            date = DateUtil.toYearMonth(LocalDate.now());
        }

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> new EntityNotFoundException("CustomerCompany not found"));

        // TODO: 잘 가져와지는지 확인 => 안돼
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            // TODO: null 처리
            return null;
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        // 차트 데이터 조회
        // TODO: parallelStream 으로 변경 필요
        for (ChartPredictSettingDetail chartPredictSettingDetail : chartPredictSettingDetails) {
            // TODO: goodsDataSource 잘 가져와지는지 확(N+1 없이)
            GoodsDataSourceDetail goodsDataSource = chartPredictSettingDetail.getGoodsDataSourceDetail();

            // 칼럼 목록 조회
            String[] tableColumnListNm = Arrays.stream(goodsDataSource.getTableColumnListNm().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            // 날짜 칼럼
            String dateColumnName = tableColumnListNm[MARKET_DATA_DATE_COLUMN_INDEX];
            // 기본 데이터 칼럼
            String defaultDataColumnName = tableColumnListNm[MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

            // Lasted Date 기준 (2025-01-01)
            LocalDate endDate = DateUtil.fromYearMonth(date).plusMonths(chartPredictSetting.getPredictPeriodMonthsCnt());
            LocalDate startDate = endDate.minusMonths(DASHBOARD_CHART_START_DATE_FROM_MONTH);
            
            // 예측 데이터와 정형데이터의 date 포맷이 다름
            // 차트 조회 종료 날짜(2025-01-01)
            String END_DATE = chartPredictSettingDetail.getIsChartLinePredictValue() ? DateUtil.toYmd(endDate) : DateUtil.toYmdHyphen(endDate);
            // 차트 조회 시작 날짜(2025-01-01)
            String START_DATE = chartPredictSettingDetail.getIsChartLinePredictValue() ? DateUtil.toYmd(startDate) : DateUtil.toYmdHyphen(startDate);

            // 원시 데이터 조회
            List<Map<String, Object>> rawData = getMarketDataRows(goodsDataSource, dateColumnName, START_DATE, END_DATE);

            if (rawData.isEmpty()) {
                // TODO: null 처리
                return null;
            }

            // 마지막 데이터 값
            Double lastValue = Double.parseDouble(rawData.get(rawData.size() - 1).get(defaultDataColumnName).toString());

            log.info("lastValue: {}", lastValue);

            // associatedGoodsDataSourceDetail 기준으로 묶어야 함
        }
        return null;
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
    private List<Map<String, Object>> getMarketDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate,
            String endDate) {
        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        String whereClause = "%s >= '%s' AND %s <= '%s'".formatted(dateColumnName, startDate, dateColumnName, endDate);

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause, dateColumnName));
    }
}
