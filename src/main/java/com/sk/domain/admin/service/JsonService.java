package com.sk.domain.admin.service;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.FileTypeStrategy;
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

    @Transactional
    public boolean uploadStructuredData(ComponentInfo componentInfo, String criteriaDt, String category) throws IOException {
        try {
            importService.validateComponentInfo(componentInfo);
            List<String> columnNames = Arrays.stream(componentInfo.getExcelTableColumnListNm().split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());

            FileTypeStrategy strategy = fileTypeStrategyResolver.resolve(ExcelParsedUtil.validateFile(file));
            List<Object[]> values = strategy.processFile(file, componentInfo, headerRowIndex, dataStartRowIndex, criteriaDt, category);

            return importService.saveBatchData(componentInfo.getTargetDbNm(), columnNames, values, componentInfo.getTargetTableNm());
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }




}
