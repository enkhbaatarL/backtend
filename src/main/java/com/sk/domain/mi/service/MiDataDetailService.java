package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationTableRes;
import com.sk.common.model.TableRes;
import com.sk.common.utils.ChartSliceUtil;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelUtils;
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
import org.apache.commons.lang3.BooleanUtils;
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
        return UnstructuredDataRes.ofList(documents);
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

        Map<PersonalizeKey, PersonalizedChartSetting> currentMap = currentSettings.stream().collect(Collectors.toMap(keyOfEntity, Function.identity()));

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
        // 1. 데이터 소스 검증 및 유효성 검사
        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        validateChartParameters(gds, interval, range, startDate, endDate);

        // 2. 단위 변환 및 개인화 설정 처리
        ChartSettingsContext settingsContext = finalizedChartSettings(gds, interval, range, startDate, endDate, unitCd);

        // 3. 차트 데이터 생성
        ChartBundleRes bundle = generateChartBundle(gds, settingsContext);

        // 4. 차트 데이터 슬라이싱 및 반환
        return ChartSliceUtil.sliceBundle(bundle, DateUtil.parseDate(settingsContext.finalizedRange().getLeft()));
    }

    /**
     * 차트 파라미터의 유효성을 검증합니다.
     *
     * @param gds       데이터소스 상세 정보
     * @param interval  인터벌
     * @param range     범위
     * @param startDate 시작일
     * @param endDate   종료일
     */
    private void validateChartParameters(GoodsDataSourceDetail gds, Interval interval, Range range, LocalDate startDate, LocalDate endDate) {

        if (interval == null || range == null) {
            return;
        }

        // 인터벌이 데이터소스의 최소 인터벌보다 작은지 검증
        if (BooleanUtils.isFalse(interval.getPeriodUnitSeparateCd().isGreaterOrEqual(gds.getPeriodUnitSeparateCd()))) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                "The 'interval' is greater than the minInterval of the goods-data-source. interval: %s, minInterval: %s".formatted(interval, gds.getPeriodUnitSeparateCd()));
        }

        // 인터벌에 대해 범위가 유효한지 검증
        if (BooleanUtils.isFalse(interval.isAllowedRange(range, startDate, endDate))) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                "The 'range' is not allowed in the 'interval'. interval: %s, range: %s".formatted(interval, range));
        }

    }

    /**
     * 차트 설정 컨텍스트를 결정합니다.
     * 개인화 설정과 요청 파라미터를 기반으로 차트 설정을 결정합니다.
     *
     * @param gds       데이터소스 상세 정보
     * @param interval  인터벌
     * @param range     범위
     * @param startDate 시작일
     * @param endDate   종료일
     * @param unitCd    단위 코드
     * @return 결정된 차트 설정 컨텍스트
     */
    private ChartSettingsContext finalizedChartSettings(GoodsDataSourceDetail gds, Interval interval, Range range, LocalDate startDate, LocalDate endDate, String unitCd) {

        // 변환 가능한 단위 맵 조회 (GDS 기본 단위 포함)
        Map<UnitCd, UnitConvert> unitCdMap = chartUnitConversionService.getConvertUnitCdMap(gds);

        PersonalizationService.Preferences resolve = personalizationService.resolve(gds, authorizationService.getCurrentRole(), authorizationService.getSubject());

        Interval finalizedInterval = Optional.ofNullable(interval) // req
            .or(resolve::interval) // personalize
            .orElse(Interval.ONE_DAY); // default

        Pair<String, String> finalizedRange = Optional.ofNullable(range) // req
            .map(r -> toPair(r, startDate, endDate)) // req
            .or(resolve::periodDates) // personalize
            .or(() -> resolve.range().map(DateUtil::getDateRange)) // personalize
            .orElse(DateUtil.getDateRange(Range.ONE_YEAR)); // default

        UnitCd finalizedUnitCd = Optional.ofNullable(unitCd)
            .map(UnitCd::parse)
            .or(resolve::unitCd)
            .orElse(new UnitCd(gds.getConvertTargetDetailTypeNm(), gds.getGoodsDataSourceUnitNm()));

        ChartType finalizedChartType = resolve.chartType()
            .orElse(ChartType.LINE);

        List<String> finalizedIndicators = resolve.indicators();

        List<Integer> finalizedCompIds = resolve.compDataSourceIds();

        // 결정된 단위코드 유효성 검증
        chartUnitConversionService.validateUnitCd(unitCdMap, finalizedUnitCd);

        return new ChartSettingsContext(
            unitCdMap,
            finalizedInterval,
            finalizedRange,
            finalizedUnitCd,
            finalizedChartType,
            finalizedIndicators,
            finalizedCompIds
        );
    }

    private static Pair<String, String> toPair(Range range, LocalDate from, LocalDate to) {
        if (range == Range.CUSTOM) {
            if (from == null || to == null) {
                throw new IllegalArgumentException("CUSTOM 범위엔 startDate·endDate 둘 다 필요해!");
            }
            return Pair.of(DateUtil.toYmd(from), DateUtil.toYmd(to));
        }
        return DateUtil.getDateRange(range);
    }

    /**
     * 차트 번들을 생성합니다.
     * 메타데이터, 메인 차트, 기술적 지표, 비교 차트를 생성하여 번들로 묶습니다.
     *
     * @param gds             데이터소스 상세 정보
     * @param settingsContext 차트 설정 컨텍스트
     * @return 생성된 차트 번들
     */
    private ChartBundleRes generateChartBundle(GoodsDataSourceDetail gds, ChartSettingsContext settingsContext) {

        // [metadata] 생성
        ChartMetadata metadata = ChartMetadata.of(
            gds.getGoodsDataSourceCurrencyUnitNm(),
            settingsContext.finalizedChartType(),
            settingsContext.finalizedUnitCd(),
            settingsContext.unitCdMap().keySet(),
            settingsContext.finalizedInterval()
        );

        // [mainChart] 생성
        Chart mainChart = chartGenerationService.generateChartData(
            gds,
            settingsContext.finalizedRange(),
            settingsContext.finalizedInterval(),
            settingsContext.unitCdMap().get(settingsContext.finalizedUnitCd()),
            Boolean.TRUE, // 메인 차트는 항상 visible 상태
            Boolean.TRUE
        );

        // [indicators] 생성
        List<Indicator> indicators = technicalIndicatorService.generateIndicators(
            mainChart,
            settingsContext.finalizedIndicators()
        );

        // [comparisonCharts] 생성
        List<Chart> comparisonCharts = chartGenerationService.generateComparisonCharts(
            gds,
            settingsContext.finalizedRange(),
            settingsContext.finalizedInterval(),
            settingsContext.finalizedCompIds()
        );

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
    public PaginationTableRes<TableRes.Row, TableRes.Column> getGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, Integer page, Integer size, String startDate, String endDate) {

        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        RawDataQueryContext queryContext = prepareRawDataQueryContext(gds, startDate, endDate);

        // Count 쿼리 실행
        List<Map<String, Object>> countQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofCountQuery(gds, queryContext.whereClause()));
        int totalElements = extractTotalElements(countQuery);

        // Paging 데이터 쿼리 실행
        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofPagingQuery(gds, queryContext.whereClause(), queryContext.dateColumn(), page, size));

        // 테이블 응답 생성
        TableRes table = buildTableResponse(dataQuery, queryContext.columnExpressions());

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
    public ByteArrayInputStream downloadGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, String startDate, String endDate) {

        GoodsDataSourceDetail gds = getValidatedGoodsDataSource(gdsNo, gdNo, goodsNo);
        RawDataQueryContext queryContext = prepareRawDataQueryContext(gds, startDate, endDate);

        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(gds, queryContext.whereClause(), queryContext.dateColumn()));

        // 테이블 응답 생성
        TableRes table = buildTableResponse(dataQuery, queryContext.columnExpressions());

        // 엑셀 파일 생성 및 반환
        return ExcelUtils.toExcel(gds.getGoodsDataSourceNm(), table.headers(), table.rows());
    }

    /**
     * GoodsData를 조회하고 유효성을 검사합니다.
     */
    private GoodsData getValidatedGoodsData(Integer gdNo, Integer goodsNo) {
        return goodsDataRepository.findByGoodsDataNoAndGoods_GoodsNoAndIsDeletedFalse(gdNo, goodsNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "It doesn't exist in the [goods-data]. goodsDataNo: %s, goodsNo: %s".formatted(gdNo, goodsNo)));
    }

    /**
     * GoodsDataSourceDetail을 조회하고 유효성을 검사합니다.
     */
    private GoodsDataSourceDetail getValidatedGoodsDataSource(Integer gdsNo, Integer gdNo, Integer goodsNo) {
        return goodsDataSourceDetailRepository.findGoodsDataSourceWithGoodsTree(gdsNo, gdNo, goodsNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.REQ_INVALID, "It doesn't exist in the [goods-data-source]. goodsDataSource: %s, goodsDataNo: %s, goodsNo: %s".formatted(gdsNo, gdNo, goodsNo)));
    }

    /**
     * 쿼리에 필요한 컨텍스트 정보 (컬럼 표현식, 날짜 컬럼, WHERE 절)를 준비합니다.
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

        return new RawDataQueryContext(columnExpressions, dateColumn, whereClause);
    }

    /**
     * 조회된 데이터와 컬럼 표현식을 기반으로 TableRes 객체를 생성합니다.
     */
    private TableRes buildTableResponse(List<Map<String, Object>> dataQuery, Map<String, String> columnExpressions) {

        if (dataQuery == null || dataQuery.isEmpty()) {
            return TableRes.of(Collections.emptyList(), Collections.emptyList());
        }

        // 필드명 추출 (첫 번째 데이터 기준)
        List<String> fields = new ArrayList<>(dataQuery.get(0).keySet());

        // Headers 생성
        List<TableRes.Column> headers = fields.stream()
            .map(field -> TableRes.Column.of(field, columnExpressions.getOrDefault(field, field)))
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
     * 상품 데이터 소스의 컬럼 표현식을 조회합니다. (기존 메소드 유지)
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
