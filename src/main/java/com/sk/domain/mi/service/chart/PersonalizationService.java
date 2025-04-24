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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    /**
     * 개인화 차트 설정을 Map 형태로 변환한다.
     * 개인화 설정 ChartSettingConditionCd를 키로 하고 설정값 리스트를 값으로 하는 맵으로 변환한다.
     * 설정이 없는 경우에도 모든 ChartSettingConditionCd에 대해 빈 리스트를 가진 맵을 반환한다.
     *
     * @param gds         데이터소스상품상세 객체
     * @param userLoginId 로그인 아이디
     * @return 개인화 차트 설정 Map (ChartSettingConditionCd -> List<String>)
     */
    public Map<ChartSettingConditionCd, List<String>> getConvertPersonalizedMap(GoodsDataSourceDetail gds, String userLoginId) {

        if (StringUtils.isEmpty(userLoginId)) {
            return createEmptyPersonalizedMap();
        }

        // 개인화 설정 조회
        List<PersonalizedChartSetting> personalSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(gds, userLoginId);

        // 기본 맵 생성 (모든 ChartSettingConditionCd에 대해 빈 리스트 초기화)
        Map<ChartSettingConditionCd, List<String>> personalizedMap = createEmptyPersonalizedMap();

        // 설정이 없는 경우 기본 맵 반환, ADMIN(타사접속가능), API_PLUS_OPERATOR Role로 접근 시, 기본 설정 값으로 반환 됨. -> 개인화 설정을 할 수 없음.
        if (personalSettings == null || personalSettings.isEmpty()) {
            return personalizedMap;
        }

        personalSettings.stream()
            .filter(setting -> setting != null && setting.getChartSettingConditionCd() != null)
            .collect(Collectors.groupingBy(PersonalizedChartSetting::getChartSettingConditionCd))
            .forEach((key, valueList) -> {
                List<String> values = valueList.stream().map(PersonalizedChartSetting::getChartSettingConditionVal).filter(Objects::nonNull).collect(Collectors.toList());
                personalizedMap.put(key, values);
            });

        return personalizedMap;

    }

    /**
     * 모든 ChartSettingConditionCd에 대해 빈 리스트를 가진 맵을 생성한다.
     * -> 개인화 설정이 없는 경우에도 기본적으로 모든 설정을 default 설정으로 초기화하기 위해 사용된다.
     *
     * @return 빈 개인화 설정 맵
     */
    private Map<ChartSettingConditionCd, List<String>> createEmptyPersonalizedMap() {
        Map<ChartSettingConditionCd, List<String>> emptyMap = new HashMap<>();
        for (ChartSettingConditionCd cd : ChartSettingConditionCd.values()) {
            emptyMap.put(cd, new ArrayList<>());
        }
        return emptyMap;
    }

    /**
     * 개인화 설정에서 Range 값을 가져온다.
     * 요청된 범위가 있으면 그 값을 우선 사용하고, 없는 경우 개인화 설정을 확인한다.
     * 개인화 설정 맵에서 RANGE 키에 해당하는 값 목록의 첫 번째 값을 기준으로 Range enum을 찾는다.
     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값(ONE_YEAR)을 반환한다.
     *
     * @param pMap         개인화 설정 Map
     * @param reqRange     요청된 Range 값
     * @param reqStartDate 사용자 지정 시작일 (YYYYMMDD 형식)
     * @param reqEndDate   사용자 지정 종료일 (YYYYMMDD 형식)
     * @return 시작일과 종료일 쌍 (Pair<String, String>)
     */
    public Pair<String, String> getPsRangeOrDefault(Map<ChartSettingConditionCd, List<String>> pMap, Range reqRange, String reqStartDate, String reqEndDate) {

        // 1. 요청된 범위가 있으면 우선 사용
        if (ObjectUtils.isNotEmpty(reqRange)) {
            if (reqRange == Range.CUSTOM) {
                if (StringUtils.isEmpty(reqStartDate) || StringUtils.isEmpty(reqEndDate)) {
                    throw new IllegalArgumentException("The 'startDate' and 'endDate' are required for the [CUSTOM] range.");
                }
                return Pair.of(reqStartDate, reqEndDate);
            }
            return DateUtil.getDateRange(reqRange);
        }

        // 2. 개인화 설정이 없다면 default 값 사용
        List<String> rangeValues = pMap.get(ChartSettingConditionCd.RANGE);
        if (ObjectUtils.isEmpty(rangeValues)) {
            return DateUtil.getDateRange(Range.ONE_YEAR);
        }

        // 3. 개인화 설정에 따른 범위 값 조회
        String rangeValue = rangeValues.get(0);
        return DateUtil.getDateRange(Range.findByCode(rangeValue));

    }

    /**
     * 개인화 설정에서 Interval 값을 가져온다.
     * 요청된 인터벌이 있으면 그 값을 우선 사용하고, 없는 경우 개인화 설정을 확인한다.
     * 개인화 설정 맵에서 INTERVAL 키에 해당하는 값 목록의 첫 번째 값을 기준으로 Interval enum을 찾는다.
     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값(ONE_DAY)을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @param reqInterval     요청된 Interval 값
     * @return 개인화된 Interval 값 또는 기본값
     */
    public Interval getPsIntervalOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap, Interval reqInterval) {

        // 1. 요청된 인터벌이 있으면 우선 사용
        if (ObjectUtils.isNotEmpty(reqInterval)) {
            return reqInterval;
        }

        // 2. 개인화 설정이 없다면 default 인터벌 사용
        List<String> intervalValues = personalizedMap.get(ChartSettingConditionCd.INTERVAL);
        if (ObjectUtils.isEmpty(intervalValues)) {
            return Interval.ONE_DAY;
        }

        // 3. 개인화 설정에 따른 인터벌 사용
        String intervalValue = intervalValues.get(0);
        return Interval.findByCode(intervalValue);
    }

    /**
     * 개인화 설정에서 ChartType 값을 가져온다.
     * 개인화 설정 맵에서 CHART 키에 해당하는 값 목록의 첫 번째 값을 기준으로 ChartType enum을 찾는다.
     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 ChartType 값 또는 기본값
     */
    public ChartType getPsChartTypeOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        List<String> chartTypeValues = personalizedMap.get(ChartSettingConditionCd.CHART);
        if (ObjectUtils.isEmpty(chartTypeValues)) {
            return ChartType.LINE;
        }

        String chartTypeValue = chartTypeValues.get(0);

        return ChartType.valueOf(chartTypeValue);
    }

    /**
     * 개인화 설정에서 Unit 값을 가져온다.
     * 개인화 설정 맵에서 UNIT 키에 해당하는 값 목록의 첫 번째 값을 반환한다.
     * 설정이 없는 경우 기본값을 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @param reqUnitCd       요청받은 Unit 값
     * @param defaultUnitCd   기본 Unit 값(by. gds)
     * @return 개인화된 Unit 값 또는 기본값
     */
    public String getPsUnitCdOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap, String reqUnitCd, String defaultUnitCd) {

        // 1. 요청된 단위가 있으면 우선 사용
        if (ObjectUtils.isNotEmpty(reqUnitCd)) {
            return reqUnitCd;
        }

        // 2. 개인화 설정이 없다면 기본값 사용
        List<String> unitValues = personalizedMap.get(ChartSettingConditionCd.UNIT);
        if (ObjectUtils.isEmpty(unitValues)) {
            return defaultUnitCd;
        }

        // 3. 개인화 설정에 따른 단위 사용
        return unitValues.get(0);
    }

    /**
     * 개인화 설정에서 INDICATOR 값 목록을 가져온다.
     * 개인화 설정 맵에서 INDICATOR 키에 해당하는 값 목록을 반환한다.
     * 설정이 없는 경우 빈 리스트를 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 INDICATOR 값 목록
     */
    public List<String> getPersonalizedIndicators(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        // 유효성 검증
        if (personalizedMap == null) {
            return Collections.emptyList();
        }

        List<String> indicatorValues = personalizedMap.get(ChartSettingConditionCd.INDICATOR);
        if (indicatorValues == null) {
            return Collections.emptyList();
        }

        return indicatorValues;
    }

    /**
     * 개인화 설정에서 COMP_DATA_SOURCE 값 목록을 가져온다.
     * 개인화 설정 맵에서 COMP_DATA_SOURCE 키에 해당하는 값 목록을 정수로 변환하여 반환한다.
     * 설정이 없거나 변환 중 오류가 발생한 경우 빈 리스트를 반환한다.
     *
     * @param personalizedMap 개인화 설정 Map
     * @return 개인화된 COMP_DATA_SOURCE 값 목록
     */
    public List<Integer> getPersonalizedCompDataSources(Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        List<String> compDataSourceStrings = personalizedMap.get(ChartSettingConditionCd.COMP_DATA_SOURCE);
        if (compDataSourceStrings == null || compDataSourceStrings.isEmpty()) {
            return Collections.emptyList();
        }

        // 문자열을 정수로 변환
        return compDataSourceStrings.stream()
            .filter(s -> s != null && !s.isEmpty())
            .map(Integer::parseInt)
            .collect(Collectors.toList());
    }

}
