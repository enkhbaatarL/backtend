package com.sk.domain.admin.service.excel;

import org.springframework.stereotype.Component;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;

@Component
public class FileTypeStrategyResolver {
    public FileTypeStrategy resolve(String fileName) {
        if (fileName.endsWith(".csv")) {
            return new CsvFileParsed(new DataCollectorStrategyResolver());
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return new ExcelFileParsed(new DataCollectorStrategyResolver());
        }
        throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported file type: " + fileName);
    }
}
