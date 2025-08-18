package com.sk.domain.admin.service.excel;

import com.sk.domain.admin.service.excel.collector.*;
import org.springframework.stereotype.Component;

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
        } else if (componentType == ComponentType.WW_RIG_SUMMARY) {
            return new Ww2RigSummaryDataCollector();
        } else if (componentType == ComponentType.SHORT_TERM_EO) {
            return new ShortTermEoDataCollector();
        } else if (componentType == ComponentType.OECD_CLI) {
            return new OecdCliDataCollector();
        } else if (componentType == ComponentType.TSMC) {
            return new TsmcDataCollector();
        } else if (componentType == ComponentType.PMI_MANUFACTURE) {
            return new PmiManufactureDataCollector();
        } else if (componentType == ComponentType.GLOBAL_OIL) {
            return new GlobalCrudeOilDataCollector();
        } else if (componentType == ComponentType.GIE_EU) {
            return new GieEuDataCollector();
        } else {
            return new DefaultDataCollector();
        }
    }


}
