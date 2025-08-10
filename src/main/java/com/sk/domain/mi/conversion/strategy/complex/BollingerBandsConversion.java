package com.sk.domain.mi.conversion.strategy.complex;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.FormulaService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.strategy.ResultWrapperConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.FixedStandardDeviationConversion;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 볼린저 밴드 계산을 위한 복합 전략 클래스
 * - 이동평균은 MovingAverageConversion을 사용
 * - 고정 표준편차는 FixedStandardDeviationConversion(최근 최대 1년치) 적용
 */
@Component
public class BollingerBandsConversion implements ResultWrapperConversionStrategy<BollingerBandResult> {

    private static final BigDecimal DEFAULT_K = new BigDecimal("2");

    private final MovingAverageConversion movingAverageConversion;
    private final FixedStandardDeviationConversion fixedStandardDeviationConversion;

    public BollingerBandsConversion(MovingAverageConversion movingAverageConversion, FixedStandardDeviationConversion fixedStandardDeviationConversion) {
        this.movingAverageConversion = movingAverageConversion;
        this.fixedStandardDeviationConversion = fixedStandardDeviationConversion;
    }

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.BOLLINGER_BANDS;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "BOLLINGER_BANDS requires a time series data");
    }

    /**
     * 시계열 데이터 기반으로 볼린저 밴드 상단/하단 값을 생성한다.
     * 표준편차는 최근 최대 1년치 데이터를 기준으로 고정 계산된다.
     *
     * @param timeSeriesData 전체 시계열 데이터
     * @param windowSize     이동평균 윈도우 크기
     * @param resultType     결과 타입 (BollingerBandResult)
     */
    @Override
    public List<BollingerBandResult> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<BollingerBandResult> resultType) {

        if (timeSeriesData == null) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
        }

        if (!BollingerBandResult.class.isAssignableFrom(resultType)) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Only BOLLINGER_BANDS resultType is supported");
        }

        // 이동평균 계산
        List<BigDecimal> maList = movingAverageConversion.convertTimeSeries(timeSeriesData, windowSize);
        // 고정 표준편차 계산 (최근 1년치)
        List<BigDecimal> sdList = fixedStandardDeviationConversion.convertTimeSeries(timeSeriesData, windowSize);

        List<BollingerBandResult> results = new ArrayList<>(timeSeriesData.size());
        for (int i = 0; i < timeSeriesData.size(); i++) {
            BigDecimal ma = maList.get(i);
            BigDecimal sd = sdList.get(i);

            if (ma != null && sd != null) {
                BigDecimal upper = MathUtil.add(ma, MathUtil.multiply(sd, DEFAULT_K));
                BigDecimal lower = MathUtil.subtract(ma, MathUtil.multiply(sd, DEFAULT_K));
                results.add(BollingerBandResult.of(ma, upper, lower));
            } else {
                results.add(BollingerBandResult.ofEmpty());
            }
        }
        return results;
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

    @Override
    public Class<BollingerBandResult> resultType() {
        return BollingerBandResult.class;
    }

}

