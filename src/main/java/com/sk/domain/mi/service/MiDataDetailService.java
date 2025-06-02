package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationTableRes;
import com.sk.common.model.TableRes;
import com.sk.common.utils.ChartSliceUtil;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelUtils;
import com.sk.common.utils.MDCUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.charts.*;
import com.sk.domain.mi.dto.response.detail.GoodsDataCategoryProjection;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataSourceRes;
import com.sk.domain.mi.dto.response.detail.UnstructuredDataRes;
import com.sk.domain.mi.service.chart.*;
import com.sk.domain.mi.service.chart.dto.ChartSettingsContext;
import com.sk.domain.mi.service.chart.dto.PersonalizeKey;
import com.sk.domain.mi.service.chart.dto.RawDataQueryContext;
import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataRepository goodsDataRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    private final MiGoodsPermissionService userService;
    private final ChartGenerationService chartGenerationService;
    private final PersonalizationService personalizationService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final ChartUnitConversionService chartUnitConversionService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final MiGoodsDataInquiryUserService goodsDataInquiryUserService;

    private final AuthorizationService authorizationService;


    /**
     * 상품 데이터 정보를 조회합니다.
     *
     * @param goodsNo     상품 번호
     * @param goodsDataNo 상품 데이터 번호
     * @return 상품 데이터 응답
     */
    @Transactional
    public GoodsDataRes getGoodsData(Integer goodsNo, Integer goodsDataNo) {

        GoodsData goodsData = getValidatedGoodsData(goodsDataNo, goodsNo);

        List<GoodsDataCategoryProjection> categoryProjections = goodsDataCategoryRepository.findGoodsDataAndIsDeleted(goodsData.getGoodsDataNo(), GoodsDataManagementSeparateCode.GDAT.getCode());

        PermissionRes permissionRes = switch (authorizationService.getCurrentRole()) {
            case USER -> userService.getUserGoodsPermissions(authorizationService.getUserNo());
            case ADMIN -> userService.getAccessAdminGoodsPermissions();
            case API_PLUS_OPERATOR -> userService.getPlusOperatorGoodsPermissions(authorizationService.getCompanyNo());
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        };

        // 상품-데이터 조회 이력 적재
        goodsDataInquiryUserService.createGoodsDataInquiryUser(goodsData, authorizationService.getSubject());

        return GoodsDataRes.of(goodsNo, goodsData, categoryProjections, permissionRes, authorizationService.getCompanyNo());
    }

    /**
     * 상품 데이터의 비정형 데이터를 조회합니다.
     *
     * @param goodsNo     상품 번호
     * @param goodsDataNo 상품 데이터 번호
     * @return 비정형 데이터 응답 목록
     */
    public List<UnstructuredDataRes> getGoodsDataUnstructuredData(Integer goodsNo, Integer goodsDataNo) {
        GoodsData goodsData = getValidatedGoodsData(goodsDataNo, goodsNo);
        List<UploadedFile> documents = uploadedFileRepository.findByFileRelationshipObjectDistinguishNoAndIsDeletedFalse(goodsData.getGoodsDataNo());

        PermissionRes permissionRes = switch (authorizationService.getCurrentRole()) {
            case USER -> userService.getUserGoodsPermissions(authorizationService.getUserNo());
            case ADMIN -> userService.getAccessAdminGoodsPermissions();
            case API_PLUS_OPERATOR -> userService.getPlusOperatorGoodsPermissions(authorizationService.getCompanyNo());
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        };

        int companyNo = ObjectUtils.isNotEmpty(MDCUtil.getValue(MDCUtil.REQUEST_X_COMPANY_NO)) ? Integer.parseInt(MDCUtil.getValue(MDCUtil.REQUEST_X_COMPANY_NO)) : authorizationService.getCompanyNo();
        Boolean goodsViewable = permissionRes.getGoodsViewableMap(companyNo).getOrDefault(goodsNo, Boolean.FALSE);

        return UnstructuredDataRes.ofList(documents, goodsViewable);
    }

    /**
     * 상품 데이터 소스 목록을 조회합니다.
     *
     * @param goodsNo     상품 번호
     * @param goodsDataNo 상품 데이터 번호
     * @return 상품 데이터 소스 응답 목록
     */
    public List<GoodsDataSourceRes> getGoodsDataSources(Integer goodsNo, Integer goodsDataNo) {
        GoodsData goodsData = getValidatedGoodsData(goodsDataNo, goodsNo);
        List<GoodsDataSourceDetail> goodsDataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);
        return GoodsDataSourceRes.ofList(goodsDataSources);
    }

    /**
     * 차트 개인화를 설정합니다.
     *
     * @param goodsNo     상품 번호
     * @param gdNo        상품 데이터 번호
     * @param gdsNo       상품 데이터소스 번호
     * @param rawRequests 차트 개인화 요청 데이터 리스트
     * @return 개인화된 차트 응답 데이터 리스트
     */
    @Transactional
    public List<ChartsPersonalizeRes> putGoodsDataSourcesChartsPersonalize(Integer goodsNo, Integer gdNo, Integer gdsNo, List<ChartsPersonalizeReq> rawRequests) {

        String userId = authorizationService.getSubject();

        // 기본 검증
        GoodsDataSourceDetail dataSource = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);

        // 요청 정제
        List<ChartsPersonalizeReq> requests = ChartsPersonalizeReq.filterPersonalizeRequests(rawRequests);
        if (requests.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "No valid parameter values in the array.");
        }

        // 사용자 개인화 현황 조회
        List<PersonalizedChartSetting> currentSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(dataSource, userId);


        Function<ChartsPersonalizeReq, PersonalizeKey> keyOfReq = r -> new PersonalizeKey(r.chartConditionCd(), r.chartConditionVal());
        Function<PersonalizedChartSetting, PersonalizeKey> keyOfEntity = e -> new PersonalizeKey(e.getChartSettingConditionCd(), e.getChartSettingConditionVal());

        Map<PersonalizeKey, PersonalizedChartSetting> currentMap = currentSettings.stream().collect(Collectors.toMap(keyOfEntity, Function.identity(), (v1, v2) -> v2));

        Set<PersonalizeKey> requestKeys = requests.stream()
            .map(keyOfReq)
            .collect(Collectors.toSet());

        Set<PersonalizeKey> keysToRemove = new HashSet<>(currentMap.keySet());
        keysToRemove.removeAll(requestKeys);

        Set<PersonalizeKey> keysToInsert = new HashSet<>(requestKeys);
        keysToInsert.removeAll(currentMap.keySet());

        // soft-delete
        keysToRemove.stream()
            .map(currentMap::get)
            .forEach(PersonalizedChartSetting::softDelete);

        // insert
        if (!keysToInsert.isEmpty()) {
            List<PersonalizedChartSetting> inserts = requests.stream()
                .filter(r -> keysToInsert.contains(keyOfReq.apply(r)))
                .map(r -> PersonalizedChartSetting.of(r, dataSource, userId))
                .toList();
            personalizedChartSettingRepository.saveAll(inserts);
        }

        return ChartsPersonalizeRes.ofList(requests);

    }

    public ChartMetadataSimple getGoodsDataSourceMetadata(Integer goodsNo, Integer gdNo, Integer gdsNo) {
        // 1. 데이터 소스 검증
        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        ChartSettingsContext simpleContext = personalizeAndDefault(gds, ChartSettingsContext.ofEmpty());
        Set<String> allowedUnitCd = chartUnitConversionService.getConvertUnitCdMap(gds).keySet().stream().map(UnitCd::toString).collect(Collectors.toSet());
        return ChartMetadataSimple.of(gds, simpleContext, allowedUnitCd);

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
    public ChartBundleRes getGoodsDataSourceCharts(Integer goodsNo, Integer gdNo, Integer gdsNo, Interval interval, Range range, LocalDate startDate, LocalDate endDate, String unitCd) {

        // 1. 데이터 소스 검증
        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);

        // 2. 요청 파라미터 1차 유효성 hard 검증 -> 검증 실패 시, 예외 처리
        ChartSettingsContext settingsContextV1 = hardValidateChartParameters(gds, interval, range, startDate, endDate, unitCd);

        // 3. 개인화 & 디폴트 설정 처리
        ChartSettingsContext settingsContextV2 = personalizeAndDefault(gds, settingsContextV1);

        // 4. 파라미터 2차 유효성 soft 검증 -> 검증 실패 시, 조정값 주입
        ChartSettingsContext settingsContextV3 = softValidateChartParametersAndAdjust(gds, settingsContextV2);

        // 5. 차트 데이터 생성
        ChartBundleRes bundle = generateChartBundle(gds, settingsContextV3);

        // 6. 차트 데이터 슬라이싱 및 반환
        return ChartSliceUtil.sliceBundle(bundle, DateUtil.parseDate(settingsContextV3.finalizedRangeBetween().getLeft()));

    }

    /**
     * 차트 요청 파라미터의 1차 유효성을 검증합니다.
     * 사용자 요청에 대한 값 검증을 수행합니다.
     *
     * @param gds       데이터소스 상세 정보
     * @param interval  인터벌 (요청 값)
     * @param range     범위 (요청 값)
     * @param startDate 시작일 (요청 값)
     * @param endDate   종료일 (요청 값)
     * @param unitCd    단위 변환 코드 (상세유형명#변환후단위명) (요청 값)
     * @return ChartSettingsContext (1차 검증된 차트 설정 컨텍스트)
     */
    private ChartSettingsContext hardValidateChartParameters(GoodsDataSourceDetail gds, Interval interval, Range range, LocalDate startDate, LocalDate endDate, String unitCd) {

        // 'Range' 검증
        if (range == Range.CUSTOM) {

            // Range가 CUSTOM인 경우, 시작일과 종료일이 모두 필요
            if (startDate == null || endDate == null) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "For [CUSTOM] 'range', both 'startDate' and 'endDate' must be provided.");
            }

            // 시작일과 종료일이 유효한 날짜인지 검증
            if (endDate.isBefore(startDate)) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "'endDate' (%s) must be on or after 'startDate' (%s).".formatted(DateUtil.toYmd(endDate), DateUtil.toYmd(startDate)));
            }

        }

        // 'Interval' 검증
        if (ObjectUtils.isNotEmpty(interval)) {

            // 인터벌과 기간단위코드 상관관계 검증
            // example: interval ONE_DAY(1D)라면? 데이터소스의 기간단위구분코드가 최소 'D(daily)' 여야 한다.
            if (!interval.getPeriodCd().isGreaterOrEqual(gds.getPeriodUnitSeparateCd())) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                    "Requested interval (%s - %s) is too granular. goods-data-source minimum period unit is %s (%s). Minimum allowed interval is %s.".formatted(
                        interval.getCode(),
                        interval.getDescription(),
                        gds.getPeriodUnitSeparateCd().getCode(),
                        gds.getPeriodUnitSeparateCd().getDescription(),
                        gds.getPeriodUnitSeparateCd().minInterval().getCode()
                    )
                );
            }

            // 범위와 인터벌 상관관계 검증
            if (!interval.isAllowedRange(range, startDate, endDate)) {
                String customRangeInfo = range == Range.CUSTOM ? " (from %s to %s)".formatted(DateUtil.toYmd(startDate), DateUtil.toYmd(endDate)) : "";
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Requested range (%s%s) is not compatible with the requested interval (%s).".formatted(
                        range.getCode(),
                        customRangeInfo,
                        interval.getCode()
                    )
                );
            }

        }

        UnitConvert unitConvert = ChartUnitConversionService.DEFAULT_IDENTITY;
        Map<UnitCd, UnitConvert> unitCdMap = chartUnitConversionService.getConvertUnitCdMap(gds);

        // 'UnitCd' 검증
        if (StringUtils.isNotEmpty(unitCd)) {

            UnitCd requestUnitCd = UnitCd.parse(unitCd);

            if (!unitCdMap.containsKey(requestUnitCd)) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                    "Requested unitCd cannot be applied to the current goods-data-source(no: %s). unitCd (%s)".formatted(gds.getGoodsDataSourceNo(), requestUnitCd.toString()));
            }

            unitConvert = unitCdMap.get(requestUnitCd);
        }

        // 1차 유효성 검증이 끝난 차트 설정 컨텍스트
        return ChartSettingsContext.builder()
            .requestedInterval(interval)
            .requestedRange(range)
            .requestedStartDateStr(startDate)
            .requestedEndDateStr(endDate)
            .requestedUnitCdStr(unitCd)
            .finalizedUnitConvert(unitConvert)
            .finalizedAllowedUnitCds(unitCdMap.keySet())
            .build();
    }

    /**
     * 차트 파라미터의 2차 유효성을 검증합니다.
     * 데이터소스의 최소 수집 단위에 맞춰 인터벌과 범위를 조정하고,
     * 개인화/기본값 적용 후 발생할 수 있는 Interval과 Range 간의 불일치를 해소합니다.
     * 검증 실패 시 예외 대신 값을 [보정]합니다.
     *
     * @param gds       데이터소스 상세 정보
     * @param contextV2 개인화 및 기본값이 적용된 파라미터 설정 컨텍스트 객체(v2)
     * @return ChartSettingsContext (2차 검증된 차트 설정 컨텍스트)
     */
    private ChartSettingsContext softValidateChartParametersAndAdjust(GoodsDataSourceDetail gds, ChartSettingsContext contextV2) {

        Interval initialInterval = contextV2.finalizedInterval();
        Range initialRange = contextV2.finalizedRange();
        Pair<String, String> initialPeriodPair = contextV2.finalizedRangeBetween();

        LocalDate initialStartDate = DateUtil.fromYmd(initialPeriodPair.getLeft());
        LocalDate initialEndDate = DateUtil.fromYmd(initialPeriodPair.getRight());

        PeriodUnitSeparateCd minDataSourceUnit = gds.getPeriodUnitSeparateCd();

        Interval currentInterval = initialInterval;
        Range currentRange = initialRange;
        LocalDate currentStartDate = initialStartDate;
        LocalDate currentEndDate = initialEndDate;

        // 단계 1: Interval이 데이터소스 최소 단위보다 작은 경우, 최소 단위 Interval로 조정
        if (!currentInterval.getPeriodCd().isGreaterOrEqual(minDataSourceUnit)) {
            Interval newInterval = minDataSourceUnit.minInterval();
            log.info("[Chart Parameter Soft-Validation]: Initial Interval ({}) is too granular for goods-data-source minimum unit ({}). Adjusted Interval to {}.", currentInterval.getCode(), minDataSourceUnit.getCode(), newInterval.getCode());
            currentInterval = newInterval;
        }

        // 단계 2: 조정된 Interval과 현재 Range/날짜의 호환성 검사
        // Interval.isAllowedRange는 Range가 CUSTOM일 때 startDate, endDate를 사용함.
        boolean isCompatible = currentInterval.isAllowedRange(currentRange, currentStartDate, currentEndDate);

        if (!isCompatible) {
            log.info("[Chart Parameter Soft-Validation]: Current Interval ({}) and Range ({}, Dates: {} to {}) are incompatible. Attempting further adjustments.",
                currentInterval.getCode(), currentRange.getCode(), DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate));

            // 시도 1: 현재 Range를 유지하고, 호환되는 가장 작은 Interval (데이터소스 최소단위 고려)로 재조정
            Interval candidateInterval = Interval.smallestAllowed(currentRange, minDataSourceUnit, currentStartDate, currentEndDate);

            if (candidateInterval != null) {
                log.info("[Chart Parameter Soft-Validation]: For current Range ({}, Dates: {} to {}), adjusting Interval from {} to {}.",
                    currentRange.getCode(), DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate), currentInterval.getCode(), candidateInterval.getCode());
                currentInterval = candidateInterval;
                isCompatible = currentInterval.isAllowedRange(currentRange, currentStartDate, currentEndDate); // 호환성 재확인
            } else {
                // 현재 Range와 날짜에 맞는 Interval이 없음. Range를 조정해야 함.
                log.info("[Chart Parameter Soft-Validation]: No suitable Interval found for current Range ({}, Dates: {} to {}) and data source unit ({}). Will adjust Range.",
                    currentRange.getCode(), DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate), minDataSourceUnit.getCode());
            }
        }

        // 단계 3 (단계 2에서 호환성을 찾지 못한 경우): Range를 ONE_YEAR로 기본 설정하고, 이에 맞는 Interval 찾기
        if (!isCompatible) {
            log.info("[Chart Parameter Soft-Validation]: Previous adjustments insufficient. Defaulting Range to 1Y(ONE_YEAR) and finding a compatible Interval.");

            Range oldRangeForLog = currentRange;
            LocalDate oldStartDateForLog = currentStartDate;
            LocalDate oldEndDateForLog = currentEndDate;

            currentRange = Range.ONE_YEAR;
            Pair<String, String> oneYearDates = DateUtil.getDateRange(Range.ONE_YEAR);
            currentStartDate = DateUtil.parseDate(oneYearDates.getLeft());
            currentEndDate = DateUtil.parseDate(oneYearDates.getRight());

            Interval candidateIntervalForOneYear = Interval.smallestAllowed(currentRange, minDataSourceUnit, currentStartDate, currentEndDate);

            if (candidateIntervalForOneYear != null) {
                log.info("[Chart Parameter Soft-Validation]: Adjusted Range to {} (Dates: {} to {}). Interval for this range set to {}. (Original Range was {}, Dates {} to {}, Initial Interval was {})",
                    currentRange.getCode(), DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate),
                    candidateIntervalForOneYear.getCode(),
                    oldRangeForLog.getCode(), DateUtil.toYmd(oldStartDateForLog), DateUtil.toYmd(oldEndDateForLog), initialInterval.getCode());
                currentInterval = candidateIntervalForOneYear;
            } else {
                // ONE_YEAR에도 맞는 Interval이 없다면, 데이터소스 최소 Interval로 강제
                Interval fallbackInterval = minDataSourceUnit.minInterval();
                log.warn("[Chart Parameter Soft-Validation]: No Interval found for Range.ONE_YEAR. Fallback to data source minimum Interval {}. (Original Range was {}, Dates {} to {}, Initial Interval was {})",
                    fallbackInterval.getCode(), oldRangeForLog.getCode(), DateUtil.toYmd(oldStartDateForLog), DateUtil.toYmd(oldEndDateForLog), initialInterval.getCode());
                currentInterval = fallbackInterval;
            }
        }

        // 로그: 최종적으로 값이 변경된 경우
        boolean intervalChanged = !initialInterval.equals(currentInterval);
        boolean rangeChanged = !initialRange.equals(currentRange);
        boolean datesChanged = !initialStartDate.equals(currentStartDate) || !initialEndDate.equals(currentEndDate);

        if (intervalChanged || rangeChanged || datesChanged) {
            log.info("[Chart Parameter Soft-Validation Summary]: Parameters adjusted. Initial: Range={}, Interval={}, Dates={}-{}. Final:   Range={}, Interval={}, Dates={}-{}.",
                initialRange.getCode(), initialInterval.getCode(), DateUtil.toYmd(initialStartDate), DateUtil.toYmd(initialEndDate),
                currentRange.getCode(), currentInterval.getCode(), DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate));
        }

        // 최종적으로 결정된 Range와 날짜로 finalizedRangeBetween 업데이트
        Pair<String, String> finalPeriodDatesPair;
        if (currentRange == Range.CUSTOM) {
            finalPeriodDatesPair = Pair.of(DateUtil.toYmd(currentStartDate), DateUtil.toYmd(currentEndDate));
        } else {
            finalPeriodDatesPair = DateUtil.getDateRange(currentRange);
        }

        return contextV2
            .withFinalizedInterval(currentInterval)
            .withFinalizedRange(currentRange)
            .withFinalizedStartDate(currentStartDate)
            .withFinalizedEndDate(currentEndDate)
            .withFinalizedRangeBetween(finalPeriodDatesPair);
    }


    /**
     * 차트 설정 컨텍스트를 결정합니다.
     * 개인화 설정과 요청 파라미터를 기반으로 차트 설정을 결정합니다.
     * <p>
     * [파라미터 우선 순위]
     * 1. 사용자 요청 값
     * 2. 개인화 조회 값
     * 3. 기본값
     *
     * @param gds       데이터소스 상세 정보
     * @param contextV1 파라미터 설정 컨텍스트 객체(v1)
     * @return 결정된 차트 설정 컨텍스트
     */
    private ChartSettingsContext personalizeAndDefault(GoodsDataSourceDetail gds, ChartSettingsContext contextV1) {

        PersonalizationService.Preferences resolve = personalizationService.resolve(gds, authorizationService.getCurrentRole(), authorizationService.getSubject());

        // Interval 결정: 요청값 > 개인화 > 기본값
        Interval finalizedInterval = Optional.ofNullable(contextV1.requestedInterval())
            .or(resolve::interval)
            .orElse(Interval.ONE_DAY);

        // Range 결정: 요청값 > 개인화 > 기본값
        Range finalizedRange = Optional.ofNullable(contextV1.requestedRange())
            .or(resolve::range)
            .orElse(Range.ONE_YEAR);

        // RangeBetween 결정: 요청값 > 개인화 > 기본값
        Pair<String, String> finalizedRangeBetween = Optional.ofNullable(contextV1.requestedRange())
            .map(r -> Range.toPair(r, contextV1.requestedStartDateStr(), contextV1.requestedEndDateStr()))
            .or(resolve::periodDates)
            .orElse(DateUtil.getDateRange(Range.ONE_YEAR));

        // UnitCd 결정: 요청값 > 개인화 > 기본값
        UnitCd finalizedUnitCd = Optional.ofNullable(contextV1.requestedUnitCdStr())
            .map(UnitCd::parse)
            .or(resolve::unitCd)
            .orElse(new UnitCd(gds.getConvertTargetDetailTypeNm(), gds.getGoodsDataSourceUnitNm()));

        // ChartType 결정: 개인화 > 기본값
        ChartType finalizedChartType = resolve.chartType()
            .orElse(ChartType.LINE);

        // Indicators 결정: 개인화
        List<String> finalizedIndicators = resolve.indicators();

        // ComparisonChartIds 결정: 개인화
        List<Integer> finalizedCompIds = resolve.compDataSourceIds();

        // settingsContextV1 -> settingsContextV2
        return contextV1
            .withFinalizedInterval(finalizedInterval)
            .withFinalizedRange(finalizedRange)
            .withFinalizedRangeBetween(finalizedRangeBetween)
            .withFinalizedUnitCd(finalizedUnitCd)
            .withFinalizedChartType(finalizedChartType)
            .withFinalizedIndicators(finalizedIndicators)
            .withFinalizedCompIds(finalizedCompIds);
    }


    /**
     * 차트 번들을 생성합니다.
     * 메타데이터, 메인 차트, 기술적 지표, 비교 차트를 생성하여 번들로 묶습니다.
     *
     * @param gds       데이터소스 상세 정보
     * @param contextV3 차트 설정 컨텍스트(v3)
     * @return 생성된 차트 번들
     */
    private ChartBundleRes generateChartBundle(GoodsDataSourceDetail gds, ChartSettingsContext contextV3) {

        LocalDate latestDate = chartGenerationService.getLatestDate(gds);

        // [metadata] 생성
        ChartMetadata metadata = ChartMetadata.of(gds, contextV3, DateUtil.toYmd(latestDate));

        // [mainChart] 생성
        Chart mainChart = chartGenerationService.generateChartData(
            gds,
            contextV3,
            Boolean.TRUE, // 메인 차트는 항상 visible 상태
            Boolean.TRUE
        );

        // [indicators] 생성
        List<Indicator> indicators = technicalIndicatorService.generateIndicators(mainChart, contextV3.finalizedIndicators());

        // [comparisonCharts] 생성
        List<Chart> comparisonCharts = chartGenerationService.generateComparisonCharts(gds, contextV3);

        return ChartBundleFactory.create(metadata, mainChart, indicators, comparisonCharts);
    }

    /**
     * 상품 데이터 소스의 원천 데이터를 조회합니다.
     *
     * @param goodsNo   상품 번호
     * @param gdNo      상품 데이터 번호
     * @param gdsNo     상품 데이터소스 번호
     * @param page      페이지 번호
     * @param size      페이지 크기
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 페이징된 원천 데이터 테이블 응답
     */
    public PaginationTableRes<TableRes.Row, TableRes.Column> getGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, Integer page, Integer size, LocalDate startDate, LocalDate endDate) {

        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        RawDataQueryContext queryContext = prepareRawDataQueryContext(gds, DateUtil.toYmd(startDate), DateUtil.toYmd(endDate));

        // Count 쿼리 실행
        List<Map<String, Object>> countQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofCountQuery(gds, queryContext.whereClause()));
        int totalElements = extractTotalElements(countQuery);

        // Paging 데이터 쿼리 실행
        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofPagingQuery(gds, queryContext.whereClause(), queryContext.dateColumn(), page, size));

        // 테이블 응답 생성
        TableRes table = buildTableResponse(dataQuery, queryContext);

        // 최종 페이징 응답 반환
        return PaginationTableRes.toRes(page, size, totalElements, table.rows(), table.headers());
    }

    /**
     * 상품 데이터 소스의 원천 데이터를 엑셀 파일로 다운로드합니다.
     *
     * @param goodsNo   상품 번호
     * @param gdNo      상품 데이터 번호
     * @param gdsNo     상품 데이터소스 번호
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 엑셀 파일 스트림
     */
    public ByteArrayInputStream downloadGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, LocalDate startDate, LocalDate endDate) {

        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        RawDataQueryContext queryContext = prepareRawDataQueryContext(gds, DateUtil.toYmd(startDate), DateUtil.toYmd(endDate));

        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(gds, queryContext.whereClause(), queryContext.dateColumn()));

        // 테이블 응답 생성
        TableRes table = buildTableResponse(dataQuery, queryContext);

        // 엑셀 파일 생성 및 반환
        return ExcelUtils.toExcel(gds.getGoodsDataSourceNm(), table.headers(), table.rows());
    }

    /**
     * GoodsData를 조회하고 유효성을 검사합니다.
     *
     * @param gdNo    상품 데이터 번호
     * @param goodsNo 상품 번호
     * @return 유효한 GoodsData 객체
     * @throws AppErrorException 지정된 상품 데이터가 존재하지 않거나 삭제된 경우
     */
    private GoodsData getValidatedGoodsData(Integer gdNo, Integer goodsNo) {
        return goodsDataRepository.findByGoodsDataNoAndGoods_GoodsNoAndIsDeletedFalse(gdNo, goodsNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "It doesn't exist in the [goods-data]. goodsDataNo: %s, goodsNo: %s".formatted(gdNo, goodsNo)));
    }

    /**
     * GoodsDataSourceDetail을 조회하고 유효성을 검사합니다.
     *
     * @param gdsNo   상품 데이터소스 번호
     * @param gdNo    상품 데이터 번호
     * @param goodsNo 상품 번호
     * @return 유효한 GoodsDataSourceDetail 객체
     * @throws AppErrorException 지정된 상품 데이터소스가 존재하지 않거나 삭제된 경우
     */
    private GoodsDataSourceDetail getValidatedGoodsDataSource(Integer gdsNo, Integer gdNo, Integer goodsNo) {
        return goodsDataSourceDetailRepository.findGoodsDataSourceWithGoodsTree(gdsNo, gdNo, goodsNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "It doesn't exist in the [goods-data-source]. goodsDataSource: %s, goodsDataNo: %s, goodsNo: %s".formatted(gdsNo, gdNo, goodsNo)));
    }

    /**
     * 쿼리에 필요한 컨텍스트 정보 (컬럼 표현식, 날짜 컬럼, WHERE 절)를 준비합니다.
     *
     * @param gds       상품 데이터 소스 상세 정보
     * @param startDate 조회 시작일 (YYYYMMDD)
     * @param endDate   조회 종료일 (YYYYMMDD)
     * @return RawDataQueryContext 객체
     */
    private RawDataQueryContext prepareRawDataQueryContext(GoodsDataSourceDetail gds, String startDate, String endDate) {
        Map<String, String> columnExpressions = getColumnExpressions(gds);

        // 컬럼 목록 파싱 및 날짜 컬럼 결정 (null 또는 빈 문자열 처리 추가 고려)
        List<String> columns = Optional.ofNullable(gds.getTableColumnListNm())
            .map(s -> Arrays.stream(s.split(",")).map(String::trim).toList())
            .orElse(Collections.emptyList());

        if (columns.isEmpty()) {
            // 적절한 예외 처리 또는 기본값 설정
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Table column list is not defined for goodsDataSourceNo: %s".formatted(gds.getGoodsDataSourceNo()));
        }

        String dateColumn = columns.get(0);
        String whereClause = "%s between '%s' and '%s'".formatted(dateColumn, startDate, endDate);

        return new RawDataQueryContext(columns, columnExpressions, dateColumn, whereClause);
    }

    /**
     * 조회된 데이터와 컬럼 표현식을 기반으로 TableRes 객체를 생성합니다.
     *
     * @param dataQuery    Mybatis 동적 쿼리 결과 데이터 목록
     * @param queryContext 원천 데이터 쿼리 컨텍스트
     * @return TableRes 객체
     */
    private TableRes buildTableResponse(List<Map<String, Object>> dataQuery, RawDataQueryContext queryContext) {

        if (dataQuery == null || dataQuery.isEmpty()) {
            return TableRes.of(Collections.emptyList(), Collections.emptyList());
        }

        // 필드명 추출 (select 절 순서 보장)
        List<String> fields = new ArrayList<>(queryContext.selectClause());

        // Headers 생성
        List<TableRes.Column> headers = fields.stream()
            .map(field -> TableRes.Column.of(field, queryContext.columnExpressions().getOrDefault(field, field)))
            .toList();

        // Rows 생성
        List<TableRes.Row> rows = dataQuery.stream()
            .map(rowMap -> TableRes.Row.of(
                fields.stream()
                    .map(key -> Optional.ofNullable(rowMap.get(key)).map(Object::toString).orElse(null))
                    .toList()))
            .toList();

        return TableRes.of(headers, rows);
    }

    /**
     * Count 쿼리 결과에서 총 개수를 추출합니다.
     *
     * @param countQueryResult Count 쿼리 결과 목록
     * @return 총 개수
     */
    private int extractTotalElements(List<Map<String, Object>> countQueryResult) {
        return countQueryResult.stream()
            .findFirst()
            .map(m -> m.get("count(*)")) // 키 이름 확인 필요
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .map(Number::intValue)
            .orElse(0);
    }

    /**
     * 상품 데이터 소스의 컬럼 표현식을 조회합니다.
     *
     * @param goodsDataSourceDetail 상품 데이터 소스 상세 정보
     * @return 컬럼명과 컬럼 표현식 맵
     */
    private Map<String, String> getColumnExpressions(GoodsDataSourceDetail goodsDataSourceDetail) {
        // null 체크 강화
        if (goodsDataSourceDetail == null ||
            goodsDataSourceDetail.getGoodsData() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods().getProvider() == null ||
            goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns() == null) {
            return Collections.emptyMap();
        }

        return goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns()
            .stream()
            // ProviderDataColumn 및 ID null 체크 추가
            .filter(c -> c != null && c.getId() != null && c.getId().getProviderDataColumnNm() != null && c.getDataColumnExpn() != null)
            .collect(Collectors.toMap(
                c -> c.getId().getProviderDataColumnNm(),
                ProviderDataColumn::getDataColumnExpn,
                (existing, replacement) -> existing // 중복 키 발생 시 기존 값 유지
            ));
    }

}
