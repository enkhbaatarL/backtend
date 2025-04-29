package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationTableRes;
import com.sk.common.model.TableRes;
import com.sk.common.model.ValidationResult;
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
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    private final MiUserService userService;
    private final ChartGenerationService chartGenerationService;
    private final PersonalizationService personalizationService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final MiGoodsDataInqueryUserService goodsDataInquiryUserService;

    private final ChartUnitConversionService chartUnitConversionService;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    public GoodsDataRes getGoodsData(Integer goodsNo, Integer goodsDataNo, Authentication authentication) {

        Role role = (authentication instanceof JwtAuthenticationToken jwtToken) ? Role.valueOf(jwtToken.getToken().getClaimAsString("role"))
                : Role.SYSTEM;

        ValidationResult<GoodsData> goodsDataResult = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataResult.getOrThrow();

        List<GoodsDataCategoryProjection> categoryProjections = goodsDataCategoryRepository.findGoodsDataAndIsDeleted(goodsData.getGoodsDataNo(),
                GoodsDataManagementSeparateCode.GDAT.getCode());

        PermissionRes permissionRes = switch (role) {
            case USER -> {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                Integer userNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.USER_NO.getCode()));
                yield userService.getUserGoodsPermissions(userNo);
            }
            case ADMIN -> userService.getAdminGoodsPermissions();
            case API_PLUS_OPERATOR -> {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                Integer companyNo = Integer.parseInt(jwtToken.getToken().getClaimAsString(Claim.COMPANY_NO.getCode()));
                yield userService.getPlusOperatorGoodsPermissions(companyNo);
            }
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        };

        // 상품-데이터 조회 이력
        goodsDataInquiryUserService.createGoodsDataInquiryUser(goodsData, authentication.getName());

        return GoodsDataRes.of(goodsNo, goodsData, categoryProjections, permissionRes);
    }

    public List<UnstructuredDataRes> getGoodsDataUnstructuredData(Integer goodsNo, Integer goodsDataNo) {
        List<UploadedFile> documents = uploadedFileRepository.findByFileRelationshipObjectDistinguishNoAndIsDeletedFalse(goodsDataNo);
        return UnstructuredDataRes.ofList(documents);
    }

    public List<GoodsDataSourceRes> getGoodsDataSources(Integer goodsNo, Integer goodsDataNo) {

        ValidationResult<GoodsData> goodsDataResult = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataResult.getOrThrow();

        List<GoodsDataSourceDetail> goodsDataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);

        return GoodsDataSourceRes.ofList(goodsDataSources);

    }

    /**
     * 차트 개인화를 설정합니다.
     *
     * @param gdNo                  상품 데이터 번호
     * @param gdsNo                 상품 데이터소스 번호
     * @param chartsPersonalizeReqs 차트 개인화 요청 데이터 리스트
     * @return 개인화된 차트 응답 데이터 리스트
     */
    @Transactional
    public List<ChartsPersonalizeRes> putGoodsDataSourcesChartsPersonalize(Integer gdNo, Integer gdsNo,
            List<ChartsPersonalizeReq> chartsPersonalizeReqs, String subject) {

        // 1) 유효성 검증 & 필터링
        GoodsDataSourceDetail gds = goodsDataSourceDetailValidator
                .ensureExists(gdsNo)
                .getOrThrow();

        List<ChartsPersonalizeReq> validReqs = chartsPersonalizeReqs.stream()
                .map(ChartsPersonalizeReq::validThen)
                .filter(Objects::nonNull)
                .toList();

        // 2) 기존 DB 설정을 key → 엔티티 맵으로 변환
        List<PersonalizedChartSetting> existing = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(gds,
                subject);

        Function<ChartsPersonalizeReq, String> keyOfReq = req -> req.chartConditionCd().name() + ":" + req.chartConditionVal();
        Function<PersonalizedChartSetting, String> keyOfEntity = ent -> ent.getChartSettingConditionCd().name() + ":"
                + ent.getChartSettingConditionVal();

        Map<String, PersonalizedChartSetting> existingMap = existing.stream()
                .collect(Collectors.toMap(
                        keyOfEntity,
                        Function.identity(),
                        (v1, v2) -> v2));

        Set<String> incomingKeys = validReqs.stream()
                .map(keyOfReq)
                .collect(Collectors.toSet());

        // 삭제 대상: 기존 키 중에 incomingKeys에 없는 것
        existingMap.entrySet().stream()
                .filter(e -> !incomingKeys.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .forEach(BaseEntity::softDelete);

        // 삽입 대상: incomingKeys 중에 existingMap에 없는 것
        List<PersonalizedChartSetting> toInsert = validReqs.stream()
                .filter(req -> !existingMap.containsKey(keyOfReq.apply(req)))
                .map(req -> PersonalizedChartSetting.of(req, gds, subject))
                .toList();

        if (!toInsert.isEmpty()) {
            personalizedChartSettingRepository.saveAll(toInsert);
        }

        return ChartsPersonalizeRes.ofList(validReqs);

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
    public ChartBundleRes getGoodsDataSourceCharts(Integer goodsNo, Integer gdNo, Integer gdsNo, Interval interval, Range range, String startDate,
            String endDate, String unitCd, String userLoginId) {

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
        String determinedUnitCd = personalizationService.getPsUnitCdOrDefault(personalizedMap, unitCd,
                ChartUnitConversionService.generateDefaultUnitCd(gds));
        ChartType determinedChartType = personalizationService.getPsChartTypeOrDefault(personalizedMap);

        // 결정된 단위코드 유효성 검증
        chartUnitConversionService.validateUnitCd(unitCdMap, determinedUnitCd);

        // 메타데이터 생성
        ChartMetadata metadata = ChartMetadata.of(gds.getGoodsDataSourceCurrencyUnitNm(), determinedChartType, determinedUnitCd, unitCdMap.keySet(),
                determinedInterval);

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
                personalizedMap);

        // 비교 차트 생성
        List<Chart> comparisonCharts = chartGenerationService.generateComparisonCharts(
                gds,
                determinedRange.getLeft(),
                determinedRange.getRight(),
                determinedInterval,
                personalizedMap);

        return ChartBundleFactory.create(metadata, mainChart, indicators, comparisonCharts);
    }

    public PaginationTableRes<TableRes.Row, TableRes.Column> getGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, Integer page,
            Integer size, String startDate, String endDate) {

        // 상품-데이터 유효성 검증
        goodsDataValidator.ensureExists(gdNo).validOrThrow();

        GoodsDataSourceDetail gds = goodsDataSourceDetailValidator.ensureExists(gdsNo).getOrThrow();

        // 컬럼 정보 추출
        Map<String, String> columnExpressions = getColumnExpressions(gds);

        return getRowsWithPagination(gds, page, size, columnExpressions, startDate, endDate);
    }

    public ByteArrayInputStream downloadGoodsDataSourcesRawData(Integer goodsNo, Integer gdNo, Integer gdsNo, String startDate, String endDate) {

        // 상품-데이터 유효성 검증
        goodsDataValidator.ensureExists(gdNo).validOrThrow();

        GoodsDataSourceDetail gds = goodsDataSourceDetailValidator.ensureExists(gdsNo).getOrThrow();

        // 컬럼 정보 추출
        Map<String, String> columnExpressions = getColumnExpressions(gds);

        List<String> columns = Arrays.stream(gds.getTableColumnListNm().split(",")).map(String::trim).toList();
        String whereClause = "%s between '%s' and '%s'".formatted(columns.get(0), startDate, endDate);

        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(gds, whereClause));

        List<String> fields = dataQuery.isEmpty() ? List.of() : new ArrayList<>(dataQuery.get(0).keySet());

        // headers 생성
        List<TableRes.Column> headers = fields.stream()
                .map(f -> TableRes.Column.of(f, columnExpressions.getOrDefault(f, f)))
                .toList();

        // rows 생성
        List<TableRes.Row> rows = dataQuery.stream()
                .map(rowMap -> TableRes.Row.of(
                        fields.stream()
                                .map(key -> Optional.ofNullable(rowMap.get(key))
                                        .map(Object::toString)
                                        .orElse(null))
                                .toList()))
                .toList();

        return ExcelUtils.toExcel(gds.getGoodsDataSourceNm(), headers, rows);
    }

    @Transactional(transactionManager = "myBatisTransactionManager")
    public PaginationTableRes<TableRes.Row, TableRes.Column> getRowsWithPagination(GoodsDataSourceDetail gds, Integer page, Integer size,
            Map<String, String> columnExpressions, String startDate, String endDate) {

        List<String> columns = Arrays.stream(gds.getTableColumnListNm().split(",")).map(String::trim).toList();

        String whereClause = "%s between '%s' and '%s'".formatted(columns.get(0), startDate, endDate);

        List<Map<String, Object>> countQuery = mybatisDynamicQueryService.getDynamicData(DynamicDataReq.ofCountQuery(gds, whereClause));
        List<Map<String, Object>> dataQuery = mybatisDynamicQueryService
                .getDynamicData(DynamicDataReq.ofPagingQuery(gds, whereClause, columns.get(0), page, size));

        int totalElements = countQuery.stream()
                .findFirst()
                .map(m -> ((Number) m.get("count(*)")).intValue())
                .orElse(0);

        List<String> fields = dataQuery.isEmpty() ? List.of() : new ArrayList<>(dataQuery.get(0).keySet());

        // headers 생성
        List<TableRes.Column> headers = fields.stream()
                .map(f -> TableRes.Column.of(f, columnExpressions.getOrDefault(f, f)))
                .toList();

        // rows 생성
        List<TableRes.Row> rows = dataQuery.stream()
                .map(rowMap -> TableRes.Row.of(
                        fields.stream()
                                .map(key -> Optional.ofNullable(rowMap.get(key))
                                        .map(Object::toString)
                                        .orElse(null))
                                .toList()))
                .toList();

        TableRes table = TableRes.of(headers, rows);
        return PaginationTableRes.toRes(page, size, totalElements, table.rows(), table.headers());
    }

    private Map<String, String> getColumnExpressions(GoodsDataSourceDetail goodsDataSourceDetail) {
        if (goodsDataSourceDetail == null ||
                goodsDataSourceDetail.getGoodsData() == null ||
                goodsDataSourceDetail.getGoodsData().getGoods() == null ||
                goodsDataSourceDetail.getGoodsData().getGoods().getProvider() == null ||
                goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns() == null) {
            return Collections.emptyMap();
        }

        return goodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns()
                .stream()
                .filter(c -> c != null && c.getId() != null)
                .collect(Collectors.toMap(
                        c -> c.getId().getProviderDataColumnNm(),
                        ProviderDataColumn::getDataColumnExpn,
                        // In case of duplicate keys, keep the first one
                        (first, second) -> first));
    }

}
