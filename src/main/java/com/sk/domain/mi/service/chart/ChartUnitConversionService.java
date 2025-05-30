package com.sk.domain.mi.service.chart;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.repository.master.tmd.GoodsDataUnitConvertRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChartUnitConversionService {

    // GDS 기본 단위 유지를 위한 객체
    public static final UnitConvert DEFAULT_IDENTITY;

    static {
        DEFAULT_IDENTITY = UnitConvert.ofDefault(BigDecimal.ONE);
    }

    private final UnitConversionService unitConversionService;
    private final GoodsDataUnitConvertRepository goodsDataUnitConvertRepository;

    /**
     * 상품-데이터-소스에 대한 단위 변환 맵을 조회한다.
     * 상품-데이터-소스 번호를 기준으로 단위 변환 목록을 조회하고, 변환 가능한 단위 맵을 생성한다.
     *
     * @param gds 데이터소스 상세 정보
     * @return 단위 변환 맵 (key: 중분류#단위, value: 단위변환기본 객체)
     */
    public Map<UnitCd, UnitConvert> getConvertUnitCdMap(GoodsDataSourceDetail gds) {

        // 단위 변환 목록 조회
        List<GoodsDataUnitConvert> unitConversions = goodsDataUnitConvertRepository.findByGoodsDataSourceDetailAndIsDeletedFalse(gds);

        Map<UnitCd, UnitConvert> unitCdMap = unitConversions.stream().collect(
            Collectors.toMap(uc -> new UnitCd(uc.getUnitConvert().getConvertTargetDetailTypeNm(), uc.getUnitConvert().getConvertAfterUnitNm()), GoodsDataUnitConvert::getUnitConvert, (v1, v2) -> v2)
        );

        unitCdMap.put(new UnitCd(gds.getConvertTargetDetailTypeNm(), gds.getGoodsDataSourceUnitNm()), DEFAULT_IDENTITY);

        return unitCdMap;

    }

    /**
     * 단위 변환을 수행하고, 변환된 값을 포함한 새로운 데이터 리스트를 반환한다.
     *
     * @param isCandleChartPossible 캔들 차트 여부
     * @param aggregatedData        집계된 원시 데이터
     * @param targetColumns         컬럼명 리스트 (date 컬럼이 0번)
     * @param volumeColumn          거래량 컬럼명
     * @param unitConvert           단위 변환 정보
     * @return 변환된 값이 반영된 데이터 리스트
     */
    public List<Map<String, Object>> convert(boolean isCandleChartPossible, List<Map<String, Object>> aggregatedData, List<String> targetColumns, String volumeColumn, UnitConvert unitConvert) {

        if (ObjectUtils.isEmpty(aggregatedData) || unitConvert == ChartUnitConversionService.DEFAULT_IDENTITY) {
            return aggregatedData;
        }

        String dateCol = targetColumns.get(RenderAs.DATE.getIndex());

        if (isCandleChartPossible) {
            return convertCandleChart(aggregatedData, targetColumns, unitConvert, dateCol, volumeColumn);
        } else {
            return convertLineChart(aggregatedData, targetColumns, unitConvert, dateCol, volumeColumn);
        }
    }

    /**
     * 캔들 차트 데이터에 대한 단위 변환을 수행한다.
     * open, high, low, close 값에 대해 단위 변환을 적용한다.
     *
     * @param aggregatedData 집계된 원시 데이터
     * @param targetColumns  컬럼명 리스트
     * @param unitConvert    단위 변환 정보
     * @param dateCol        날짜 컬럼명
     * @param volumeColumn   거래량 컬럼명
     * @return 변환된 캔들 차트 데이터
     */
    private List<Map<String, Object>> convertCandleChart(List<Map<String, Object>> aggregatedData, List<String> targetColumns, UnitConvert unitConvert, String dateCol, String volumeColumn) {
        String closeCol = targetColumns.get(RenderAs.CLOSE.getIndex());
        String openCol = targetColumns.get(RenderAs.OPEN.getIndex());
        String highCol = targetColumns.get(RenderAs.HIGH.getIndex());
        String lowCol = targetColumns.get(RenderAs.LOW.getIndex());

        // 시계열 데이터 추출
        List<BigDecimal> closeSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(closeCol)))
            .toList();
        List<BigDecimal> openSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(openCol)))
            .toList();
        List<BigDecimal> highSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(highCol)))
            .toList();
        List<BigDecimal> lowSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(lowCol)))
            .toList();

        // 단위 변환 적용
        List<BigDecimal> convertedClose = unitConversionService.convertTimeSeries(unitConvert, closeSeries);
        List<BigDecimal> convertedOpen = unitConversionService.convertTimeSeries(unitConvert, openSeries);
        List<BigDecimal> convertedHigh = unitConversionService.convertTimeSeries(unitConvert, highSeries);
        List<BigDecimal> convertedLow = unitConversionService.convertTimeSeries(unitConvert, lowSeries);

        // 단위 변환 미적용(거래량)
        List<BigDecimal> volumeSeries = null;
        if (StringUtils.isNotEmpty(volumeColumn)) {
            volumeSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimalOrNull(r.get(volumeColumn)))
                .toList();
        }

        // 결과 생성
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < aggregatedData.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(dateCol, aggregatedData.get(i).get(dateCol));
            row.put(openCol, convertedOpen.get(i));
            row.put(highCol, convertedHigh.get(i));
            row.put(lowCol, convertedLow.get(i));
            row.put(closeCol, convertedClose.get(i));
            if (CollectionUtils.isNotEmpty(volumeSeries)) {
                row.put(volumeColumn, volumeSeries.get(i));
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 메트릭 차트 데이터에 대한 단위 변환을 수행한다.
     * 메인 값에 대해 단위 변환을 적용하고, 참조 값은 그대로 유지한다.
     *
     * @param aggregatedData 집계된 원시 데이터
     * @param targetColumns  컬럼명 리스트
     * @param unitConvert    단위 변환 정보
     * @param dateCol        날짜 컬럼명
     * @param volumeColumn   거래량 컬럼명
     * @return 변환된 메트릭 차트 데이터
     */
    private List<Map<String, Object>> convertLineChart(List<Map<String, Object>> aggregatedData, List<String> targetColumns, UnitConvert unitConvert, String dateCol, String volumeColumn) {
        String mainCol = targetColumns.get(RenderAs.MAIN.getIndex());

        // 메인 시계열 데이터 추출 및 변환
        List<BigDecimal> mainSeries = aggregatedData.stream()
            .map(r -> MathUtil.toBigDecimalOrNull(r.get(mainCol)))
            .toList();
        List<BigDecimal> convertedMain = unitConversionService.convertTimeSeries(unitConvert, mainSeries);

        // 단위 변환 미적용(거래량)
        List<BigDecimal> volumeSeries = null;
        if (StringUtils.isNotEmpty(volumeColumn)) {
            volumeSeries = aggregatedData.stream()
                .map(r -> MathUtil.toBigDecimalOrNull(r.get(volumeColumn)))
                .toList();
        }

        // 결과 생성
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < aggregatedData.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(dateCol, aggregatedData.get(i).get(dateCol));
            row.put(mainCol, convertedMain.get(i));
            if (CollectionUtils.isNotEmpty(volumeSeries)) {
                row.put(volumeColumn, volumeSeries.get(i));
            }
            // 참조 컬럼은 원본 값 그대로 복사
            for (int j = RenderAs.REFERENCE.getIndex(); j < targetColumns.size(); j++) {
                String col = targetColumns.get(j);
                row.put(col, aggregatedData.get(i).get(col));
            }
            result.add(row);
        }
        return result;
    }

    public static String generateDefaultUnitCd(GoodsDataSourceDetail goodsDataSourceDetail) {
        return StringUtil.joinPoundSignNonNull(goodsDataSourceDetail.getConvertTargetDetailTypeNm(), goodsDataSourceDetail.getGoodsDataSourceUnitNm());
    }

}
