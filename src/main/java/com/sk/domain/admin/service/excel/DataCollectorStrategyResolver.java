package com.sk.domain.admin.service.excel;

import org.springframework.stereotype.Component;

import com.sk.domain.admin.service.excel.collector.DefaultDataCollector;
import com.sk.domain.admin.service.excel.collector.NaRigDataCollector;
import com.sk.domain.admin.service.excel.collector.NinoSstDataCollector;
import com.sk.domain.admin.service.excel.collector.OecdCliDataCollector;
import com.sk.domain.admin.service.excel.collector.PivotDataCollector;
import com.sk.domain.admin.service.excel.collector.PmiManufactureDataCollector;
import com.sk.domain.admin.service.excel.collector.TsmcDataCollector;
import com.sk.domain.shared.enums.ComponentType;

@Component
public class DataCollectorStrategyResolver {
    public DataCollectorStrategy resolve(ComponentType componentType) {
        if (componentType == ComponentType.PIVOT) {
            return new PivotDataCollector();
        } else if (componentType == ComponentType.CPC_NINO_SST) {
            return new NinoSstDataCollector();
        } else if (componentType == ComponentType.NA_RIG_WEEKLY) {
            return new NaRigDataCollector();
        } else if (componentType == ComponentType.NA_RIG_SUMMARY) {
            return new NinoSstDataCollector();
        } else if (componentType == ComponentType.WW_RIG_WEEKLY) {
            return new NinoSstDataCollector();
        } else if (componentType == ComponentType.WW_RIG_SUMMARY) {
            return new NinoSstDataCollector();
        } else if (componentType == ComponentType.OECD_CLI) {
            return new OecdCliDataCollector();
        } else if (componentType == ComponentType.TSMC) {
            return new TsmcDataCollector();
        } else if (componentType == ComponentType.PMI_MANUFACTURE) {
            return new PmiManufactureDataCollector();
        } else {
            return new DefaultDataCollector();
        }
    }
}
