package com.sk.domain.mi.service.period;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WoWPeriodCalculator extends AbstractPeriodCalculator{

    @Override
    public CalculatePeriodChangeType getCalculatePeriodChangeType() {
        return CalculatePeriodChangeType.WOW;
    }

    @Override
    public CalculatePeriodChangeValue calculatePeriodChange(
        GoodsDataSourceDetail mainGoodsDataSource, List<Map<String, Object>> rawData,
        String dateColumnName, String defaultDataColumnName) {

        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        Map<String,Object> lastDayEntity = rawData.get(rawData.size() -1);
        LocalDate today = DateUtil.parseDate(lastDayEntity.get(dateColumnName));

        // 이번 주
        LocalDate thisWeekStart = today.minusDays(today.getDayOfWeek().getValue() % 7); // 일요일
        LocalDate thisWeekEnd = today;
        // 지난 주
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);


        return calculatePeriodChangeValue(
            rawData, dateColumnName, defaultDataColumnName,
            thisWeekStart,
            thisWeekEnd,
            lastWeekStart,
            lastWeekEnd);
    }
}
