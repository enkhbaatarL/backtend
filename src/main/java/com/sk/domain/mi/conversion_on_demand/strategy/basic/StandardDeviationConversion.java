package com.sk.domain.mi.conversion_on_demand.strategy.basic;

import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.mi.conversion_on_demand.strategy.AbstractComposableConversionStrategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StandardDeviationConversion extends AbstractComposableConversionStrategy {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.STANDARD_DEVIATION;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw new UnsupportedOperationException("Standard deviation requires a time series data");
    }

    /**
     * 인터페이스의 convertTimeSeriesList 메서드 구현
     */
    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        if (timeSeriesData == null) {
            throw new IllegalArgumentException("Time series data cannot be null");
        }

        // 결과 리스트 초기화 (null로 채워진 동일한 크기의 리스트)
        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(timeSeriesData.size(), null));

        // 윈도우 크기 이상의 데이터가 있는 시점부터 표준편차 계산
        for (int i = windowSize - 1; i < timeSeriesData.size(); i++) {
            // 현재 윈도우의 데이터 추출
            List<BigDecimal> windowData = new ArrayList<>(windowSize);
            for (int j = 0; j < windowSize; j++) {
                windowData.add(timeSeriesData.get(i - j));
            }

            // 평균 계산
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal dataPoint : windowData) {
                sum = sum.add(dataPoint, MC);
            }
            BigDecimal mean = sum.divide(new BigDecimal(windowSize), MC);

            // 각 데이터 포인트와 평균 간의 차이의 제곱의 합 계산
            BigDecimal sumOfSquaredDifferences = BigDecimal.ZERO;
            for (BigDecimal dataPoint : windowData) {
                BigDecimal difference = dataPoint.subtract(mean, MC);
                BigDecimal squaredDifference = difference.multiply(difference, MC);
                sumOfSquaredDifferences = sumOfSquaredDifferences.add(squaredDifference, MC);
            }

            // 분산 계산 (차이 제곱의 평균)
            BigDecimal variance = sumOfSquaredDifferences.divide(new BigDecimal(windowSize), MC);

            // 표준편차 계산 (분산의 제곱근)
            results.set(i, sqrt(variance, MC));
        }

        return results;
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

    /**
     * BigDecimal용 sqrt 함수
     */
    public static BigDecimal sqrt(BigDecimal value, MathContext mc) {
        BigDecimal x = BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
        BigDecimal last;

        do {
            last = x;
            x = value.divide(x, mc).add(x).divide(BigDecimal.valueOf(2), mc);
        } while (!x.equals(last));

        return x;
    }

}
