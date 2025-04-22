package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.StringUtil;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.chart.*;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.ChartMetadata;
import com.sk.domain.mi.dto.response.charts.ChartRes;
import com.sk.domain.mi.dto.response.charts.IndicatorRes;
import com.sk.domain.mi.service.chart.ChartBundleFactory;
import com.sk.domain.mi.service.chart.ChartGenerationService;
import com.sk.domain.mi.service.chart.PersonalizationService;
import com.sk.domain.mi.service.chart.TechnicalIndicatorService;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataUnitConvertRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final ChartGenerationService chartGenerationService;
    private final PersonalizationService personalizationService;
    private final TechnicalIndicatorService technicalIndicatorService;

    private final GoodsDataUnitConvertRepository goodsDataUnitConvertRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;


    /**
     * 차트 개인화를 설정합니다.
     *
     * @param goodsDataNo           상품 데이터 번호
     * @param goodsDataSourceNo     상품 데이터소스 번호
     * @param chartsPersonalizeReqs 차트 개인화 요청 데이터 리스트
     * @return 개인화된 차트 응답 데이터 리스트
     */
    @Transactional
    public List<ChartsPersonalizeRes> putChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo, List<ChartsPersonalizeReq> chartsPersonalizeReqs) {

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
     * 상품 데이터소스 차트 묶음을 조회합니다.
     *
     * @param goodsDataNo       상품 데이터 번호
     * @param goodsDataSourceNo 상품 데이터소스 번호
     * @param interval          인터벌 (예: 하루, 일주일 등 시간 간격)
     * @param startDate         조회 시작일 (YYYYMMDD 형식)
     * @param endDate           조회 종료일 (YYYYMMDD 형식)
     * @param unitConversionCd  단위 변환 코드 (카테고리명-상세유형명-변환후단위명 ... 아니다 ...)
     * @return 생성된 차트 묶음 응답 데이터
     */
    public ChartBundleRes getDynamicCharts(Integer goodsDataNo, Integer goodsDataSourceNo, Interval interval, Range range, String startDate, String endDate, String unitConversionCd) {

        goodsDataValidator.ensureExists(goodsDataNo).validOrThrow();

        ValidationResult<GoodsDataSourceDetail> goodsDataSourceResult = goodsDataSourceDetailValidator.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceResult.getOrThrow();

        List<GoodsDataUnitConvert> unitConversions = goodsDataUnitConvertRepository.findById_GoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo);

        Map<String, UnitConvert> unitCdMap = generateChangeableUnit(goodsDataSourceDetail, unitConversions);

        if (ObjectUtils.isNotEmpty(unitConversionCd) && !unitCdMap.containsKey(unitConversionCd)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "This chart cannot be converted using the corresponding unitConversionCd. unitConversionCd: %s".formatted(unitConversionCd));
        }

        List<PersonalizedChartSetting> personalizedCondition = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "testUser001");

        Map<ChartSettingConditionCd, List<String>> personalizedMap = personalizedCondition.stream()
                .collect(groupingBy(
                    PersonalizedChartSetting::getChartSettingConditionCd,
                    Collectors.mapping(PersonalizedChartSetting::getChartSettingConditionVal, Collectors.toList())
                ));

        ChartMetadata metadata = ChartMetadata.of(goodsDataSourceDetail, unitConversionCd, unitCdMap.keySet(), interval);
        ChartRes mainChart = chartGenerationService.generateChartData(goodsDataSourceDetail, startDate, endDate, interval, unitCdMap.get(unitConversionCd));
        List<IndicatorRes> indicators = technicalIndicatorService.generateIndicators(mainChart, personalizedMap);
        List<ChartRes> comparisonCharts = chartGenerationService.generateComparisonCharts(goodsDataSourceDetail, startDate, endDate, interval, personalizedMap);

        return ChartBundleFactory.create(metadata, mainChart, indicators, comparisonCharts);

    }

    /**
     * 단위 변환 가능한 단위 Map 객체를 생성한다. (key: 변환타입#변환단위, value: 단위변환기본 객체)
     *
     *
     * @param goodsDataSourceDetail
     * @param unitConversions
     */
    private Map<String, UnitConvert> generateChangeableUnit(GoodsDataSourceDetail goodsDataSourceDetail, List<GoodsDataUnitConvert> unitConversions) {

        Map<String, UnitConvert> unitCdMap = unitConversions.stream().collect(
            Collectors.toMap(s -> StringUtil.joinPoundSignNonNull(s.getUnitConvert().getConvertTargetDetailTypeNm(), s.getUnitConvert().getConvertAfterUnitNm()), GoodsDataUnitConvert::getUnitConvert,
                (v1, v2) -> v2));

        // default unit
        unitCdMap.put(StringUtil.joinPoundSignNonNull(goodsDataSourceDetail.getConvertTargetDetailTypeNm(), goodsDataSourceDetail.getGoodsDataSourceUnitNm()), null);

        return unitCdMap;

    }

}
