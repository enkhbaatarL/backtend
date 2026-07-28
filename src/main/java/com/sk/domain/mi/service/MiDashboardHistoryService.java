package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.TableRes;
import com.sk.common.utils.CombineUtil;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelUtils;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.context.ForecastContext;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryDataRes;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryMetadataRes;
import com.sk.domain.mi.dto.response.dashboard.history.PriceEntry;
import com.sk.domain.mi.dto.response.dashboard.history.PriceStat;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardSummary;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PriceType;
import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiDashboardHistoryService {

    private final AuthorizationService authorizationService;
    private final MybatisDynamicQueryService queryService;
    private final ChartPredictSettingRepository settingRepo;
    private final ChartPredictSettingDetailRepository settingDetailRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;
    private final MiDashboardService miDashboardService;

    /**
     * 예측가 히스토리 메타데이터 조회
     *
     * @return HistoryMetadataRes
     */
    public HistoryMetadataRes getForecastHistoryMetadata() {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();

        ChartPredictSetting historySetting = settingRepo.findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [chart-predict-setting]. companyNo: %s".formatted(companyNo)));

        List<ChartPredictSettingDetail> historySettingDetails = settingDetailRepository.findByChartPredictSettingAndIsDeletedFalse(historySetting);

        Map<Boolean, List<ChartPredictSettingDetail>> partitioned = historySettingDetails.stream()
            .collect(Collectors.partitioningBy(ChartPredictSettingDetail::getIsChartLinePredictVal));

        // 예측가
        // TODO 수정 필요
        List<ChartPredictSettingDetail> forecasts = partitioned.get(Boolean.TRUE)
            .stream().filter(d->{
                return !d.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo().equals(d.getGoodsDataSourceDetail().getGoodsDataSourceNo());
            }).toList();

        // 실제가 또는 선물가
        List<ChartPredictSettingDetail> actualOrFutures = partitioned.get(Boolean.FALSE);

        boolean downloadable = false;
        if (!actualOrFutures.isEmpty()) {
            Integer goodsNo = actualOrFutures.get(0).getGoodsDataSourceDetail().getGoodsData().getGoods().getGoodsNo();
            downloadable = authorizationService.hasGoodsDownloadPermission(goodsNo);
        }

        // 예측가 데이터가 단일 객체인 경우
        if (forecasts.size() == 1) {

            ChartPredictSettingDetail forecast = forecasts.get(0);

            GoodsDataSourceDetail gds = forecast.getGoodsDataSourceDetail();
            List<TargetPredictResultData> targetPredictResultData = getPredictResultGroupedData(gds);

            List<String> fulfillDates = targetPredictResultData.stream().map(TargetPredictResultData::getPredictFulfillDate).sorted(Comparator.reverseOrder()).toList();

            List<ChartPredictSettingDetail> actual = actualOrFutures.stream()
                .filter(awf -> !awf.getIsFuture())
                .toList();

            List<ChartPredictSettingDetail> futures = actualOrFutures.stream()
                .filter(ChartPredictSettingDetail::getIsFuture)
                .toList();

            return HistoryMetadataRes.of(historySetting, List.of(ForecastContext.of(forecast, fulfillDates, actual, futures)), downloadable);

        } else if (forecasts.size() >= 2) {
            // 예측가 데이터가 복수 객체인 경우

            Map<Integer, List<ChartPredictSettingDetail>> settingDetailMap = groupByForecastDetailNo(historySettingDetails);

            List<ForecastContext> forecastContexts = forecasts.stream().map(f -> {

                GoodsDataSourceDetail gds = f.getGoodsDataSourceDetail();
                List<TargetPredictResultData> targetPredictResultData = getPredictResultGroupedData(gds);

                List<String> fulfillDates = targetPredictResultData.stream().map(TargetPredictResultData::getPredictFulfillDate).sorted(Comparator.reverseOrder()).toList();
                List<ChartPredictSettingDetail> grouped = settingDetailMap.getOrDefault(f.getChartPredictSettingDetailNo(), Collections.emptyList());

                List<ChartPredictSettingDetail> actual = grouped.stream()
                    .filter(awf -> !awf.getIsChartLinePredictVal())
                    .filter(awf -> !awf.getIsFuture())
                    .toList();
                List<ChartPredictSettingDetail> futures = grouped.stream()
                    .filter(awf -> !awf.getIsChartLinePredictVal())
                    .filter(ChartPredictSettingDetail::getIsFuture)
                    .toList();

                return ForecastContext.of(f, fulfillDates, actual, futures);

            }).toList();

            return HistoryMetadataRes.of(historySetting, forecastContexts, downloadable);

        } else {

            return HistoryMetadataRes.of(historySetting, downloadable);

        }

    }

    /**
     * 상세 데이터 그룹핑
     *
     * @param settingDetails 상세 데이터 리스트
     * @return Map<Integer, List<ChartPredictSettingDetail>> 상세 데이터 그룹핑 결과
     */
    private Map<Integer, List<ChartPredictSettingDetail>> groupByForecastDetailNo(List<ChartPredictSettingDetail> settingDetails) {

        // 1. 연관상품데이터소스번호 기준으로 1차 그룹핑
        Map<Integer, List<ChartPredictSettingDetail>> groupedByAssociateNo = settingDetails.stream()
            .collect(Collectors.groupingBy(d -> d.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()));

        // 2. 각 그룹에서 예측가를 찾아 예측가의 상세번호를 키로 사용하고, 해당 그룹의 리스트를 값으로 하는 Map 생성
        return groupedByAssociateNo.values()
            .stream()
            .collect(Collectors.toMap(
                list -> list.stream()
                    .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
                    .findFirst() // 그룹마다 예측가 데이터를 찾아
                    .map(ChartPredictSettingDetail::getChartPredictSettingDetailNo)
                    .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "At least one forecast-data is required. associate-goods-data-source-no: %s".formatted(list.get(0).getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))), // 예측가가 없는 경우 예외 발생
                Function.identity() // 그룹의 리스트를 값으로 사용
            ));
    }

    /**
     * counterpart 구분을 포함한 ForecastContext 빌더
     */
    private ForecastContext toForecastContext(ChartPredictSettingDetail forecast, List<ChartPredictSettingDetail> spotsWithFutures) {
        GoodsDataSourceDetail gds = forecast.getGoodsDataSourceDetail();

        List<TargetPredictResultData> groupedResults = getPredictResultGroupedData(gds);
        List<String> fulfillDates = groupedResults.stream()
            .map(TargetPredictResultData::getPredictFulfillDate)
            .toList();

        var counterparts = spotsWithFutures == null ? List.<ChartPredictSettingDetail>of() : spotsWithFutures;

        List<ChartPredictSettingDetail> spots = counterparts.stream()
            .filter(c -> !c.getIsChartLinePredictVal() && !c.getIsFuture())
            .toList();
        List<ChartPredictSettingDetail> futures = counterparts.stream()
            .filter(c -> !c.getIsChartLinePredictVal() && c.getIsFuture())
            .toList();

        return ForecastContext.of(forecast, fulfillDates, spots, futures);
    }

    /**
     * 히스토리 데이터 조회
     *
     * @param detailNo      설정 상세 번호
     * @param fulfillDate   예측 수행일자
     * @return List<HistoryDataRes> 히스토리 데이터 리스트
     */
    public List<HistoryDataRes> getHistoryData(Integer detailNo, LocalDate fulfillDate) {

        ChartPredictSettingDetail counterpart = settingDetailRepository
            .findByChartPredictSettingDetailNoAndIsDeletedFalse(detailNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [chart-predict-setting-detail]. settingDetailNo: %s".formatted(detailNo)));

        ChartPredictSetting setting = counterpart.getChartPredictSetting();
        List<ChartPredictSettingDetail> details = settingDetailRepository.findByChartPredictSettingAndIsDeletedFalse(setting);

        List<ChartPredictSettingDetail> forecasts = details.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .toList();

        if (forecasts.isEmpty()) {
            return HistoryDataRes.ofEmpty();
        }

        List<HistoryDataRes> result = null;

        final List<ChartPredictSettingDetail> settingDetailForCheckDownload;
        //TODO 변경 필요
        if(setting.getCustomerCompany().getCustomerCompanyNo().equals(2013)){
            result = buildHistoryData(forecasts.get(0), forecasts.get(1), counterpart, fulfillDate);
            settingDetailForCheckDownload = forecasts;
        }else{
            ChartPredictSettingDetail forecast = selectForecast(forecasts, counterpart);
            result = buildHistoryData(forecast, null, counterpart, fulfillDate);
            settingDetailForCheckDownload = List.of(forecast);
        }

        return result.stream().map(r->{
            if(r.baseline() == null && r.baseline2() == null){
                return r;
            }
            List<DashboardSummary> dashboardSummaries = miDashboardService.getDashboardSummaries(settingDetailForCheckDownload, DateUtil.toYmd(fulfillDate),
                r.date().length() == 6 ? r.date()+"01" : r.date());
            boolean download = false;
            for(DashboardSummary dashboardSummary : dashboardSummaries){
                if(org.springframework.util.StringUtils.hasText(dashboardSummary.explanatoryFilePath())){
                    download = true;
                    break;
                }
            }
            return HistoryDataRes.builder()
                .date(r.date())
                .baseline(r.baseline())
                .baseline2(r.baseline2())
                .counterpart(r.counterpart())
                .download(download)
                .build();
        }).toList();

    }

    /**
     * counterpart 와 동일한 그룹의 forecast 선택
     */
    private ChartPredictSettingDetail selectForecast(List<ChartPredictSettingDetail> forecasts, ChartPredictSettingDetail counterpart) {

        if (forecasts.size() == 1) return forecasts.get(0);

        return forecasts.stream()
            .filter(f -> f.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo().equals(counterpart.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()))
            .findFirst()
            .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "No forecast matched to counterpart. counterpart-detail-no: %s".formatted(counterpart.getChartPredictSettingDetailNo())));
    }

    /**
     * HistoryDataRes 리스트 빌더
     */
    private List<HistoryDataRes> buildHistoryData(ChartPredictSettingDetail forecast1, ChartPredictSettingDetail forecast2, ChartPredictSettingDetail counterpart, LocalDate fulfillDate) {

        // 26.07.28 취약점 진단 수정, '누락된 널 값 검사-102157'
        if (fulfillDate == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "fulfillDate is required.");
        }

        List<TargetPredictResultData> forecastResults1 = getPredictResultData(forecast1.getGoodsDataSourceDetail(), fulfillDate);

        // 1. 예측 결과 날짜 추출
        List<LocalDate> dates1 = forecastResults1.stream()
            .map(TargetPredictResultData::getPredictResultDate)
            .filter(StringUtils::isNotEmpty)
            .map(DateUtil::fromYmd)
            .toList();

        // 2. 최소/최대 날짜 추출
        LocalDate minDate = dates1.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = dates1.stream().max(LocalDate::compareTo).orElse(LocalDate.now());

        List<TargetPredictResultData> forecastResults2 = null;
        if(forecast2 != null){
            forecastResults2 = getPredictResultData(forecast2.getGoodsDataSourceDetail(), fulfillDate);
            // 1. 예측 결과 날짜 추출
            List<LocalDate> dates2 = forecastResults2.stream()
                .map(TargetPredictResultData::getPredictResultDate)
                .filter(StringUtils::isNotEmpty)
                .map(DateUtil::fromYmd)
                .toList();
            LocalDate minDate2 = dates2.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
            LocalDate maxDate2 = dates2.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
            minDate = Collections.min(Arrays.asList(minDate, minDate2));
            maxDate = Collections.max(Arrays.asList(maxDate, maxDate2));
        }

        LocalDate baseDate = minDate.isBefore(fulfillDate) ? minDate : fulfillDate.with(TemporalAdjusters.firstDayOfMonth());

        // 3. 해당 달의 1일/말일로 변환 (실제가/선물가)
        String start = DateUtil.toYmd(baseDate);
        String end = DateUtil.toYmd(maxDate.with(TemporalAdjusters.lastDayOfMonth()));

        List<DoubleValueResultData> counterpartResults = getCounterPartResultData(counterpart.getGoodsDataSourceDetail(), start, end);

        int goodsNo = counterpart.getGoodsDataSourceDetail().getGoodsData().getGoods().getGoodsNo();
        boolean permitted = authorizationService.hasGoodsViewPermission(goodsNo);

        return HistoryDataRes.ofList(forecast1.getChartPredictSetting(), forecastResults1, forecastResults2, counterpartResults, counterpart.getIsFuture(), permitted, fulfillDate);
    }

    /**
     * 히스토리 다운로드 (엑셀)
     */
    public ByteArrayInputStream downloadHistoryData(Integer detailNo, LocalDate fulfillDate) {
        List<HistoryDataRes> historyData = getHistoryData(detailNo, fulfillDate);

        PriceType type = historyData.stream()
            .map(HistoryDataRes::counterpart)
            .filter(Objects::nonNull)
            .map(PriceEntry::type)
            .findFirst()
            .orElse(PriceType.ACTUAL);

        TableRes table = toTable(historyData, type);
        return ExcelUtils.toExcel(fulfillDate.toString(), table.headers(), table.rows());
    }

    private TableRes toTable(List<HistoryDataRes> data, PriceType counterpartType) {
        List<TableRes.Column> headers = HistoryTableHelper.headers(counterpartType);
        List<TableRes.Row> rows = data.stream()
            .map(HistoryTableHelper::mapToRow)
            .toList();
        return TableRes.of(headers, rows);
    }

    private List<DoubleValueResultData> getCounterPartResultData(GoodsDataSourceDetail gds, String start, String end) {
        String[] cols = CombineUtil.getSelectColumnArrays(gds.getTableColumnListNm());
        String dateCol = cols[0].strip();
        String valueCol = cols[1].strip();
        String select = CombineUtil.createSelectClause(CombineUtil.concatAlias(dateCol, "date"), CombineUtil.concatAlias(valueCol, "value"));
        String where = CombineUtil.createBetweenClause(dateCol, start, end);
        return queryService.getDoubleValueResultQuery(DynamicDataReq.of(gds, select, where, dateCol));
    }

    private List<TargetPredictResultData> getPredictResultGroupedData(GoodsDataSourceDetail gds) {
        String select = CombineUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        String groupBy = CombineUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        return queryService.getPredictResultQuery(DynamicDataReq.ofGroupByQuery(gds, select, groupBy));
    }

    private List<TargetPredictResultData> getPredictResultData(GoodsDataSourceDetail gds, LocalDate fulfillDate) {
        String select = CombineUtil.getBizTargetSelectClause(gds.getTableColumnListNm());
        String where = CombineUtil.createWhereClause(CombineUtil.BIZ_TARGET_FULFILL_DT_COLUMN, DateUtil.toYmd(fulfillDate));
        String resultDtColumn = CombineUtil.BIZ_TARGET_RESULT_DT_COLUMN;
        return queryService.getPredictResultQuery(DynamicDataReq.of(gds, select, where, resultDtColumn));
    }

    /**
     * 헤더 & 로우 빌더 유틸 (Service 코드 분리)
     */
    private static final class HistoryTableHelper {

        private static List<TableRes.Column> headers(PriceType type) {
            return List.of(
                TableRes.Column.of("date", "날짜"),
                TableRes.Column.of("baselinePrice", "예측가"),
                TableRes.Column.of("baselineChangeRate", "예측가 변동률(%)"),
                TableRes.Column.of("baselineMax", "예측가 최고"),
                TableRes.Column.of("baselineMin", "예측가 최저"),
                TableRes.Column.of("counterPrice", type == PriceType.ACTUAL ? "실제가" : "선물가"),
                TableRes.Column.of("counterChangeRate", type == PriceType.ACTUAL ? "실제가 변동률(%)" : "선물가 변동률(%)"),
                TableRes.Column.of("counterMax", type == PriceType.ACTUAL ? "실제가 최고" : "선물가 최고"),
                TableRes.Column.of("counterMin", type == PriceType.ACTUAL ? "실제가 최저" : "선물가 최저")
            );
        }

        private static TableRes.Row mapToRow(HistoryDataRes h) {
            PriceEntry base = h.baseline();
            PriceEntry counter = h.counterpart();

            PriceStat baseStat = base != null ? base.stat() : null;
            PriceStat counterStat = (counter != null && Boolean.TRUE.equals(counter.isPermitted())) ? counter.stat() : null;

            return TableRes.Row.of(List.of(
                h.date(),
                // baseline
                baseStat != null && baseStat.price() != null ? baseStat.price().toPlainString() : "-",
                baseStat != null && baseStat.changeRate() != null ? baseStat.changeRate().toPlainString() : "-",
                baseStat != null && baseStat.isMaximum() ? "Y" : "-",
                baseStat != null && baseStat.isMinimum() ? "Y" : "-",
                // counterpart
                counterStat != null && counterStat.price() != null ? counterStat.price().toPlainString() : "-",
                counterStat != null && counterStat.changeRate() != null ? counterStat.changeRate().toPlainString() : "-",
                counterStat != null && counterStat.isMaximum() ? "Y" : "-",
                counterStat != null && counterStat.isMinimum() ? "Y" : "-"
            ));
        }
    }
}
