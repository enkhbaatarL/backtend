package com.sk.domain.mi.conversion.strategy.basic;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.FormulaService;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 이동 평균 계산을 위한 전략 클래스
 */
@Component
public class MovingAverageConversion implements ConversionStrategy {

    @Override
    public FormulaService getFormulaService() {
        return FormulaService.MOVING_AVERAGE;
    }

    @Override
    public BigDecimal convert(BigDecimal value) {
        throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "MOVING_AVERAGE requires a time series data");
    }

    /**
     * 시계열 데이터에 대한 이동 평균 계산
     */
//    @Override
//    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
//
//        if (timeSeriesData == null) {
//            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
//        }
//
//        // 결과 리스트 초기화 (null로 채워진 동일한 크기의 리스트)
//        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(timeSeriesData.size(), null));
//
//        // 윈도우 크기 이상의 데이터가 있는 시점부터 이동평균 계산
//        for (int i = windowSize - 1; i < timeSeriesData.size(); i++) {
//            BigDecimal sum = BigDecimal.ZERO;
//
//            // 현재 윈도우의 데이터 합계 계산
//            for (int j = 0; j < windowSize; j++) {
//                sum = MathUtil.add(sum, timeSeriesData.get(i - j));
//            }
//
//            // 평균 계산 및 결과 저장
//            results.set(i, MathUtil.divide(sum, new BigDecimal(windowSize)));
//        }
//
//        return results;
//    }

    @Override
    public List<BigDecimal> convertTimeSeries(List<BigDecimal> timeSeriesData, int windowSize) {
        if (timeSeriesData == null) {
            throw SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Time series data cannot be null");
        }

        List<BigDecimal> results = new ArrayList<>(Collections.nCopies(timeSeriesData.size(), null));

        for (int i = windowSize - 1; i < timeSeriesData.size(); i++) {
            List<BigDecimal> window = new ArrayList<>(windowSize);
            for (int j = 0; j < windowSize; j++) {
                BigDecimal val = timeSeriesData.get(i - j);
                if (val != null) {
                    window.add(val);
                }
            }
            // 유효 데이터가 하나도 없으면 결과도 null
            if (window.isEmpty()) {
                results.set(i, null);
            } else {
                // 평균 계산
                BigDecimal sum = BigDecimal.ZERO;
                for (BigDecimal v : window) sum = MathUtil.add(sum, v);
                results.set(i, MathUtil.divide(sum, new BigDecimal(window.size())));
            }
        }
        return results;
    }


    @Override
    public boolean supports(FormulaService formulaService) {
        return formulaService == getFormulaService();
    }

}
