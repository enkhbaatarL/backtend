package com.sk.domain.admin.service.excel;

import org.springframework.stereotype.Component;

import com.sk.domain.shared.enums.ComponentType;

@Component
public class DataCollectorStrategyResolver {
    public DataCollectorStrategy resolve(ComponentType componentType) {
        if (componentType == ComponentType.PIVOT) {
            return new PivotDataCollector();
        } else if (componentType == ComponentType.CPC_NINO_SST) {
            return new NinoSstDataCollector();
        } else {
            return new DefaultDataCollector();
        }
    }
}
