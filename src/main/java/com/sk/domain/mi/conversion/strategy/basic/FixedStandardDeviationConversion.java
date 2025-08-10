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
import java.util.Objects;

/**
 * 고정 표준편차(Fixed Standard Deviation)를 계산하는 전략 클래스
 * 전체 시계열 데이터 중 최근 최대 1년치(365개) 데이터를 기준으로 한 번만 표준편차를 계산하여
 * 모든 인덱스에 동일한 값을 반환한다.
 */
@Component
public class FixedStandardDeviationConversion implements ConversionStrategy {

    private static final int LOOKBACK_DAYS = 365;

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.FIXED_STANDARD_DEVIATION;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "FIXED_STANDARD_DEVIATION requires a full time series data");
    }

    /**
     * 시계열 전체를 기준으로, 최근 최대 1년치(LOOKBACK_DAYS)에 해당하는 데이터만을 사용하여
     * 고정된 표준편차를 계산한다. 데이터가 LOOKBACK_DAYS보다 적으면 전체 데이터를 사용한다.
     *
     * @param timeSeriesData 전체 시계열 데이터 (일별 기준 정렬된 값)
     * @param windowSize     파라미터는 무시됩니다.
     * @return 모든 인덱스에 동일한 고정 표준편차 값을 담은 리스트
     */
//    @Override
//    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
//
//        if (timeSeriesData == null) {
//            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
//        }
//
//        int total = timeSeriesData.size();
//
//        if (total == 0) {
//            return Collections.emptyList();
//        }
//
//        // 최근 최대 LOOKBACK_DAYS 범위 설정
//        int lookback = Math.min(total, LOOKBACK_DAYS);
//
//        List<BigDecimal> recent = timeSeriesData.subList(total - lookback, total);
//
//        // 전체 평균 계산
//        BigDecimal sum = BigDecimal.ZERO;
//        for (BigDecimal v : recent) {
//            sum = MathUtil.add(sum, v);
//        }
//
//        BigDecimal mean = MathUtil.divide(sum, new BigDecimal(lookback));
//
//        // 분산 계산 (population 분산)
//        BigDecimal sumOfSquaredDiffs = BigDecimal.ZERO;
//        for (BigDecimal v : recent) {
//            BigDecimal diff = MathUtil.subtract(v, mean);
//            BigDecimal sqr = MathUtil.multiply(diff, diff);
//            sumOfSquaredDiffs = MathUtil.add(sumOfSquaredDiffs, sqr);
//        }
//        BigDecimal variance = MathUtil.divide(sumOfSquaredDiffs, new BigDecimal(lookback));
//
//        // 표준편차 계산
//        BigDecimal stdDev = MathUtil.sqrt(variance);
//
//        // 모든 인덱스에 동일한 값 할당
//        return new ArrayList<>(Collections.nCopies(total, stdDev));
//    }

    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {

        if (timeSeriesData == null) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
        }

        int total = timeSeriesData.size();
        if (total == 0) return Collections.emptyList();

        int lookback = Math.min(total, LOOKBACK_DAYS);
        List<BigDecimal> recent = timeSeriesData.subList(total - lookback, total)
            .stream().filter(Objects::nonNull).toList();

        if (recent.isEmpty()) {
            // 유효 데이터가 없으면 전체 null
            return new ArrayList<>(Collections.nCopies(total, null));
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : recent) sum = MathUtil.add(sum, v);
        BigDecimal mean = MathUtil.divide(sum, new BigDecimal(recent.size()));

        BigDecimal sumSqDiff = BigDecimal.ZERO;
        for (BigDecimal v : recent) {
            BigDecimal diff = MathUtil.subtract(v, mean);
            sumSqDiff = MathUtil.add(sumSqDiff, MathUtil.multiply(diff, diff));
        }
        BigDecimal variance = MathUtil.divide(sumSqDiff, new BigDecimal(recent.size()));
        BigDecimal stdDev = MathUtil.sqrt(variance);

        return new ArrayList<>(Collections.nCopies(total, stdDev));
    }


    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

}
