package com.sk.domain.mi.service.period;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractPeriodCalculator implements PeriodCalculator{
    protected CalculatePeriodChangeValue calculatePeriodChangeValue(
        List<Map<String, Object>> rawData,
        String dateColumnName,
        String defaultDataColumnName,
        LocalDate currentStartDate,
        LocalDate currentEndDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate) {
        double currentSum = 0.0;
        int currentCount = 0;
        double previousSum = 0.0;
        int previousCount = 0;

        for (Map<String, Object> row : rawData) {
            Object value = row.get(defaultDataColumnName);
            if (value == null) {
                continue;
            }

            double val;
            try {
                val = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid number format: {}, skipping", value, e);
                continue;
            }

            LocalDate date = parseRequiredMarketDate(row.get(dateColumnName), dateColumnName);

            if (!date.isBefore(currentStartDate) && !date.isAfter(currentEndDate)) {
                currentSum += val;
                currentCount++;
            } else if (!date.isBefore(previousStartDate) && !date.isAfter(previousEndDate)) {
                previousSum += val;
                previousCount++;
            }

        }

        // 데이터가 없으면 null 반환
        if (currentCount == 0 || previousCount == 0) {
            return CalculatePeriodChangeValue.ofEmpty();
        }

        double currentAvg = currentSum / currentCount;
        double previousAvg = previousSum / previousCount;

        return calculateChangeRate(currentAvg, previousAvg);
    }

    protected LocalDate parseRequiredMarketDate(Object rawDate, String columnName) {
        try {
            LocalDate parsedDate = DateUtil.parseDate(rawDate);
            if (parsedDate != null) {
                return parsedDate;
            }
        } catch (RuntimeException exception) {
            log.error("Invalid required market date. source: period-calculator, column: {}",
                columnName, exception);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }

        log.error("Invalid required market date. source: period-calculator, column: {}",
            columnName);
        throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
    }

    protected CalculatePeriodChangeValue calculateChangeRate(double currentValue, double previousValue){
        // 변화율 계산: ((현재 평균 - 이전 평균) / 이전 평균) * 100
        double changeRateRaw = ((currentValue - previousValue) / previousValue) * 100;
        BigDecimal changeRateBd = BigDecimal.valueOf(changeRateRaw).setScale(2, RoundingMode.HALF_UP); // formatting
        double changeRate = changeRateBd.doubleValue();

        // 변화 수치 계산: 현재 평균 - 이전 평균
        double changeValueRaw = currentValue - previousValue;
        BigDecimal changeValueBd = BigDecimal.valueOf(changeValueRaw).setScale(2, RoundingMode.HALF_UP); // formatting
        double changeValue = changeValueBd.doubleValue();

        return CalculatePeriodChangeValue.of(changeRate, changeValue);
    }
}
