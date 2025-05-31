package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.ClauseUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.context.ForecastContext;
import com.sk.domain.mi.dto.response.dashboard.HistoryMetadataRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardHistoryService {

    private final AuthorizationService authorizationService;

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;

    /**
     * 예측가 이력 메타데이터 조회
     *
     * @return HistoryMetadataRes
     */
    public HistoryMetadataRes getForecastHistoryMetadata() {

        Integer companyNo = authorizationService.getCompanyNoWithPriority();

        ChartPredictSetting historySetting = chartPredictSettingRepository.findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [chart-predict-setting]. companyNo: %s".formatted(companyNo)));

        List<ChartPredictSettingDetail> historySettingDetails = chartPredictSettingDetailRepository.findByChartPredictSettingAndIsDeletedFalse(historySetting);

        Map<Boolean, List<ChartPredictSettingDetail>> partitioned = historySettingDetails.stream()
            .collect(Collectors.partitioningBy(ChartPredictSettingDetail::getIsChartLinePredictVal));

        // 예측가
        List<ChartPredictSettingDetail> forecasts = partitioned.get(Boolean.TRUE);

        // 현물가 & 선물가
        List<ChartPredictSettingDetail> spotsWithFutures = partitioned.get(Boolean.FALSE);

        // 예측가 데이터가 1개인 경우
        if (forecasts.size() == 1) {

            ChartPredictSettingDetail forecast = forecasts.get(0);

            GoodsDataSourceDetail gds = forecast.getGoodsDataSourceDetail();
            List<TargetPredictResultData> targetPredictResultData = getPredictResultData(gds);

            List<String> fulfillDates = targetPredictResultData.stream().map(TargetPredictResultData::getPredictFulfillDate).toList();

            List<ChartPredictSettingDetail> spots = spotsWithFutures.stream().filter(awf -> !awf.getIsFuture()).toList();
            List<ChartPredictSettingDetail> futures = spotsWithFutures.stream().filter(ChartPredictSettingDetail::getIsFuture).toList();

            return HistoryMetadataRes.of(historySetting, List.of(ForecastContext.of(forecast, fulfillDates, spots, futures)));

        } else if (forecasts.size() >= 2) {
            // 예측가 데이터가 2개 이상인 경우

            Map<Integer, List<ChartPredictSettingDetail>> settingDetailMap = groupByForecastDetailNo(historySettingDetails);

            List<ForecastContext> forecastContexts = forecasts.stream().map(f -> {

                GoodsDataSourceDetail gds = f.getGoodsDataSourceDetail();
                List<TargetPredictResultData> targetPredictResultData = getPredictResultData(gds);

                List<String> fulfillDates = targetPredictResultData.stream().map(TargetPredictResultData::getPredictFulfillDate).toList();
                List<ChartPredictSettingDetail> groupedSpotsWithFutures = settingDetailMap.getOrDefault(f.getChartPredictSettingDetailNo(), Collections.emptyList());
                List<ChartPredictSettingDetail> spots = groupedSpotsWithFutures.stream()
                    .filter(awf -> !awf.getIsChartLinePredictVal())
                    .filter(awf -> !awf.getIsFuture())
                    .toList();
                List<ChartPredictSettingDetail> futures = groupedSpotsWithFutures.stream()
                    .filter(awf -> !awf.getIsChartLinePredictVal())
                    .filter(ChartPredictSettingDetail::getIsFuture)
                    .toList();

                return ForecastContext.of(f, fulfillDates, spots, futures);

            }).toList();

            return HistoryMetadataRes.of(historySetting, forecastContexts);

        } else {

            return HistoryMetadataRes.of(historySetting);

        }

    }

    public List<TargetPredictResultData> getPredictResultData(GoodsDataSourceDetail goodsDataSourceDetail) {
        String selectClause = ClauseUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        String groupByColumn = ClauseUtil.BIZ_TARGET_FULFILL_DT_COLUMN;
        return mybatisDynamicQueryService.getPredictResultQuery(DynamicDataReq.ofGroupByQuery(goodsDataSourceDetail, selectClause, groupByColumn));
    }

    public Map<Integer, List<ChartPredictSettingDetail>> groupByForecastDetailNo(List<ChartPredictSettingDetail> historySettingDetails) {

        // 1. 연관상품데이터소스번호 기준으로 1차 그룹핑
        Map<Integer, List<ChartPredictSettingDetail>> groupedByAssoNo = historySettingDetails.stream()
            .collect(Collectors.groupingBy(d -> d.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()));

        // 각 그룹에서 예측가를 찾아 데이터소스 번호를 키로 사용하고, 해당 그룹의 리스트를 값으로 하는 Map 생성
        return groupedByAssoNo.values()
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

}
