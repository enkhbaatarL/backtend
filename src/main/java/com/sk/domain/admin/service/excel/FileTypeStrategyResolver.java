package com.sk.domain.admin.service.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;

@Component
@RequiredArgsConstructor
public class FileTypeStrategyResolver {
    private final DataCollectorStrategyResolver dataCollectorStrategyResolver;

    public FileTypeStrategy resolve(String fileName) {
        if (fileName.endsWith(".csv")) {
            return new CsvFileParsed(dataCollectorStrategyResolver);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return new ExcelFileParsed(dataCollectorStrategyResolver);
        }
        throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported file type: " + fileName);
    }
}
