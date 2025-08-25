package com.sk.domain.mi.service.period;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.entity.master.tif.GieEu;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import com.sk.domain.shared.repository.master.tif.GieEuRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiveYearAvgPeriodCalculator extends AbstractPeriodCalculator{

    private final GieEuRepository gieEuRepository;
    private static final String GIE_EU_CODE_PAST_5Y_AVG = "PAST_5Y_AVG";
    private static final String GIE_EU_TABLE_NAME = "tif_gier_eu";

    @Override
    public CalculatePeriodChangeType getCalculatePeriodChangeType() {
        return CalculatePeriodChangeType.FYA;
    }

    @Override
    public CalculatePeriodChangeValue calculatePeriodChange(
        GoodsDataSourceDetail mainGoodsDataSource, List<Map<String, Object>> rawData,
        String dateColumnName, String defaultDataColumnName) {

        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        if(!GIE_EU_TABLE_NAME.equals(mainGoodsDataSource.getTargetTableNm())){
            log.warn("{} is for {} only", this.getCalculatePeriodChangeType(), GIE_EU_TABLE_NAME);
            return null;
        }

        Map<String,Object> lastDayEntity = rawData.get(rawData.size() -1);
        LocalDate today = DateUtil.parseDate(lastDayEntity.get(dateColumnName));

        Optional<GieEu> past5yAvg = gieEuRepository.findByCodeAndDate(GIE_EU_CODE_PAST_5Y_AVG, today.atStartOfDay());
        if(past5yAvg.isEmpty()) return null;

        Object lastValue = lastDayEntity.get(defaultDataColumnName);
        if (lastValue == null) return null;
        double lastValueDouble;
        try {
            lastValueDouble = Double.parseDouble(lastValue.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid number format: {}", lastValue, e);
            return null;
        }

        return calculateChangeRate(lastValueDouble, past5yAvg.get().getGasInStorageMt().doubleValue());
    }
}
