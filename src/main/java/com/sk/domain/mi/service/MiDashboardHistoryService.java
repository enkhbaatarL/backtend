package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.HistoryMetadataRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardHistoryService {

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;

    public HistoryMetadataRes getForecastHistoryMetadata(Integer companyNo) {

        ChartPredictSetting foreSetting = chartPredictSettingRepository.findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "해당 고객사에 대한 예측 설정이 존재하지 않습니다."));

        List<ChartPredictSettingDetail> foreSettingDetails = chartPredictSettingDetailRepository.findByChartPredictSettingAndIsDeletedFalse(foreSetting);

        Map<Boolean, List<ChartPredictSettingDetail>> partitioned = foreSettingDetails.stream()
            .collect(Collectors.partitioningBy(ChartPredictSettingDetail::getIsChartLinePredictVal));

        // 예측가
        List<ChartPredictSettingDetail> forecasts = partitioned.get(Boolean.TRUE);

        // 현물가 & 선물가
        List<ChartPredictSettingDetail> actualWithFutures = partitioned.get(Boolean.FALSE);

        // 예측시점은 모두 동일
        ChartPredictSettingDetail chartPredictSettingDetail = forecasts.get(0);

        GoodsDataSourceDetail goodsDataSourceDetail = chartPredictSettingDetail.getGoodsDataSourceDetail();

        return null;
    }

    public List<TargetPredictResultData> getData(GoodsDataSourceDetail goodsDataSourceDetail) {

        String selectClause = "prdc_fulf_dt";
        String groupByColumn = "prdc_fulf_dt";

        return mybatisDynamicQueryService.getPredictResultQuery(DynamicDataReq.ofGroupByQuery(goodsDataSourceDetail, selectClause, groupByColumn));
    }

}
