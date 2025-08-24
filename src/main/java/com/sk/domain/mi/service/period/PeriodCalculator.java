package com.sk.domain.mi.service.period;

import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.util.List;
import java.util.Map;

public interface PeriodCalculator {

    CalculatePeriodChangeType getCalculatePeriodChangeType();

    CalculatePeriodChangeValue calculatePeriodChange(
        List<Map<String, Object>> rawData,
        String dateColumnName,
        String defaultDataColumnName);
}
