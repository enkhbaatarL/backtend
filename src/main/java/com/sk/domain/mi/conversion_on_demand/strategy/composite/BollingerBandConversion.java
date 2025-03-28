package com.sk.domain.mi.conversion_on_demand.strategy.composite;

import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.mi.conversion_on_demand.dto.BollingerBandResult;
import com.sk.domain.mi.conversion_on_demand.strategy.*;
import com.sk.domain.mi.conversion_on_demand.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion_on_demand.strategy.basic.StandardDeviationConversion;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BollingerBandConversion implements ConversionStrategy {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private final BigDecimal k; // 보통 2
    private final ComposableConversionStrategy maStrategy;
    private final ComposableConversionStrategy sdStrategy;

    public BollingerBandConversion(BigDecimal k) {
        this.k = k;
        this.maStrategy = new MovingAverageConversion();
        this.sdStrategy = new StandardDeviationConversion();
    }

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.BOLLINGER_BANDS;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw new UnsupportedOperationException("Bollinger Band requires time series data");
    }

    @Override
    public <T> List<T> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<T> resultType) {

        if (!resultType.equals(BollingerBandResult.class)) {
            throw new IllegalArgumentException("Only BollingerBand resultType is supported");
        }

        // 합성함수 방식
        List<BigDecimal> maList = maStrategy.convertTimeSeries(timeSeriesData, windowSize);
        List<BigDecimal> sdList = sdStrategy.convertTimeSeries(timeSeriesData, windowSize);

        List<T> results = new ArrayList<>(timeSeriesData.size());
        for (int i = 0; i < timeSeriesData.size(); i++) {
            BigDecimal ma = maList.get(i);
            BigDecimal sd = sdList.get(i);

            if (ma != null && sd != null) {
                BigDecimal upper = ma.add(sd.multiply(k, MC), MC);
                BigDecimal lower = ma.subtract(sd.multiply(k, MC), MC);
                results.add(resultType.cast(BollingerBandResult.of(ma, upper, lower)));
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

}
