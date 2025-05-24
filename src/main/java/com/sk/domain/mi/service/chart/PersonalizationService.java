package com.sk.domain.mi.service.chart;

import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PersonalizationService {

    private final PersonalizedChartSettingRepository repository;

    public Preferences resolve(GoodsDataSourceDetail gds, Role role, String userLoginId) {

        EnumMap<ChartSettingConditionCd, List<String>> raw = initializeEmpty();

        // 로그인 안 한 경우(혹은 미개발 환경) → 빈 Preferences 반환
        if (StringUtils.isBlank(userLoginId) || Role.USER != role) {
            return new Preferences(raw);
        }

        // DB 조회
        List<PersonalizedChartSetting> rows = repository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(gds, userLoginId);

        // 그룹핑 후 EnumMap 채우기
        raw.putAll(rows.stream()
            .filter(row -> row.getChartSettingConditionCd() != null)
            .collect(Collectors.groupingBy(
                PersonalizedChartSetting::getChartSettingConditionCd,
                () -> new EnumMap<>(ChartSettingConditionCd.class),
                Collectors.mapping(PersonalizedChartSetting::getChartSettingConditionVal, Collectors.toList())
            )));

        return new Preferences(raw);
    }

    private EnumMap<ChartSettingConditionCd, List<String>> initializeEmpty() {
        EnumMap<ChartSettingConditionCd, List<String>> map = new EnumMap<>(ChartSettingConditionCd.class);
        for (ChartSettingConditionCd cd : ChartSettingConditionCd.values()) {
            map.put(cd, Collections.emptyList());
        }
        return map;
    }

    /**
     * 개인화 결과를 표현하는 값 객체.
     * 노출용이므로 불변(Immutable) 하게 설계한다.
     *
     * @param raw Raw 문자열 형태의 개인화 정보
     */
    public record Preferences(EnumMap<ChartSettingConditionCd, List<String>> raw) {

        /* ---------- Enum 타입 값 ---------- */

        public Optional<Range> range() {
            return first(raw, ChartSettingConditionCd.RANGE).map(Range::findByCode);
        }

        public Optional<Interval> interval() {
            return first(raw, ChartSettingConditionCd.INTERVAL).map(Interval::findByCode);
        }

        public Optional<ChartType> chartType() {
            return first(raw, ChartSettingConditionCd.CHART).map(ChartType::valueOf);
        }

        public Optional<UnitCd> unitCd() {
            return first(raw, ChartSettingConditionCd.UNIT_CD).map(UnitCd::parse);
        }

        public List<String> indicators() {
            return list(raw, ChartSettingConditionCd.INDICATOR);
        }

        public List<Integer> compDataSourceIds() {
            return list(raw, ChartSettingConditionCd.COMP_DATA_SOURCE).stream().map(Integer::valueOf).toList();
        }

        public Optional<Pair<String, String>> periodDates() {
            if (range().orElse(null) != Range.CUSTOM) {
                return Optional.empty();
            }
            List<String> dates = list(raw, ChartSettingConditionCd.RANGE);
            if (dates.size() != 2) {
                return Optional.empty();
            }
            return Optional.of(Pair.of(dates.get(0), dates.get(1)));
        }

        private static Optional<String> first(Map<ChartSettingConditionCd, List<String>> map, ChartSettingConditionCd key) {
            return map.getOrDefault(key, List.of())
                .stream()
                .filter(StringUtils::isNotBlank)
                .findFirst();
        }

        private static List<String> list(Map<ChartSettingConditionCd, List<String>> map, ChartSettingConditionCd key) {
            return map.getOrDefault(key, List.of());
        }
    }


//    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

//    /**
//     * 사용자의 차트 개인화 설정을 조회하여 통합 객체로 반환한다.
//     *
//     * @param gdsId    데이터소스 ID
//     * @param loginId  사용자 로그인 ID
//     * @param fallback 사용자가 파라미터로 보낸 기본 요청 값 (개인화가 없으면 이 값 사용)
//     */
//    public ChartPersonalization resolve(GoodsDataSourceDetail gds, String loginId, ChartRequest fallback) {
//        // 1) DB → EnumMap<조건코드, List<String>>
//        EnumMap<ChartSettingConditionCd, List<String>> map = new EnumMap<>(ChartSettingConditionCd.class);
//        personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(gds, loginId).forEach(e -> map.put(e.getConditionCd(), e.getValues()));
//
//        // 2) EnumMap → ChartPersonalization (각 항목별 fallback 처리)
//        return ChartPersonalization.builder()
//            .range(extractEnum(map, ChartSettingConditionCd.RANGE, fallback::range))
//            .interval(extractEnum(map, ChartSettingConditionCd.INTERVAL, fallback::interval))
//            .unitCd(extractFirst(map, ChartSettingConditionCd.UNIT, fallback::unitCd))
//            .chartType(extractEnum(map, ChartSettingConditionCd.CHART_TYPE, fallback::chartType))
//            .compareGoodsIds(extractList(map, ChartSettingConditionCd.COMPARE_GOODS, fallback::compareGoodsIds))
//            .build();
//    }
//
//    /* ──────────────── 파싱 유틸 ──────────────── */
//
//    private <E extends Enum<E>> E extractEnum(Map<ChartSettingConditionCd, List<String>> map, ChartSettingConditionCd cd, Function<ChartRequest, E> fbSupplier) {
//        return map.containsKey(cd)
//            ? Enum.valueOf(fbSupplier.apply(null).getDeclaringClass(), map.get(cd).get(0))
//            : fbSupplier.apply(null);
//    }
//
//    private String extractFirst(Map<ChartSettingConditionCd, List<String>> map, ChartSettingConditionCd cd, Function<ChartRequest, String> fbSupplier) {
//        return map.getOrDefault(cd, List.of()).stream().findFirst().orElseGet(() -> fbSupplier.apply(null));
//    }
//
//    private List<Integer> extractList(Map<ChartSettingConditionCd, List<String>> map,
//                                      ChartSettingConditionCd cd, Function<ChartRequest, List<Integer>> fbSupplier) {
//        return map.containsKey(cd)
//            ? map.get(cd).stream().map(Integer::valueOf).toList()
//            : fbSupplier.apply(null);
//    }

//    /**
//     * 개인화 차트 설정을 Map 형태로 변환한다.
//     * 개인화 설정 ChartSettingConditionCd를 키로 하고 설정값 리스트를 값으로 하는 맵으로 변환한다.
//     * 설정이 없는 경우에도 모든 ChartSettingConditionCd에 대해 빈 리스트를 가진 맵을 반환한다.
//     *
//     * @param gds         데이터소스상품상세 객체
//     * @param userLoginId 로그인 아이디
//     * @return 개인화 차트 설정 Map (ChartSettingConditionCd -> List<String>)
//     */
//    public Map<ChartSettingConditionCd, List<String>> getConvertPersonalizedMap(GoodsDataSourceDetail gds, String userLoginId) {
//
//        if (StringUtils.isEmpty(userLoginId)) {
//            return createEmptyPersonalizedMap();
//        }
//
//        // 개인화 설정 조회
//        List<PersonalizedChartSetting> personalSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(gds, userLoginId);
//
//        // 기본 맵 생성 (모든 ChartSettingConditionCd에 대해 빈 리스트 초기화)
//        Map<ChartSettingConditionCd, List<String>> personalizedMap = createEmptyPersonalizedMap();
//
//        // 설정이 없는 경우 기본 맵 반환, ADMIN(타사접속가능), API_PLUS_OPERATOR Role로 접근 시, 기본 설정 값으로 반환 됨. -> 개인화 설정을 할 수 없음.
//        if (personalSettings == null || personalSettings.isEmpty()) {
//            return personalizedMap;
//        }
//
//        personalSettings.stream()
//            .filter(setting -> setting != null && setting.getChartSettingConditionCd() != null)
//            .collect(Collectors.groupingBy(PersonalizedChartSetting::getChartSettingConditionCd))
//            .forEach((key, valueList) -> {
//                List<String> values = valueList.stream().map(PersonalizedChartSetting::getChartSettingConditionVal).filter(Objects::nonNull).collect(Collectors.toList());
//                personalizedMap.put(key, values);
//            });
//
//        return personalizedMap;
//
//    }
//
//    /**
//     * 모든 ChartSettingConditionCd에 대해 빈 리스트를 가진 맵을 생성한다.
//     * -> 개인화 설정이 없는 경우에도 기본적으로 모든 설정을 default 설정으로 초기화하기 위해 사용된다.
//     *
//     * @return 빈 개인화 설정 맵
//     */
//    private Map<ChartSettingConditionCd, List<String>> createEmptyPersonalizedMap() {
//        Map<ChartSettingConditionCd, List<String>> emptyMap = new HashMap<>();
//        for (ChartSettingConditionCd cd : ChartSettingConditionCd.values()) {
//            emptyMap.put(cd, new ArrayList<>());
//        }
//        return emptyMap;
//    }
//
//    /**
//     * 개인화 설정에서 Range 값을 가져온다.
//     * 요청된 범위가 있으면 그 값을 우선 사용하고, 없는 경우 개인화 설정을 확인한다.
//     * 개인화 설정 맵에서 RANGE 키에 해당하는 값 목록의 첫 번째 값을 기준으로 Range enum을 찾는다.
//     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값(ONE_YEAR)을 반환한다.
//     *
//     * @param pMap         개인화 설정 Map
//     * @param reqRange     요청된 Range 값
//     * @param reqStartDate 사용자 지정 시작일 (YYYYMMDD 형식)
//     * @param reqEndDate   사용자 지정 종료일 (YYYYMMDD 형식)
//     * @return 시작일과 종료일 쌍 (Pair<String, String>)
//     */
//    public Pair<String, String> getPsRangeOrDefault(Map<ChartSettingConditionCd, List<String>> pMap, Range reqRange, LocalDate reqStartDate, LocalDate reqEndDate) {
//
//        // 1. 요청된 범위가 있으면 우선 사용
//        if (ObjectUtils.isNotEmpty(reqRange)) {
//            if (reqRange == Range.CUSTOM) {
//                if (ObjectUtils.isEmpty(reqStartDate) || ObjectUtils.isEmpty(reqEndDate)) {
//                    throw new IllegalArgumentException("The 'startDate' and 'endDate' are required for the [CUSTOM] range.");
//                }
//                return Pair.of(DateUtil.toYmd(reqStartDate), DateUtil.toYmd(reqEndDate));
//            }
//            return DateUtil.getDateRange(reqRange);
//        }
//
//        // 2. 개인화 설정이 없다면 default 값 사용
//        List<String> rangeValues = pMap.get(ChartSettingConditionCd.RANGE);
//        if (ObjectUtils.isEmpty(rangeValues)) {
//            return DateUtil.getDateRange(Range.ONE_YEAR);
//        }
//
//        // 3. 개인화 설정에 따른 범위 값 조회
//        String rangeValue = rangeValues.get(0);
//        return DateUtil.getDateRange(Range.findByCode(rangeValue));
//
//    }
//
//    /**
//     * 개인화 설정에서 Interval 값을 가져온다.
//     * 요청된 인터벌이 있으면 그 값을 우선 사용하고, 없는 경우 개인화 설정을 확인한다.
//     * 개인화 설정 맵에서 INTERVAL 키에 해당하는 값 목록의 첫 번째 값을 기준으로 Interval enum을 찾는다.
//     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값(ONE_DAY)을 반환한다.
//     *
//     * @param personalizedMap 개인화 설정 Map
//     * @param reqInterval     요청된 Interval 값
//     * @return 개인화된 Interval 값 또는 기본값
//     */
//    public Interval getPsIntervalOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap, Interval reqInterval) {
//
//        // 1. 요청된 인터벌이 있으면 우선 사용
//        if (ObjectUtils.isNotEmpty(reqInterval)) {
//            return reqInterval;
//        }
//
//        // 2. 개인화 설정이 없다면 default 인터벌 사용
//        List<String> intervalValues = personalizedMap.get(ChartSettingConditionCd.INTERVAL);
//        if (ObjectUtils.isEmpty(intervalValues)) {
//            return Interval.ONE_DAY;
//        }
//
//        // 3. 개인화 설정에 따른 인터벌 사용
//        String intervalValue = intervalValues.get(0);
//        return Interval.findByCode(intervalValue);
//    }
//
//    /**
//     * 개인화 설정에서 ChartType 값을 가져온다.
//     * 개인화 설정 맵에서 CHART 키에 해당하는 값 목록의 첫 번째 값을 기준으로 ChartType enum을 찾는다.
//     * 설정이 없거나 일치하는 enum을 찾지 못한 경우 기본값을 반환한다.
//     *
//     * @param personalizedMap 개인화 설정 Map
//     * @return 개인화된 ChartType 값 또는 기본값
//     */
//    public ChartType getPsChartTypeOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
//
//        List<String> chartTypeValues = personalizedMap.get(ChartSettingConditionCd.CHART);
//        if (ObjectUtils.isEmpty(chartTypeValues)) {
//            return ChartType.LINE;
//        }
//
//        String chartTypeValue = chartTypeValues.get(0);
//
//        return ChartType.valueOf(chartTypeValue);
//    }
//
//    /**
//     * 개인화 설정에서 Unit 값을 가져온다.
//     * 개인화 설정 맵에서 UNIT 키에 해당하는 값 목록의 첫 번째 값을 반환한다.
//     * 설정이 없는 경우 기본값을 반환한다.
//     *
//     * @param personalizedMap 개인화 설정 Map
//     * @param reqUnitCd       요청받은 Unit 값
//     * @param defaultUnitCd   기본 Unit 값(by. gds)
//     * @return 개인화된 Unit 값 또는 기본값
//     */
//    public String getPsUnitCdOrDefault(Map<ChartSettingConditionCd, List<String>> personalizedMap, String reqUnitCd, String defaultUnitCd) {
//
//        // 1. 요청된 단위가 있으면 우선 사용
//        if (ObjectUtils.isNotEmpty(reqUnitCd)) {
//            return reqUnitCd;
//        }
//
//        // 2. 개인화 설정이 없다면 기본값 사용
//        List<String> unitValues = personalizedMap.get(ChartSettingConditionCd.UNIT_CD);
//        if (ObjectUtils.isEmpty(unitValues)) {
//            return defaultUnitCd;
//        }
//
//        // 3. 개인화 설정에 따른 단위 사용
//        return unitValues.get(0);
//    }
//
//    /**
//     * 개인화 설정에서 INDICATOR 값 목록을 가져온다.
//     * 개인화 설정 맵에서 INDICATOR 키에 해당하는 값 목록을 반환한다.
//     * 설정이 없는 경우 빈 리스트를 반환한다.
//     *
//     * @param personalizedMap 개인화 설정 Map
//     * @return 개인화된 INDICATOR 값 목록
//     */
//    public List<String> getPersonalizedIndicators(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
//        // 유효성 검증
//        if (personalizedMap == null) {
//            return Collections.emptyList();
//        }
//
//        List<String> indicatorValues = personalizedMap.get(ChartSettingConditionCd.INDICATOR);
//        if (indicatorValues == null) {
//            return Collections.emptyList();
//        }
//
//        return indicatorValues;
//    }
//
//    /**
//     * 개인화 설정에서 COMP_DATA_SOURCE 값 목록을 가져온다.
//     * 개인화 설정 맵에서 COMP_DATA_SOURCE 키에 해당하는 값 목록을 정수로 변환하여 반환한다.
//     * 설정이 없거나 변환 중 오류가 발생한 경우 빈 리스트를 반환한다.
//     *
//     * @param personalizedMap 개인화 설정 Map
//     * @return 개인화된 COMP_DATA_SOURCE 값 목록
//     */
//    public List<Integer> getPersonalizedCompDataSources(Map<ChartSettingConditionCd, List<String>> personalizedMap) {
//
//        List<String> compDataSourceStrings = personalizedMap.get(ChartSettingConditionCd.COMP_DATA_SOURCE);
//        if (compDataSourceStrings == null || compDataSourceStrings.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // 문자열을 정수로 변환
//        return compDataSourceStrings.stream()
//            .filter(s -> s != null && !s.isEmpty())
//            .map(Integer::parseInt)
//            .collect(Collectors.toList());
//    }

}
