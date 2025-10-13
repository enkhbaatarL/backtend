package com.sk.domain.admin.service.excel;

import com.sk.domain.shared.enums.ComponentType;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataCollectorStrategyResolver {

    private final Map<String, DataCollectorStrategy> dataCollectorStrategies;
    private Map<ComponentType, DataCollectorStrategy> dataCollectorStrategyMap;

    @PostConstruct
    public void init(){
        dataCollectorStrategyMap = new HashMap<>();
        for(DataCollectorStrategy dataCollectorStrategy : dataCollectorStrategies.values()){
            dataCollectorStrategyMap.put(dataCollectorStrategy.getComponentType(), dataCollectorStrategy);
        }
    }
    public DataCollectorStrategy resolve(ComponentType componentType) {
        DataCollectorStrategy dataCollectorStrategy = dataCollectorStrategyMap.get(componentType);
        if(dataCollectorStrategy == null){
            dataCollectorStrategyMap.get(ComponentType.GENERAL);
        }
        return dataCollectorStrategy;
    }
}
