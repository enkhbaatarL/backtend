package com.sk.domain.admin.service.json;

import com.sk.domain.admin.service.excel.DataCollectorStrategy;
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
public class DataCollectorJsonStrategyResolver {

    private final Map<String, DataCollectorJsonStrategy> dataCollectorJsonStrategies;
    private Map<ComponentType, DataCollectorJsonStrategy> dataCollectorStrategyMap;

    @PostConstruct
    public void init(){
        dataCollectorStrategyMap = new HashMap<>();
        for(DataCollectorJsonStrategy dataCollectorStrategy : dataCollectorJsonStrategies.values()){
            dataCollectorStrategyMap.put(dataCollectorStrategy.getComponentType(), dataCollectorStrategy);
        }
    }
    public DataCollectorJsonStrategy resolve(ComponentType componentType) {
        DataCollectorJsonStrategy dataCollectorStrategy = dataCollectorStrategyMap.get(componentType);
        if(dataCollectorStrategy == null){
            dataCollectorStrategyMap.get(ComponentType.DEFAULT);
        }
        return dataCollectorStrategy;
    }
}
