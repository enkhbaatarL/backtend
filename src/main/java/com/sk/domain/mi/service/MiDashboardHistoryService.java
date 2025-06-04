package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.model.TableRes;
import com.sk.common.utils.ClauseUtil;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelUtils;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.context.ForecastContext;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryDataRes;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryMetadataRes;
import com.sk.domain.mi.dto.response.dashboard.history.PriceEntry;
import com.sk.domain.mi.dto.response.dashboard.history.PriceStat;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PriceType;
import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiDashboardHistoryService {

    private final AuthorizationService authorizationService;
    private final MybatisDynamicQueryService queryService;
    private final ChartPredictSettingRepository settingRepo;
    private final ChartPredictSettingDetailRepository settingDetailRepo;

    /**
     * 예측가 히스토리 메타데이터 조회
     */
    public HistoryMetadataRes getForecastHistoryMetadata() {
        Integer companyNo = authorizationService.getCompanyNoWithPriority();

        ChartPredictSetting setting = settingRepo
            .findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [chart-predict-setting]. companyNo: %s".formatted(companyNo)));

        List<ChartPredictSettingDetail> details = settingDetailRepo.findByChartPredictSettingAndIsDeletedFalse(setting);

        Map<Boolean, List<ChartPredictSettingDetail>> partitioned = details.stream()
            .collect(Collectors.partitioningBy(ChartPredictSettingDetail::getIsChartLinePredictVal));

        // 모든 예측가를 ForecastContext 로 변환
        List<ForecastContext> forecastContexts = partitioned.getOrDefault(Boolean.TRUE, List.of())
            .stream()
            .map(forecast -> toForecastContext(forecast, partitioned.get(Boolean.FALSE)))
            .toList();

        return HistoryMetadataRes.of(setting, forecastContexts);
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
     */
    public List<HistoryDataRes> getHistoryData(Integer detailNo, LocalDate fulfillDate) {

        ChartPredictSettingDetail counterpart = settingDetailRepo
            .findByChartPredictSettingDetailNoAndIsDeletedFalse(detailNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [chart-predict-setting-detail]. settingDetailNo: %s".formatted(detailNo)));

        ChartPredictSetting setting = counterpart.getChartPredictSetting();
        List<ChartPredictSettingDetail> details = settingDetailRepo.findByChartPredictSettingAndIsDeletedFalse(setting);

        List<ChartPredictSettingDetail> forecasts = details.stream()
            .filter(ChartPredictSettingDetail::getIsChartLinePredictVal)
            .toList();

        if (forecasts.isEmpty()) {
            return HistoryDataRes.ofEmpty();
        }

        ChartPredictSettingDetail forecast = selectForecast(forecasts, counterpart);
        return buildHistoryData(forecast, counterpart, fulfillDate);
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
    private List<HistoryDataRes> buildHistoryData(ChartPredictSettingDetail forecast, ChartPredictSettingDetail counterpart, LocalDate fulfillDate) {

        List<TargetPredictResultData> forecastResults = getPredictResultData(forecast.getGoodsDataSourceDetail(), fulfillDate);

        // 예측구간 추출
        String start = forecastResults.get(0).getPredictResultDate();
        String end = forecastResults.get(forecastResults.size() - 1).getPredictResultDate();

        List<DoubleValueResultData> counterpartResults = getCounterPartResultData(counterpart.getGoodsDataSourceDetail(), start, end);

        int goodsNo = counterpart.getGoodsDataSourceDetail().getGoodsData().getGoods().getGoodsNo();
        boolean permitted = authorizationService.hasGoodsViewPermission(goodsNo);

        return HistoryDataRes.ofList(forecastResults, counterpartResults, counterpart.getIsFuture(), permitted);
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
        return ExcelUtils.toExcel("history", table.headers(), table.rows());
    }

    private TableRes toTable(List<HistoryDataRes> data, PriceType counterpartType) {
        List<TableRes.Column> headers = HistoryTableHelper.headers(counterpartType);
        List<TableRes.Row> rows = data.stream()
            .map(HistoryTableHelper::mapToRow)
            .toList();
        return TableRes.of(headers, rows);
    }

    private List<DoubleValueResultData> getCounterPartResultData(GoodsDataSourceDetail gds, String start, String end) {
        String[] cols = ClauseUtil.getSelectColumnArrays(gds.getTableColumnListNm());
        String dateCol = cols[0].strip();
        String valueCol = cols[1].strip();
        String select = ClauseUtil.createSelectClause(ClauseUtil.concatAlias(dateCol, "date"), ClauseUtil.concatAlias(valueCol, "value"));
        String where = ClauseUtil.createBetweenClause(dateCol, start, end);
        return queryService.getDoubleValueResultQuery(DynamicDataReq.of(gds, select, where, dateCol));
    }

    private List<TargetPredictResultData> getPredictResultGroupedData(GoodsDataSourceDetail gds) {
        String select = ClauseUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        String groupBy = ClauseUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        return queryService.getPredictResultQuery(DynamicDataReq.ofGroupByQuery(gds, select, groupBy));
    }

    private List<TargetPredictResultData> getPredictResultData(GoodsDataSourceDetail gds, LocalDate fulfillDate) {
        String select = ClauseUtil.getBizTargetSelectClause(gds.getTableColumnListNm());
        String where = ClauseUtil.createWhereClause(ClauseUtil.BIZ_TARGET_FULFILL_DT_COLUMN, DateUtil.toYmd(fulfillDate));
        String resultDtColumn = ClauseUtil.BIZ_TARGET_RESULT_DT_COLUMN;
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
                h.date().toString(),
                // baseline
                baseStat != null ? baseStat.price().toPlainString() : "-",
                baseStat != null && baseStat.changeRate() != null ? baseStat.changeRate().toPlainString() : "-",
                baseStat != null && baseStat.isMaximum() ? "Y" : "-",
                baseStat != null && baseStat.isMinimum() ? "Y" : "-",
                // counterpart
                counterStat != null ? counterStat.price().toPlainString() : "-",
                counterStat != null && counterStat.changeRate() != null ? counterStat.changeRate().toPlainString() : "-",
                counterStat != null && counterStat.isMaximum() ? "Y" : "-",
                counterStat != null && counterStat.isMinimum() ? "Y" : "-"
            ));
        }
    }
}
