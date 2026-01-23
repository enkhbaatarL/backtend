package com.sk.domain.mi.service;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.ForecastTargetRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastSummaryRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.mapper.dynamic.ForecastMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
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
    private final ForecastMapper forecastMapper;

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

        // 차트선예측값여부 chrt_line_prdc_val_yn = N, 선물여부 futr_yn = N
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
                .filter(Objects::nonNull)
                .toList();

        LocalDate lastUpdateDt = lastUpdatedList.stream()
                .max(LocalDate::compareTo)
                .orElse(null);

        return ForecastInfoRes.of(chartPredictSetting, targetList, lastUpdateDt);
    }

    /**
     * Forecast 요약 목록 조회
     *
     * @param companyNo 회사 번호
     * @param goodsDataSourceNo 데이터 소스 번호(선택)
     * @return List<ForecastSummaryRes>
     */
    public List<ForecastSummaryRes> getForecastSummaries(int companyNo, Integer goodsDataSourceNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                    .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                    .filter(detail -> goodsDataSourceNo.equals(detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
                    .toList();
        }
        List<String> availableDates = getAvailableDates(chartPredictSettingDetails);

        List<ForecastSummaryRes> results = new ArrayList<>();
        for (String date : availableDates) {
            results.addAll(getForecastSummariesByDate(chartPredictSettingDetails, date));
        }

        return results;
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
     * 조회 가능한 날짜 목록 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @return List<String>
     */
    private List<String> getAvailableDates(List<ChartPredictSettingDetail> chartPredictSettingDetails) {
        return chartPredictSettingDetails.stream()
                .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
                .flatMap(detail -> {
                    GoodsDataSourceDetail goodsDataSource = detail.getGoodsDataSourceDetail();
                    if (goodsDataSource == null) {
                        return Stream.empty();
                    }

                    List<Map<String, Object>> rawData = getDateDataRows(goodsDataSource, MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                    return rawData.stream()
                            .map(row -> row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME))
                            .filter(Objects::nonNull)
                            .map(String::valueOf);
                })
                .toList();
    }

    /**
     * Forecast 요약 데이터 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param date 조회 날짜
     * @return List<ForecastSummaryRes>
     */
    private List<ForecastSummaryRes> getForecastSummariesByDate(List<ChartPredictSettingDetail> chartPredictSettingDetails, String date) {
        return chartPredictSettingDetails.stream()
                .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
                .flatMap(detail -> {
                    GoodsDataSourceDetail goodsDataSourceDetail = detail.getGoodsDataSourceDetail();
                    if (goodsDataSourceDetail == null || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                        return Stream.empty();
                    }

                    DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

                    String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
                    List<DashboardChartSummaryMap> summaries = forecastMapper.chartSummary(queryWhere, date, null, null);

                    Stream<DashboardChartSummaryMap> decodedSummaries = summaries != null ? summaries.stream().map(summary -> {
                        if (summary.getExplanatoryFilePath() == null) {
                            return summary;
                        }
                        try {
                            String decodedPath = URLDecoder.decode(summary.getExplanatoryFilePath(), StandardCharsets.UTF_8);
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
                    }) : Stream.empty();

                    return decodedSummaries.map(summary -> ForecastSummaryRes.of(date, summary));
                })
                .toList();
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

        int startDateLength = 1;
        int endDateLength = 6;
        String dateColumn = String.format("MAX(%s) as %s", dateColumnName, dateColumnName);
        String groupBy = "SUBSTRING(%s, %d, %d)".formatted(dateColumnName, startDateLength, endDateLength);
        String orderBy = String.format("%s DESC", groupBy);

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofGroupByOrderBy(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), dateColumn, goodsDataSourceDetail.getQueryConditionCntt(), groupBy, orderBy));
    }
}
