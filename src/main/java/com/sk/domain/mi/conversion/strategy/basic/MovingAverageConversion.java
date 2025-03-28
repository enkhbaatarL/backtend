package com.sk.domain.mi.conversion.strategy.basic;

import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 이동 평균 계산을 위한 전략 클래스
 */
public class MovingAverageConversion implements ConversionStrategy {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.MOVING_AVERAGE;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw new UnsupportedOperationException("Moving average requires a time series data");
    }

    /**
     * 시계열 데이터에 대한 이동 평균 계산
     */
    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {

        if (timeSeriesData == null) {
            throw new IllegalArgumentException("Time series data cannot be null");
        }

        // 결과 리스트 초기화 (null로 채워진 동일한 크기의 리스트)
        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(timeSeriesData.size(), null));

        // 윈도우 크기 이상의 데이터가 있는 시점부터 이동평균 계산
        for (int i = windowSize - 1; i < timeSeriesData.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;

            // 현재 윈도우의 데이터 합계 계산
            for (int j = 0; j < windowSize; j++) {
                sum = sum.add(timeSeriesData.get(i - j), MC);
            }

            // 평균 계산 및 결과 저장
            results.set(i, sum.divide(new BigDecimal(windowSize), MC));
        }

        return results;
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

}
