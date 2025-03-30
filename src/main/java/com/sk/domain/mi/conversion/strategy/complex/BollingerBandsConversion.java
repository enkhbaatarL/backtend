package com.sk.domain.mi.conversion.strategy.complex;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.FormulaService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.strategy.ResultWrapperConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion.strategy.basic.StandardDeviationConversion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 볼린저 밴드 계산을 위한 복합 전략 클래스 (위임 방식 사용)
 */
@Component
public class BollingerBandsConversion implements ResultWrapperConversionStrategy<BollingerBandResult> {

    private static final BigDecimal DEFAULT_K = new BigDecimal("2");

    private final MovingAverageConversion movingAverageConversion;
    private final StandardDeviationConversion standardDeviationConversion;

    public BollingerBandsConversion(MovingAverageConversion movingAverageConversion, StandardDeviationConversion standardDeviationConversion) {
        this.movingAverageConversion = movingAverageConversion;
        this.standardDeviationConversion = standardDeviationConversion;
    }

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.BOLLINGER_BANDS;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "BOLLINGER_BANDS requires a time series data");
    }

    @Override
    public List<BollingerBandResult> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<BollingerBandResult> resultType) {

        if (timeSeriesData == null) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
        }

        if (!BollingerBandResult.class.isAssignableFrom(resultType)) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Only BOLLINGER_BANDS resultType is supported");
        }

        List<BigDecimal> maList = movingAverageConversion.convertTimeSeries(timeSeriesData, windowSize);
        List<BigDecimal> sdList = standardDeviationConversion.convertTimeSeries(timeSeriesData, windowSize);

        List<BollingerBandResult> results = new ArrayList<>(timeSeriesData.size());
        for (int i = 0; i < timeSeriesData.size(); i++) {
            BigDecimal ma = maList.get(i);
            BigDecimal sd = sdList.get(i);

            if (ma != null && sd != null) {
                BigDecimal upper = ma.add(sd.multiply(DEFAULT_K, MathUtil.MC), MathUtil.MC);
                BigDecimal lower = ma.subtract(sd.multiply(DEFAULT_K, MathUtil.MC), MathUtil.MC);
                results.add(BollingerBandResult.of(ma, upper, lower));
            } else {
                results.add(null);
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

