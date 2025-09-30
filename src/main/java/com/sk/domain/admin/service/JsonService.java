package com.sk.domain.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.FileTypeStrategy;
import com.sk.domain.admin.service.json.DataCollectorJsonStrategy;
import com.sk.domain.admin.service.json.DataCollectorJsonStrategyResolver;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class JsonService {

    private final ImportService importService;
    private final DataCollectorJsonStrategyResolver dataCollectorJsonStrategyResolver;

    @Transactional
    public Boolean uploadStructuredData(ComponentInfo componentInfo, String criteriaDt, String category, JsonNode jsonNode){
        try {
            importService.validateComponentInfo(componentInfo);
            List<String> columnNames = Arrays.stream(componentInfo.getExcelTableColumnListNm().split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());

            DataCollectorJsonStrategy dataCollectorJsonStrategy = dataCollectorJsonStrategyResolver.resolve(componentInfo.getDataStructureConvertNm());
            List<Object[]> values = dataCollectorJsonStrategy.processJsonData(jsonNode,null, null, null );

            return importService.saveBatchData(componentInfo.getTargetDbNm(), columnNames, values, componentInfo.getTargetTableNm());
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }




}
