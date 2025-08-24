package com.sk.domain.mi.service.period;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DoDPeriodCalculator extends AbstractPeriodCalculator{

    @Override
    public CalculatePeriodChangeType getCalculatePeriodChangeType() {
        return CalculatePeriodChangeType.DOD;
    }

    @Override
    public CalculatePeriodChangeValue calculatePeriodChange(List<Map<String, Object>> rawData,
        String dateColumnName, String defaultDataColumnName) {

        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        Map<String,Object> lastDayEntity = rawData.get(rawData.size() -1);
        LocalDate today = DateUtil.parseDate(lastDayEntity.get(dateColumnName));
        // 어제
        LocalDate yesterday = today.minusDays(1);

        return calculatePeriodChangeValue(
            rawData, dateColumnName, defaultDataColumnName,
            today,
            today,
            yesterday,
            yesterday);
    }
}
