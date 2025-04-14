package com.sk.domain.mi.service;

import com.sk.common.model.ValidationResult;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    public PermissionRes getGoodsDataCharts(Integer goodsDataNo) {

        ValidationResult<GoodsData> goodsDataValid = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataValid.getOrThrow();

        // 1. 상품 데이터 소스 상세 (메인 차트)
        List<GoodsDataSourceDetail> goodsDataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);

        // 2. 차트 별 개인화 차트 설정 내역 조회
        // 3. 데이터 소스 그룹 매핑 조인(비교군 차트)
        // 4. 단위 변환 및 indicator 추가
        return null;
    }

    @Transactional(transactionManager = "dynamicTransactionManager")
    public List<Map<String, Object>> getGoodsDataCharts(GoodsDataSourceDetail goodsDataSourceDetail) {
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail));
    }

}
