package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.ForecastTargetRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MiForecastService {
    private static final String DASHBOARD_CHART_LAST_UPDATE_DATE_LIMIT = "1";
    private static final String MARKET_DATA_PREDICT_DATE_COLUMN_NAME = "prdc_fulf_dt";
    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    /**
     * 데이터 존재 여부 조회
     *
     * @param companyNo 회사 번호
     * @return boolean
     */
    public boolean getDataExists(int companyNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

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
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        ChartPredictSetting chartPredictSetting = chartPredictSettingDetailRes.chartPredictSetting();

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();

        List<ForecastTargetRes> targetList = chartPredictSettingDetails.stream()
                .filter(detail -> Boolean.FALSE.equals(detail.getIsChartLinePredictVal()))
                .filter(detail -> Boolean.FALSE.equals(detail.getIsFuture()))
                .map(ForecastTargetRes::of)
                .toList();

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
                .filter(date -> date != null)
                .toList();

        LocalDate lastUpdateDt = lastUpdatedList.stream()
                .max(LocalDate::compareTo)
                .orElse(null);

        return ForecastInfoRes.of(chartPredictSetting, targetList, lastUpdateDt);
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
}
