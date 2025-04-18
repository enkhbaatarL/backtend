package com.sk.domain.mi.service;

import com.sk.common.model.ValidationResult;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.ChartBundleFactory;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.mi.validator.GoodsDataValidator;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.enums.Interval;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    private final GoodsDataValidator goodsDataValidator;
    private final GoodsDataSourceValidator goodsDataSourceDetailValidator;

    private final UnitConversionService unitConversionService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final DataSourceGroupMappingRepository dataSourceGroupMappingRepository;
    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;


//    /**
//     * 상품 데이터를 상세 조회한다.
//     *
//     * @param goodsDataNo 상품 데이터 기본
//     * @return
//     */
//    public GoodsDataDetailRes getGoodsData(Integer goodsDataNo) {
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//        return null;
//    }
//
//    /**
//     * 데이터소스 목록을 조회한다.
//     *
//     * @param goodsDataNo 상품 데이터 번호
//     * @return
//     */
//    public List<GoodsDataSourceRes> getGoodsDataSources(Integer goodsDataNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//
//        List<GoodsDataSourceDetail> dataSources = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(goodsData);
//
//        return GoodsDataSourceRes.ofList(dataSources);
//
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @return
//     */
//    public List<ChartsPersonalizeRes> getChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        result.validOrThrow();
//
//        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
//            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));
//
//        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");
//
//        return ChartsPersonalizeRes.ofList(personalizedChartSettings);
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @return
//     */
//    public List<UnitConversionRes> getGoodsDataSourcesUnitConversion(Integer goodsDataNo, Integer goodsDataSourceNo) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        GoodsData goodsData = result.getOrThrow();
//
//        return null;
//
//    }
//
//    /**
//     *
//     * @param goodsDataNo
//     * @param goodsDataSourceNo
//     * @param chartsPersonalizeReqs
//     * @return
//     */
//    @Transactional
//    public List<ChartsPersonalizeRes> putChartsPersonalize(Integer goodsDataNo, Integer goodsDataSourceNo, List<ChartsPersonalizeReq> chartsPersonalizeReqs) {
//
//        ValidationResult<GoodsData> result = goodsDataValidator.ensureExists(goodsDataNo);
//        result.validOrThrow();
//
//        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo)
//            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. dataSourceNo: %s".formatted(goodsDataSourceNo)));
//
//        List<PersonalizedChartSetting> personalizedChartSettings = personalizedChartSettingRepository.findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(goodsDataSourceDetail, "");
//        Map<ChartSettingConditionCd, PersonalizedChartSetting> personalSettingMap = personalizedChartSettings.stream().collect(Collectors.toMap(PersonalizedChartSetting::getChartSettingConditionCd, Function.identity()));
//
//        chartsPersonalizeReqs.forEach(req -> {
//
//            if (personalSettingMap.containsKey(req.chartSettingConditionCd())) {
//
//                PersonalizedChartSetting personalizedChartSetting = personalSettingMap.get(req.chartSettingConditionCd());
//
//                if (ObjectUtils.isEmpty(req.chartConditionValue())) {
//                    personalizedChartSetting.softDelete(); // 값이 null이면 soft delete
//                } else {
//                    personalizedChartSetting.update(req); // 값이 null이 아니면 update
//                }
//
//            } else {
//
//                PersonalizedChartSetting setting = PersonalizedChartSetting.of(req, goodsDataSourceDetail);// 새로운 key의 값이면 insert
//
//            }
//
//        });
//
//
//        return null; // todo
//    }

    /**
     * 상품 데이터소스 차트 묶음을 조회한다.
     *
     * @param goodsDataNo 상품 데이터 번호
     * @param goodsDataSourceNo 상품 데이터소스 번호
     * @param interval 인터벌
     * @param startDate 범위 시작일
     * @param endDate 범위 종료일
     * @return ChartBundleRes
     */
    public ChartBundleRes getDynamicCharts(Integer goodsDataNo, Integer goodsDataSourceNo, Interval interval, String startDate, String endDate) {

        // todo 개인화 추가...
        // todo 단위변경 추가...

        // 상품데이터 검증
        ValidationResult<GoodsData> goodsDataResult = goodsDataValidator.ensureExists(goodsDataNo);
        GoodsData goodsData = goodsDataResult.getOrThrow();

        // 컬럼 정보 Map 생성
        Map<String, String> dataColumnsMap = goodsData.getGoods().getProvider().getProviderDataColumns()
            .stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

        // 상품 데이터소스 검증 및 '메인 차트' 메타 정보 조회
        ValidationResult<GoodsDataSourceDetail> goodsDataSourceResult = goodsDataSourceDetailValidator.ensureExists(goodsDataSourceNo);
        GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceResult.getOrThrow();

        // 상품 데이터소스 '메인 차트' 데이터 조회

        // hint 1. date 컬럼명
        String[] cols = goodsDataSourceDetail.getTableColumnListNm().split(",");
        String dateColumnName = cols[0];
        // hint 2. 차트 유형 (캔들 형 or 매트릭 형)
        Boolean isCandleChartPossible = goodsDataSourceDetail.getIsCandleChartPossible();

        List<Map<String, Object>> goodsDataSourceCharts = getRows(goodsDataSourceDetail, dateColumnName, startDate, endDate);
        List<Map<String, Object>> grouped = aggregateByInterval(goodsDataSourceCharts, interval, dateColumnName, cols, isCandleChartPossible);

        // 데이터 변경
        ChartData chartData = ChartBundleFactory.convertToChartData(goodsDataSourceDetail.getIsCandleChartPossible(), grouped, goodsDataSourceDetail.getTableColumnListNm(), dataColumnsMap);

        // 보조지표를 위한 주요 메트릭 시계열 데이터 추출 ('close' 또는 'main')
        List<BigDecimal> majorMetricSeries = ChartBundleFactory.extractMajorMetricSeries(chartData);

        List<BigDecimal> ma5 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 5);
        List<BigDecimal> ma20 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 20);
        List<BigDecimal> ma60 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 60);
        List<BigDecimal> ma120 = unitConversionService.convertTimeSeriesToMovingAverage(majorMetricSeries, 120);

        // 이동평균선(MA) 지표 데이터 생성
        IndicatorData maIndicator = ChartBundleFactory.createMovingAverageIndicator(chartData, ma5, ma20, ma60, ma120);

        List<BollingerBandResult> bollingerBandResults = unitConversionService.convertTimeSeriesToBollingerBands(majorMetricSeries, 20);

        // 볼린저 밴드(BB) 지표 데이터 생성
        IndicatorData bbIndicator = ChartBundleFactory.createBollingerBandsIndicator(chartData, bollingerBandResults);

        // 지표 데이터 목록 생성
        List<IndicatorData> indicators = List.of(maIndicator, bbIndicator);

        List<ChartData> compareDynamicChartRes = new ArrayList<>();

        // 상품 데이터소스 '비교군' 메타 정보 조회
        List<DataSourceGroupMapping> compareDataSources = dataSourceGroupMappingRepository.findByMainGoodsDataSourceDetailAndIsDeletedFalse(goodsDataSourceDetail);

        compareDataSources.forEach(compareDataSource -> {
            GoodsDataSourceDetail compareGoodsDataSourceDetail = compareDataSource.getCompareGoodsDataSourceDetail();

            Map<String, String> compareDataColumnsMap = compareGoodsDataSourceDetail.getGoodsData().getGoods().getProvider().getProviderDataColumns()
                .stream().collect(Collectors.toMap(c -> c.getId().getProviderDataColumnNm(), ProviderDataColumn::getDataColumnExpn));

            String[] compareCols = compareGoodsDataSourceDetail.getTableColumnListNm().split(",");
            String compareDateColumnName = compareCols[0];
            Boolean isCompareCandleChartPossible = compareGoodsDataSourceDetail.getIsCandleChartPossible();

            List<Map<String, Object>> cds = getRows(compareDataSource.getCompareGoodsDataSourceDetail(), compareDateColumnName, startDate, endDate);
            List<Map<String, Object>> groupedCds = aggregateByInterval(cds, interval, compareDateColumnName, compareCols, isCompareCandleChartPossible);

            compareDynamicChartRes.add(ChartBundleFactory.convertToChartData(compareGoodsDataSourceDetail.getIsCandleChartPossible(), groupedCds, compareGoodsDataSourceDetail.getTableColumnListNm(), compareDataColumnsMap));
        });

        return new ChartBundleRes(
            null,
            chartData,
            indicators,
            compareDynamicChartRes
        );

    }

    private List<Map<String, Object>> aggregateByInterval(List<Map<String, Object>> rawRows, Interval interval, String dateColName, String[] columnNames, boolean isCandleChart) {

        // 0) ONE_DAY → 원본 그대로 리턴
        if (interval == Interval.ONE_DAY) {
            return rawRows.stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    for (String col : columnNames) {
                        String c = col.trim();
                        m.put(c, row.get(c));
                    }
                    return m;
                })
                .collect(Collectors.toList());
        }

        // 1) 날짜 기준 오름차순 정렬
        rawRows.sort(Comparator.comparing(row -> DateUtil.parseDate(row.get(dateColName))));

        // 2) 전체 날짜 리스트, 시작/끝 구하기
        List<LocalDate> allDates = rawRows.stream()
            .map(r -> DateUtil.parseDate(r.get(dateColName)))
            .collect(Collectors.toList());
        LocalDate firstDate = allDates.get(0);
        LocalDate lastDate = allDates.get(allDates.size() - 1);

        // 3) bucket 시작 경계 계산 (끝 → 점점 과거)
        List<LocalDate> bucketStarts = new ArrayList<>();
        bucketStarts.add(lastDate);
        LocalDate cursor = lastDate;
        while (true) {
            LocalDate next = switch (interval) {
                case ONE_WEEK -> cursor.minusWeeks(1);
                case TWO_WEEK -> cursor.minusWeeks(2);
                case ONE_MONTH -> cursor.minusMonths(1);
                case THREE_MONTH -> cursor.minusMonths(3);
                case SIX_MONTH -> cursor.minusMonths(6);
                case ONE_YEAR -> cursor.minusYears(1);
                default -> cursor;
            };
            if (next.isBefore(firstDate)) break;
            bucketStarts.add(next);
            cursor = next;
        }

        // 4) 각 버킷마다 필터링 & 집계
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < bucketStarts.size() - 1; i++) {
            LocalDate startBound = bucketStarts.get(i + 1);
            LocalDate endBound = bucketStarts.get(i);

            // 이 구간에 속한 rawRows
            List<Map<String, Object>> bucket = rawRows.stream()
                .filter(row -> {
                    LocalDate d = DateUtil.parseDate(row.get(dateColName));
                    return (!d.isBefore(startBound) && !d.isAfter(endBound));
                })
                .collect(Collectors.toList());
            if (bucket.isEmpty()) continue;

            if (isCandleChart) {
                // 캔들형: cols[1]=close, [2]=open, [3]=high, [4]=low
                result.add(candleAggregate(
                    bucket,
                    dateColName,
                    columnNames[2].trim(), // open
                    columnNames[1].trim(), // close
                    columnNames[3].trim(), // high
                    columnNames[4].trim()  // low
                ));
            } else {
                // 매트릭형: cols[1]=main, cols[2..]=refs
                String mainCol = columnNames[1].trim();
                List<String> refCols = Arrays.stream(columnNames)
                    .skip(2)
                    .map(String::trim)
                    .collect(Collectors.toList());
                result.add(metricAggregate(bucket, dateColName, mainCol, refCols));
            }
        }

        return result;
    }

    private Map<String, Object> candleAggregate(List<Map<String, Object>> bucket, String dateCol, String openCol, String closeCol, String highCol, String lowCol) {
        LocalDate startDate = DateUtil.parseDate(bucket.get(0).get(dateCol));
        LocalDate endDate   = DateUtil.parseDate(bucket.get(bucket.size() - 1).get(dateCol));

        BigDecimal open  = toBigDecimal(bucket.get(0).get(openCol));
        BigDecimal close = toBigDecimal(bucket.get(bucket.size() - 1).get(closeCol));
        BigDecimal high  = bucket.stream().map(r -> toBigDecimal(r.get(highCol))).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal low   = bucket.stream().map(r -> toBigDecimal(r.get(lowCol))).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(dateCol, DateUtil.toYmdHyphen(startDate));
        m.put(openCol, open);
        m.put(closeCol, close);
        m.put(highCol, high);
        m.put(lowCol, low);
        return m;
    }

    private Map<String, Object> metricAggregate(List<Map<String, Object>> bucket, String dateCol, String mainCol, List<String> refCols) {
        LocalDate startDate = DateUtil.parseDate(bucket.get(0).get(dateCol));

        double mainAvg = bucket.stream()
            .mapToDouble(r -> toBigDecimal(r.get(mainCol)).doubleValue())
            .average().orElse(0d);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(dateCol, DateUtil.toYmdHyphen(startDate));
        m.put(mainCol, BigDecimal.valueOf(mainAvg));

        for (String ref : refCols) {
            double avg = bucket.stream()
                .mapToDouble(r -> toBigDecimal(r.get(ref)).doubleValue())
                .average().orElse(0d);
            m.put(ref, BigDecimal.valueOf(avg));
        }
        return m;
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) {
            return bd;
        } else if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        } else {
            return new BigDecimal(v.toString());
        }
    }

    @Transactional(transactionManager = "myBatisTransactionManager")
    public List<Map<String, Object>> getRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate, String endDate) {
        String additionalWhere = "%s between '%s' and '%s'".formatted(dateColumnName, startDate, endDate);
        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, additionalWhere));
    }

}
