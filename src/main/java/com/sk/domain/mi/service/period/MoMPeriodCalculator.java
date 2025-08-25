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
public class MoMPeriodCalculator extends AbstractPeriodCalculator{

    @Override
    public CalculatePeriodChangeType getCalculatePeriodChangeType() {
        return CalculatePeriodChangeType.MOM;
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

        // 이번 달
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd = thisMonthStart.withDayOfMonth(thisMonthStart.lengthOfMonth());
        // 지난 달
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());


        return calculatePeriodChangeValue(
            rawData, dateColumnName, defaultDataColumnName,
            thisMonthStart,
            thisMonthEnd,
            lastMonthStart,
            lastMonthEnd);
    }
}
