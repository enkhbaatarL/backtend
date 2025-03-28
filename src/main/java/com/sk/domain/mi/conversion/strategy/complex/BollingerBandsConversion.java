package com.sk.domain.mi.conversion.strategy.complex;

import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion.strategy.basic.StandardDeviationConversion;
import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.mi.conversion_on_demand.dto.BollingerBandResult;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 볼린저 밴드 계산을 위한 복합 전략 클래스 (위임 방식 사용)
 */
public class BollingerBandsConversion implements ConversionStrategy {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private final BigDecimal k; // 보통 2
    /**
     * -- GETTER --
     *  위임된 전략 목록 반환
     */
    @Getter
    private final List<ConversionStrategy> delegateStrategies;
    private final ConversionStrategy maStrategy;
    private final ConversionStrategy sdStrategy;

    public BollingerBandsConversion(BigDecimal k) {
        this.k = k;
        this.maStrategy = new MovingAverageConversion();
        this.sdStrategy = new StandardDeviationConversion();
        this.delegateStrategies = List.of(maStrategy, sdStrategy);
    }

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.BOLLINGER_BANDS;
    }


    @Override
    public BigDecimal convert(BigDecimal value) {
        throw new UnsupportedOperationException("Bollinger Band requires time series data");
    }

    /**
     * 볼린저 밴드 계산 (이동평균과 표준편차를 사용)
     */
    @Override
    public <T> List<T> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize, Class<T> resultType) {

        if (!resultType.equals(BollingerBandResult.class)) {
            throw new IllegalArgumentException("Only BollingerBand resultType is supported");
        }

        // 이동평균과 표준편차 계산 (위임 방식)
        List<BigDecimal> maList = maStrategy.convertTimeSeries(timeSeriesData, windowSize);
        List<BigDecimal> sdList = sdStrategy.convertTimeSeries(timeSeriesData, windowSize);

        // 볼린저 밴드 결과 생성
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

