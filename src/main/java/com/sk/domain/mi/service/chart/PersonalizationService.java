package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    /**
     * 사용자의 개인화 차트 설정을 조회한다.
     * 상품 데이터소스와 사용자 ID를 기반으로 저장된 개인화 설정을 데이터베이스에서 가져온다.
     *
     * @param goodsDataSourceDetail 상품 데이터소스 상세
     * @param userLoginId 사용자 로그인 ID
     * @return 개인화 차트 설정 목록
     */
    public List<PersonalizedChartSetting> getPersonalizedSettings(GoodsDataSourceDetail goodsDataSourceDetail, String userLoginId) {
        return personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, userLoginId);
    }

    /**
     * 개인화 차트 설정을 Map 형태로 변환한다.
     * 개인화 설정 목록을 ChartSettingConditionCd를 키로 하고 설정값 리스트를 값으로 하는 맵으로 변환한다.
     * 설정이 없는 경우에도 모든 ChartSettingConditionCd에 대해 빈 리스트를 가진 맵을 반환한다.
     *
     * @param settings 개인화 차트 설정 목록
     * @return 개인화 차트 설정 Map (ChartSettingConditionCd -> List<String>)
     */
    public Map<ChartSettingConditionCd, List<String>> convertToPersonalizedMap(List<PersonalizedChartSetting> settings) {
        Map<ChartSettingConditionCd, List<String>> personalizedMap = new HashMap<>();

        // 모든 ChartSettingConditionCd에 대해 빈 리스트 초기화
        for (ChartSettingConditionCd cd : ChartSettingConditionCd.values()) {
            personalizedMap.put(cd, new ArrayList<>());
        }

        // 설정이 없는 경우 기본 맵 반환
        if (settings == null || settings.isEmpty()) {
            return personalizedMap;
        }

        // 설정을 Map으로 그룹화
        Map<ChartSettingConditionCd, List<PersonalizedChartSetting>> groupedSettings = settings.stream()
                .collect(Collectors.groupingBy(PersonalizedChartSetting::getChartSettingConditionCd));

        // 각 그룹에 대해 값 리스트 생성
        groupedSettings.forEach((key, valueList) -> {
            List<String> values = valueList.stream()
                    .map(PersonalizedChartSetting::getChartSettingConditionVal)
                    .collect(Collectors.toList());
            personalizedMap.put(key, values);
        });

        return personalizedMap;
    }

    /**
     * 개인화 설정에서 Range 값을 가져옵니다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 Range 값 또는 기본값
     */
    public Pair<String, String> getPersonalizedRange(Map<ChartSettingConditionCd, List<String>> personalizedMap, Range reqRange, String customStart, String customEnd) {

        if (reqRange != null) {
            return DateUtil.getDateRange(reqRange, customStart, customEnd); // request value
        }

        List<String> rangeValues = personalizedMap.get(ChartSettingConditionCd.RANGE);

        if (rangeValues == null || rangeValues.isEmpty()) {
            return DateUtil.getDateRange(Range.ONE_YEAR); // default value
        }

        String rangeValue = rangeValues.get(0);

        for (Range range : Range.values()) {
            if (range.getCode().equals(rangeValue)) {
                return DateUtil.getDateRange(range);
            }
        }

        return DateUtil.getDateRange(Range.ONE_YEAR);
    }

    /**
     * 개인화 설정에서 Interval 값을 가져온다.
     * 개인화 설정 맵에서 INTERVAL 키에 해당하는 값 목록의 첫 번째 값을 기준으로 Interval enum을 찾는다.
     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @param reqInterval 기본 Interval 값
     * @return 개인화된 Interval 값 또는 기본값
     */
    public Interval getPersonalizedInterval(Map<ChartSettingConditionCd, List<String>> personalizedMap, Interval reqInterval) {

        if (reqInterval != null) {
            return reqInterval; // request value
        }

        List<String> intervalValues = personalizedMap.get(ChartSettingConditionCd.INTERVAL);

        if (intervalValues == null || intervalValues.isEmpty()) {
            return Interval.ONE_DAY; // default value
        }

        String intervalValue = intervalValues.get(0);

        for (Interval interval : Interval.values()) {
            if (interval.getCode().equals(intervalValue)) {
                return interval;
            }
        }
        return Interval.ONE_DAY;
    }

    /**
     * 개인화 설정에서 ChartType 값을 가져온다.
     * 개인화 설정 맵에서 CHART 키에 해당하는 값 목록의 첫 번째 값을 기준으로 ChartType enum을 찾는다.
     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @param defaultChartType 기본 ChartType 값
     * @return 개인화된 ChartType 값 또는 기본값
     */
    public ChartType getPersonalizedChartType(Map<ChartSettingConditionCd, List<String>> personalizedMap, ChartType defaultChartType) {
        List<String> chartTypeValues = personalizedMap.get(ChartSettingConditionCd.CHART);
        if (chartTypeValues == null || chartTypeValues.isEmpty()) {
            return defaultChartType;
        }

        // ChartType 값은 1개만 있어야 함
        String chartTypeValue = chartTypeValues.get(0);

        // 값으로 ChartType enum 찾기
        for (ChartType chartType : ChartType.values()) {
            if (chartType.name().equals(chartTypeValue)) {
                return chartType;
            }
        }

        return defaultChartType;
    }

    /**
     * 개인화 설정에서 Unit 값을 가져온다.
     * 개인화 설정 맵에서 UNIT 키에 해당하는 값 목록의 첫 번째 값을 반환한다.
     * 설정이 없는 경우 기본값을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @param defaultUnit 기본 Unit 값
     * @return 개인화된 Unit 값 또는 기본값
     */
    // todo.. 유닛 디폴트
    public String getPersonalizedUnit(Map<ChartSettingConditionCd, List<String>> personalizedMap, String defaultUnit) {
        List<String> unitValues = personalizedMap.get(ChartSettingConditionCd.UNIT);
        if (unitValues == null || unitValues.isEmpty()) {
            return defaultUnit;
        }

        // Unit 값은 1개만 있어야 함
        return unitValues.get(0);
    }

    /**
     * 개인화 설정에서 INDICATOR 값 목록을 가져옵니다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 INDICATOR 값 목록
     */
    public List<String> getPersonalizedIndicators(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<String> indicatorValues = personalizedMap.get(ChartSettingConditionCd.INDICATOR);
        if (indicatorValues == null) {
            return new ArrayList<>();
        }
        return indicatorValues;
    }

    /**
     * 개인화 설정에서 COMP_DATA_SOURCE 값 목록을 가져옵니다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 COMP_DATA_SOURCE 값 목록
     */
    public List<Integer> getPersonalizedCompDataSources(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<Integer> compDataSourceValues = personalizedMap.get(ChartSettingConditionCd.COMP_DATA_SOURCE).stream().map(Integer::parseInt).toList();
        if (compDataSourceValues == null) {
            return new ArrayList<>();
        }
        return compDataSourceValues;
    }
}
