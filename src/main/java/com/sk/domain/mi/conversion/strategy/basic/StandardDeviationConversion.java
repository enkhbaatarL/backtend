package com.sk.domain.mi.conversion.strategy.basic;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.FormulaService;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 표준편차 계산을 위한 전략 클래스
 */
@Component
public class StandardDeviationConversion implements ConversionStrategy {

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.STANDARD_DEVIATION;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "STANDARD_DEVIATION requires a time series data");
    }

    /**
     * 시계열 데이터에 대한 표준편차 계산
     */
    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {

        if (timeSeriesData == null) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
        }

        // 윈도우 크기가 1인 경우 표준편차는 항상 0
        if (windowSize == 1) {
            return timeSeriesData.stream()
                .map(value -> BigDecimal.ZERO)
                .toList();
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
                sum = MathUtil.add(sum, dataPoint);
            }
            BigDecimal mean = MathUtil.divide(sum, new BigDecimal(windowSize));

            // 각 데이터 포인트와 평균 간의 차이의 제곱의 합 계산
            BigDecimal sumOfSquaredDifferences = BigDecimal.ZERO;
            for (BigDecimal dataPoint : windowData) {
//                BigDecimal difference = dataPoint.subtract(mean, MathUtil.MC);
                BigDecimal difference = MathUtil.subtract(dataPoint, mean);
//                BigDecimal squaredDifference = difference.multiply(difference, MathUtil.MC);
                BigDecimal squaredDifference = MathUtil.multiply(difference, difference);
//                sumOfSquaredDifferences = sumOfSquaredDifferences.add(squaredDifference, MathUtil.MC);
                sumOfSquaredDifferences = MathUtil.add(sumOfSquaredDifferences, squaredDifference);
            }

            // 모든 값이 동일한 경우(분산이 0) 표준편차도 0
            if (sumOfSquaredDifferences.compareTo(BigDecimal.ZERO) == 0) {
                results.set(i, BigDecimal.ZERO);
                continue;
            }

            // 분산 계산 (차이 제곱의 평균)
//            BigDecimal variance = sumOfSquaredDifferences.divide(new BigDecimal(windowSize), MathUtil.MC);
            BigDecimal variance = MathUtil.divide(sumOfSquaredDifferences, new BigDecimal(windowSize));

            // 표준편차 계산 (분산의 제곱근)
            results.set(i, MathUtil.sqrt(variance));
        }

        return results;
    }

    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

}
