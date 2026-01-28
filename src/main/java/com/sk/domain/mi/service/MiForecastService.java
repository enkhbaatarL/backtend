package com.sk.domain.mi.service;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import com.sk.domain.mi.dto.response.forecast.ForecastChartRes;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.ForecastTargetRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportDetailRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.forecast.MnemonicCode;
import com.sk.domain.shared.mapper.dynamic.ForecastMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private static final String FORECAST_AVAILABLE_DATE_LIMIT = "10";
    private static final String MARKET_DATA_PREDICT_DATE_COLUMN_NAME = "prdc_fulf_dt";
    private static final String MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME = "prdc_crtr_dt";
    private static final String MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME = "prdc_rslt_dt";
    private static final String MARKET_DATA_PREDICT_RESULT_START_DATE_COLUMN_NAME = "min_prdc_rslt_dt";
    private static final String MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME = "prdc_rslt_val";
    private static final String MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME = "prdc_crtr_val";
    private static final String MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME = "mnmnc_cd";
    private static final String MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME = "mrkt_idx_anly_tgt_cd";
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
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(
                    Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", companyNo)));

        // 차트 설정 조회
        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);

        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
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
     * Forecast 차트 기간 조회 (임시)
     *
     * @param customerCompanyNo 고객사 번호
     * @param xAxisUnit x축 단위 (1D, 1W, 1M, 1Q, 1Y)
     * @param period 전체기간 (1W, 1M, 3M, 6M, YTD, 1Y, ALL)
     * @return ForecastChartRes
     */
    public ForecastChartRes getChart(Integer customerCompanyNo, String xAxisUnit, String period) {
        AxisUnit axisUnit = AxisUnit.from(xAxisUnit);
        TotalPeriod totalPeriod = TotalPeriod.from(period);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today;
        LocalDate startDate = calculateStartDate(today, totalPeriod);

        validatePeriodLength(startDate, endDate, axisUnit);

        List<ForecastChartRes.ForecastChartDateItem> dateList = buildDateList(startDate, endDate, axisUnit);

        return ForecastChartRes.builder()
                .xAxisUnit(axisUnit.label)
                .period(totalPeriod.label)
                .startDate(DateUtil.toYmd(startDate))
                .endDate(DateUtil.toYmd(endDate))
                .dateList(dateList)
                .build();
    }

    /**
     * Forecast 리포트 목록 조회
     *
     * @param companyNo 회사 번호
     * @param goodsDataSourceNo 데이터 소스 번호(선택)
     * @return List<ForecastReportRes>
     */
    public List<ForecastReportRes> getForecastReports(int companyNo, Integer goodsDataSourceNo) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(
                    Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        if (goodsDataSourceNo != null) {
            chartPredictSettingDetails = chartPredictSettingDetails.stream()
                    .filter(detail -> detail.getAssociatedGoodsDataSourceDetail() != null)
                    .filter(detail -> goodsDataSourceNo.equals(detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
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
     * Forecast report 상세 조회
     *
     * @param companyNo 회사 번호
     * @param marketIndexAnalysisTargetCd 시장지수분석대상코드
     * @param predictionFulfillDt 예측수행일자
     * @param mnemonicCd MNEMONIC코드
     * @param predictionCriteriaDt 예측기준일자
     * @return ForecastReportDetailRes
     */
    public ForecastReportDetailRes getForecastReportDetail(int companyNo, String marketIndexAnalysisTargetCd,
            String predictionFulfillDt, String mnemonicCd, String predictionCriteriaDt) {
        String currentDate = DateUtil.toYmd(LocalDate.now());
        CustomerCompany customerCompany = customerCompanyRepository.findActiveByCustomerCompanyNo(companyNo, currentDate)
                .orElseThrow(() -> AppErrorException.of(
                    Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패: %s", companyNo)));

        ChartSettingAndDetails chartPredictSettingDetailRes = chartPredictSettingDetailRepository.findByCustomerCompany(customerCompany);
        if (chartPredictSettingDetailRes == null || chartPredictSettingDetailRes.chartPredictSettingDetails().isEmpty()) {
            throw AppErrorException.of(Error.RSC_NOT_FOUND, "설정된 차트가 없습니다.");
        }

        List<ChartPredictSettingDetail> chartPredictSettingDetails = chartPredictSettingDetailRes.chartPredictSettingDetails();
        for (ChartPredictSettingDetail detail : chartPredictSettingDetails) {
            if (!Boolean.TRUE.equals(detail.getIsChartLinePredictVal())) {
                continue;
            }
            GoodsDataSourceDetail goodsDataSourceDetail = detail.getGoodsDataSourceDetail();
            if (goodsDataSourceDetail == null || goodsDataSourceDetail.getQueryConditionCntt() == null) {
                continue;
            }

            DataSourceContextHolder.setDataSourceKey(goodsDataSourceDetail.getTargetDbNm());

            String queryWhere = goodsDataSourceDetail.getQueryConditionCntt();
            List<DashboardChartSummaryMap> chartSummaries =
                    forecastMapper.chartSummary(queryWhere, predictionFulfillDt, null, null);
            if (chartSummaries == null || chartSummaries.isEmpty()) {
                continue;
            }

            DashboardChartSummaryMap matchedSummary = chartSummaries.stream()
                    .filter(summary -> marketIndexAnalysisTargetCd.equals(summary.getMarketIndexAnalysisTargetCd()))
                    .filter(summary -> mnemonicCd.equals(summary.getMnemonicCd()))
                    .findFirst()
                    .orElse(null);
            if (matchedSummary == null) {
                continue;
            }

            DashboardChartSummaryMap decodedSummary = decodeSummaryPath(matchedSummary);
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
            ForecastResultValue resultValue = getForecastResultValue(goodsDataSourceDetail, forecastKey);
            if (resultValue == null) {
                continue;
            }

            DiffResult diffResult = calculateDiffResult(resultValue.predictionCriteriaVal(),
                    resultValue.predictionResultVal());
            Double avgPredictionResultVal = toScaledDouble(resultValue.predictionResultVal());
            Double predictionCriteriaVal = toScaledDouble(resultValue.predictionCriteriaVal());
            String endPredictionResultDt = decodedSummary.getPredictionResultDt();
            if (resultValue.endPredictionResultDt() != null) {
                endPredictionResultDt = resultValue.endPredictionResultDt();
            }

            return ForecastReportDetailRes.builder()
                    .marketIndexAnalysisTargetCd(decodedSummary.getMarketIndexAnalysisTargetCd())
                    .predictionFulfillDt(decodedSummary.getPredictionFulfillDt())
                    .mnemonicCd(decodedSummary.getMnemonicCd())
                    .predictChartDetailExpn(forecastKey.predictChartDetailExpn())
                    .associatedGoodsDataSourceNo(forecastKey.associatedGoodsDataSourceNo())
                    .endPredictionResultDt(endPredictionResultDt)
                    .startPredictionResultDt(resultValue.startPredictionResultDt())
                    .predictionCriteriaDt(resultValue.predictionCriteriaDt())
                    .avgPredictionResultVal(avgPredictionResultVal)
                    .predictionCriteriaVal(predictionCriteriaVal)
                    .predictionResultDiffVal(diffResult.diffValue())
                    .predictionResultDiffRate(diffResult.diffRate())
                    .isScheduled(null)
                    .explanatoryTitleContent(decodedSummary.getExplanatoryTitleContent())
                    .explanatorySummaryContent(decodedSummary.getExplanatorySummaryContent())
                    .explanatoryFilePath(decodedSummary.getExplanatoryFilePath())
                    .build();
        }

        throw AppErrorException.of(Error.RSC_NOT_FOUND, "Forecast report 상세 조회 실패");
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
     * @return List<AvailableForecastKey>
     */
    private List<AvailableForecastKey> getAvailableDates(List<ChartPredictSettingDetail> chartPredictSettingDetails) {
        return chartPredictSettingDetails.stream()
                .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
                .flatMap(detail -> {
                    GoodsDataSourceDetail goodsDataSource = detail.getGoodsDataSourceDetail();
                    if (goodsDataSource == null) {
                        return Stream.empty();
                    }

                    Integer associatedGoodsDataSourceNo = detail.getAssociatedGoodsDataSourceDetail() != null
                            ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                            : null;
                    List<Map<String, Object>> rawData = getDateDataRows(goodsDataSource);
                    return rawData.stream()
                            .map(row -> {
                                Object fulfillDateValue = row.get(MARKET_DATA_PREDICT_DATE_COLUMN_NAME);
                                Object criteriaDateValue = row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME);
                                if (fulfillDateValue == null || criteriaDateValue == null) {
                                    return null;
                                }
                                String fulfillDate = String.valueOf(fulfillDateValue);
                                String criteriaDate = String.valueOf(criteriaDateValue);
                                String mnemonicCd = getStringValue(row.get(MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME));
                                String marketIndexAnalysisTargetCd =
                                        getStringValue(row.get(MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME));
                                String predictChartDetailExpn = null;
                                if (mnemonicCd != null) {
                                    MnemonicCode mnemonicCode = MnemonicCode.findByCode(mnemonicCd);
                                    if (mnemonicCode != null) {
                                        predictChartDetailExpn = mnemonicCode.getPredictChartDetailExpression();
                                    }
                                }
                                return new AvailableForecastKey(fulfillDate, criteriaDate, mnemonicCd, marketIndexAnalysisTargetCd,
                                        predictChartDetailExpn, associatedGoodsDataSourceNo);
                            })
                            .filter(Objects::nonNull);
                })
                .sorted(Comparator
                        .comparing(AvailableForecastKey::predictionFulfillDt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AvailableForecastKey::associatedGoodsDataSourceNo, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Forecast 리포트 데이터 조회
     *
     * @param chartPredictSettingDetails 차트 설정 상세 목록
     * @param forecastKey 조회 날짜 정보
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
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 기준일 컬럼명을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
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
        String dateColumn = String.format("MAX(%s) as %s, MAX(%s) as %s, MAX(%s) as %s, MAX(%s) as %s",
                MARKET_DATA_PREDICT_DATE_COLUMN_NAME, MARKET_DATA_PREDICT_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME, MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME,
                MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME, MARKET_DATA_PREDICT_MNEMONIC_COLUMN_NAME,
                MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME, MARKET_DATA_PREDICT_MARKET_INDEX_TARGET_COLUMN_NAME);
        String groupBy = "SUBSTRING(%s, %d, %d)".formatted(MARKET_DATA_PREDICT_DATE_COLUMN_NAME, startDateLength, endDateLength);
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

    private ForecastResultValue getForecastResultValue(GoodsDataSourceDetail goodsDataSourceDetail, AvailableForecastKey forecastKey) {
        if (goodsDataSourceDetail == null || forecastKey == null) {
            return null;
        }
        if (forecastKey.predictionFulfillDt() == null || forecastKey.predictionCriteriaDt() == null) {
            return null;
        }

        String whereClause = buildForecastResultWhereClause(goodsDataSourceDetail.getQueryConditionCntt(),
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
                        .orderByColumns(String.format("%s ASC", MARKET_DATA_PREDICT_RESULT_DATE_COLUMN_NAME))
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
            String resultVal = getStringValue(row.get(MARKET_DATA_PREDICT_RESULT_VALUE_COLUMN_NAME));
            String criteriaDt = getStringValue(row.get(MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME));
            String criteriaVal = getStringValue(row.get(MARKET_DATA_PREDICT_CRITERIA_VALUE_COLUMN_NAME));

            if (resultDt != null) {
                if (startPredictionResultDt == null) {
                    startPredictionResultDt = resultDt;
                }
                endPredictionResultDt = resultDt;
            }

            if (criteriaDt != null) {
                if (predictionCriteriaDt == null || criteriaDt.compareTo(predictionCriteriaDt) > 0) {
                    predictionCriteriaDt = criteriaDt;
                }
            }

            if (criteriaVal != null) {
                try {
                    BigDecimal criteria = new BigDecimal(criteriaVal);
                    if (predictionCriteriaVal == null || criteria.compareTo(predictionCriteriaVal) > 0) {
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

    private String buildForecastResultWhereClause(String baseWhereClause, String fulfillDate, String criteriaDate) {
        List<String> clauses = new ArrayList<>();
        if (baseWhereClause != null && !baseWhereClause.isBlank()) {
            clauses.add(baseWhereClause);
        }
        clauses.add(String.format("%s = '%s'", MARKET_DATA_PREDICT_DATE_COLUMN_NAME, fulfillDate));
        clauses.add(String.format("%s = '%s'", MARKET_DATA_PREDICT_CRITERIA_DATE_COLUMN_NAME, criteriaDate));
        return String.join(" AND ", clauses);
    }

    private String getStringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private LocalDate calculateStartDate(LocalDate today, TotalPeriod totalPeriod) {
        return switch (totalPeriod) {
            case ONE_WEEK -> today.minusWeeks(1).plusDays(1);
            case ONE_MONTH -> today.minusMonths(1).plusDays(1);
            case THREE_MONTHS -> today.minusMonths(3).plusDays(1);
            case SIX_MONTHS -> today.minusMonths(6).plusDays(1);
            case YEAR_TO_DATE -> LocalDate.of(today.getYear(), 1, 1);
            case ONE_YEAR -> today.minusYears(1).plusDays(1);
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

    private List<ForecastChartRes.ForecastChartDateItem> buildDateList(LocalDate startDate, LocalDate endDate,
            AxisUnit axisUnit) {
        List<ForecastChartRes.ForecastChartDateItem> dates = new ArrayList<>();
        if (axisUnit == AxisUnit.ONE_DAY) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                dates.add(ForecastChartRes.ForecastChartDateItem.builder()
                        .label(DateUtil.toYmd(cursor))
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
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM ''yy");
                yield formatter.format(date);
            }
            case ONE_QUARTER -> {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                int year = date.getYear() % 100;
                yield String.format("Q%d '%02d", quarter, year);
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

    private record AvailableForecastKey(String predictionFulfillDt, String predictionCriteriaDt, String mnemonicCd,
            String marketIndexAnalysisTargetCd, String predictChartDetailExpn, Integer associatedGoodsDataSourceNo) {
    }

    private record DiffResult(Double diffValue, Double diffRate) {
        private static DiffResult empty() {
            return new DiffResult(null, null);
        }
    }

    private record ForecastResultValue(String endPredictionResultDt, String startPredictionResultDt,
            String predictionCriteriaDt, String predictionResultVal, String predictionCriteriaVal,
            List<ForecastResultPoint> predictionResults) {
    }

    private record ForecastResultPoint(String predictionResultDt, String predictionResultVal) {
    }
}
