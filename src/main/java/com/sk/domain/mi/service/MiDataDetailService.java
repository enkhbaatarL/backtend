package com.sk.domain.mi.service;

import com.sk.common.model.ValidationResult;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.charts.ChartsPersonalizeRes;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.ChartMetadata;
import com.sk.domain.mi.dto.response.charts.Chart;
import com.sk.domain.mi.dto.response.charts.Indicator;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes;
import com.sk.domain.mi.service.chart.*;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final GoodsDataCategoryRepository goodsDataCategoryRepository;

    private final ChartGenerationService chartGenerationService;
    private final PersonalizationService personalizationService;
    private final TechnicalIndicatorService technicalIndicatorService;

    private final ChartUnitConversionService chartUnitConversionService;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    public GoodsDataRes getGoodsData(Integer goodsNo, Integer goodsDataNo) {

        ValidationResult<GoodsData> goodsDataResult = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataResult.getOrThrow();

        String providerNm = goodsData.getGoods().getProvider().getProviderNm();

        return null;
    }

    /**
     * 차트 개인화를 설정합니다.
     *
     * @param goodsDataNo           상품 데이터 번호
     * @param goodsDataSourceNo     상품 데이터소스 번호
     * @param chartsPersonalizeReqs 차트 개인화 요청 데이터 리스트
     * @return 개인화된 차트 응답 데이터 리스트
     */
    @Transactional
    public List<ChartsPersonalizeRes> putGoodsDataSourcesChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo, List<ChartsPersonalizeReq> chartsPersonalizeReqs) {

        ValidationResult<GoodsDataSourceDetail> goodsDataSourceResult = goodsDataSourceDetailValidator.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceResult.getOrThrow();

        List<PersonalizedChartSetting> existing = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "testUser001");

        // --- 2) incoming을 (code,val) 키로 묶기 ---
        // 이 키로 lookup 용 Map 생성

        Map<String, ChartsPersonalizeReq> incomingMap = chartsPersonalizeReqs.stream()
            .collect(Collectors.toMap(s -> s.chartSettingConditionCd().name() + ":" + s.chartConditionValue(), Function.identity()
                , (old, latest) -> latest));

        // --- 3) existing 순회하면서 update or delete 후보 분리 ---
        List<PersonalizedChartSetting> toDelete = new ArrayList<>();

        for (PersonalizedChartSetting curr : existing) {
            String key = curr.getChartSettingConditionCd().name() + ":" + curr.getChartSettingConditionVal();
            if (incomingMap.containsKey(key)) {
                // 3-1) 키가 그대로 존재한다면 → “갱신”이 필요 없다(값도 이미 같으니)
                incomingMap.remove(key);
            } else {
                // 3-2) DB엔 있는데 요청엔 없는 조합 → delete
                toDelete.add(curr);
            }
        }

        // --- 4) delete 반영 ---
        if (!toDelete.isEmpty()) {
            toDelete.forEach(BaseEntity::softDelete);
        }

        // --- 5) 남은 incoming → insert 대상 ---
        List<PersonalizedChartSetting> toInsert = new ArrayList<>();
        for (ChartsPersonalizeReq req : incomingMap.values()) {
            toInsert.add(PersonalizedChartSetting.builder()
                .userLoginId("testUser001")
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .chartSettingConditionCd(req.chartSettingConditionCd())
                .chartSettingConditionVal(req.chartConditionValue())
                .build()
            );
        }
        if (!toInsert.isEmpty()) {
            personalizedChartSettingRepository.saveAll(toInsert);
        }

        return null;
    }

    /**
     * 상품 데이터소스 차트 묶음을 조회한다.
     * 사용자의 개인화 설정에 따라 차트의 범위, 인터벌, 단위 등이 조정된다.
     * 개인화 설정이 없는 경우 기본값을 사용한다.
     *
     * @param gdNo      상품 데이터 번호
     * @param gdsNo     상품 데이터소스 번호
     * @param interval  인터벌 (예: 하루, 일주일 등 시간 간격)
     * @param range     범위 (예: 1주, 1개월, 3개월 등)
     * @param startDate 커스텀 범위 조회 시작일 (YYYYMMDD 형식)
     * @param endDate   커스텀 범위 조회 종료일 (YYYYMMDD 형식)
     * @param unitCd    단위 변환 코드 (상세유형명#변환후단위명)
     * @return 생성된 차트 묶음 응답 데이터
     */
    public ChartBundleRes getGoodsDataSourceCharts(Integer gdNo, Integer gdsNo, Interval interval, Range range, String startDate, String endDate, String unitCd, String userLoginId) {

        // 상품-데이터 유효성 검증
        goodsDataValidator.ensureExists(gdNo).validOrThrow();

        // 상품-데이터-소스 유효성 검증 및 조회
        ValidationResult<GoodsDataSourceDetail> gdsResult = goodsDataSourceDetailValidator.ensureExists(gdsNo);
        GoodsDataSourceDetail gds = gdsResult.getOrThrow();

        // 변환 가능한 단위 맵 조회 (GDS 기본 단위 포함)
        Map<String, UnitConvert> unitCdMap = chartUnitConversionService.getConvertUnitCdMap(gds);

        // 개인화 설정 맵 조회
        Map<ChartSettingConditionCd, List<String>> personalizedMap = personalizationService.getConvertPersonalizedMap(gds, userLoginId);

        Pair<String, String> determinedRange = personalizationService.getPsRangeOrDefault(personalizedMap, range, startDate, endDate);
        Interval determinedInterval = personalizationService.getPsIntervalOrDefault(personalizedMap, interval);
        String determinedUnitCd = personalizationService.getPsUnitCdOrDefault(personalizedMap, unitCd, ChartUnitConversionService.generateDefaultUnitCd(gds));
        ChartType determinedChartType = personalizationService.getPsChartTypeOrDefault(personalizedMap);

        // 결정된 단위코드 유효성 검증
        chartUnitConversionService.validateUnitCd(unitCdMap, determinedUnitCd);

        // 메타데이터 생성
        ChartMetadata metadata = ChartMetadata.of(gds.getGoodsDataSourceCurrencyUnitNm(), determinedChartType, determinedUnitCd, unitCdMap.keySet(), determinedInterval);

        // 메인 차트 생성
        Chart mainChart = chartGenerationService.generateChartData(
            gds,
            determinedRange.getLeft(),
            determinedRange.getRight(),
            determinedInterval,
            unitCdMap.get(determinedUnitCd),
            Boolean.TRUE // 메인 차트는 항상 visible 상태
        );

        // 기술적 지표 생성
        List<Indicator> indicators = technicalIndicatorService.generateIndicators(
            mainChart,
            personalizedMap
        );

        // 비교 차트 생성
        List<Chart> comparisonCharts = chartGenerationService.generateComparisonCharts(
            gds,
            determinedRange.getLeft(),
            determinedRange.getRight(),
            determinedInterval,
            personalizedMap
        );

        return ChartBundleFactory.create(metadata, mainChart, indicators, comparisonCharts);
    }

}
