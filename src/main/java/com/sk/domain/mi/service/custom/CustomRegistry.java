package com.sk.domain.mi.service.custom;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.service.custom.chart.CustomChartMaker;
import com.sk.domain.mi.service.period.PeriodCalculator;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomRegistry {

    private final Map<String, CustomChartMaker<?>> customChartMakerMap;
    private Map<String, CustomChartMaker<?>> customChartMakerMapByCustomId;

    @PostConstruct
    public void init(){
        customChartMakerMapByCustomId = new HashMap<>();
        for(CustomChartMaker<?> customChartMaker : customChartMakerMap.values()){
            customChartMakerMapByCustomId.put(customChartMaker.getCustomId(), customChartMaker);
        }
    }

    public CustomChartMaker<?> getCustomChartMaker(String customId){

        CustomChartMaker<?> customChartMaker = customChartMakerMapByCustomId.get(customId);
        if(customChartMaker == null){
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "customId is not exist.");
        }
        return customChartMaker;
    }

}
