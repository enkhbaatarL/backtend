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
public class YoYPeriodCalculator extends AbstractPeriodCalculator{

    @Override
    public CalculatePeriodChangeType getCalculatePeriodChangeType() {
        return CalculatePeriodChangeType.YOY;
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

        // 이번 연도
        LocalDate thisYearStart = today.withMonth(1).withDayOfMonth(1);
        LocalDate thisYearEnd = thisYearStart.withMonth(12).withDayOfMonth(31);
        // 지난 연도
        LocalDate lastYearStart = thisYearStart.minusYears(1);
        LocalDate lastYearEnd = lastYearStart.withMonth(12).withDayOfMonth(31);


        return calculatePeriodChangeValue(
            rawData, dateColumnName, defaultDataColumnName,
            thisYearStart,
            thisYearEnd,
            lastYearStart,
            lastYearEnd);
    }
}
