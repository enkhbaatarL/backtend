package com.sk.domain.mi.conversion;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 단위 변환 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final UnitConversionContext unitConversionContext;
    private final UnitConvertRepository unitConvertRepository;

    /**
     * 단위 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param value            변환시킬 값
     * @return BigDecimal
     */
    public BigDecimal convert(Integer unitConversionNo, BigDecimal value) {

        UnitConvert unitConvert = findActiveUnitConvert(unitConversionNo);

        if (StringUtils.isNotEmpty(unitConvert.getCalculateFormulaServiceNm())) {
            FormulaService formulaService = FormulaService.findByFormulaServiceNm(unitConvert.getCalculateFormulaServiceNm());
            // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
            return unitConversionContext.convert(formulaService, value);
        }

        // 단순 변환 계수 사용 - DB에 저장된 단위 변환
        return value.multiply(unitConvert.getConvertCoef());
    }

    /**
     * 시계열데이터 변환
     *
     * @param unitConvert 단위변환객체
     * @param timeSeriesValues 변환시킬 시계열데이터
     * @return List<BigDecimal>
     */
    public List<BigDecimal> convertTimeSeries(UnitConvert unitConvert, List<BigDecimal> timeSeriesValues) {

        if (StringUtils.isNotEmpty(unitConvert.getCalculateFormulaServiceNm())) {
            FormulaService formulaService = FormulaService.findByFormulaServiceNm(unitConvert.getCalculateFormulaServiceNm());
            // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
            return unitConversionContext.convertTimeSeries(formulaService, timeSeriesValues, 20);
        }

        // 단순 변환 계수 사용 - DB에 저장된 단위 변환
        return timeSeriesValues.stream()
            .map(value -> MathUtil.safeMultiply(value, unitConvert.getConvertCoef()))
            .toList();
    }

    /**
     * 시계열데이터 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param timeSeriesValues 변환시킬 시계열데이터
     * @return List<BigDecimal>
     */
    public List<BigDecimal> convertTimeSeries(Integer unitConversionNo, List<BigDecimal> timeSeriesValues) {

        UnitConvert unitConvert = findActiveUnitConvert(unitConversionNo);

        if (StringUtils.isNotEmpty(unitConvert.getCalculateFormulaServiceNm())) {
            FormulaService formulaService = FormulaService.findByFormulaServiceNm(unitConvert.getCalculateFormulaServiceNm());
            // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
            return unitConversionContext.convertTimeSeries(formulaService, timeSeriesValues, 20);
        }

        // 단순 변환 계수 사용 - DB에 저장된 단위 변환
        return timeSeriesValues.stream()
            .map(value -> MathUtil.safeMultiply(value, unitConvert.getConvertCoef()))
            .toList();
    }

    /**
     * 시계열데이터 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param timeSeriesValues 변환시킬 시계열데이터
     * @param windowSize       윈도우사이즈
     * @return List<BigDecimal>
     */
    public List<BigDecimal> convertTimeSeries(Integer unitConversionNo, List<BigDecimal> timeSeriesValues, int windowSize) {

        UnitConvert unitConvert = findActiveUnitConvert(unitConversionNo);

        if (StringUtils.isNotEmpty(unitConvert.getCalculateFormulaServiceNm())) {
            FormulaService formulaService = FormulaService.findByFormulaServiceNm(unitConvert.getCalculateFormulaServiceNm());
            // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
            return unitConversionContext.convertTimeSeries(formulaService, timeSeriesValues, windowSize);
        }

        // 단순 변환 계수 사용 - DB에 저장된 단위 변환
        return timeSeriesValues.stream()
            .map(value -> MathUtil.safeMultiply(value, unitConvert.getConvertCoef()))
            .toList();
    }

    /**
     * 시계열데이터 변환
     *
     * @param timeSeriesValues 변환시킬 시계열데이터
     * @param windowSize       윈도우사이즈
     * @return List<BigDecimal>
     */
    public List<BigDecimal> convertTimeSeriesToMovingAverage(List<BigDecimal> timeSeriesValues, int windowSize) {
        return unitConversionContext.convertTimeSeries(FormulaService.MOVING_AVERAGE, timeSeriesValues, windowSize);
    }


    /**
     * 특정 결과 타입을 반환하는 시계열 데이터 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param timeSeriesValues 변환시킬 시계열데이터
     * @param windowSize       윈도우사이즈
     * @return List<T>
     */
    public List<? extends ConversionResult> convertTimeSeriesResult(Integer unitConversionNo, List<BigDecimal> timeSeriesValues, int windowSize) {

        UnitConvert unitConvert = findActiveUnitConvert(unitConversionNo);

        if (StringUtils.isNotEmpty(unitConvert.getCalculateFormulaServiceNm())) {
            FormulaService formulaService = FormulaService.findByFormulaServiceNm(unitConvert.getCalculateFormulaServiceNm());
            // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
            return unitConversionContext.convertTimeSeriesResult(formulaService, timeSeriesValues, windowSize);
        }

        throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
            "Basic coefficient-based conversions do not support result types.. unitConversionNo: %s".formatted(unitConversionNo));

    }

    public List<BollingerBandResult> convertTimeSeriesToBollingerBands(List<BigDecimal> timeSeriesValues, int windowSize) {
        return unitConversionContext.convertTimeSeriesResult(FormulaService.BOLLINGER_BANDS, timeSeriesValues, windowSize);
    }

    // todo 캐싱
    private UnitConvert findActiveUnitConvert(Integer unitConversionNo) {
        return unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [currentUnitCd-conversion]. unitConversionNo: %s".formatted(unitConversionNo)));
    }

}
