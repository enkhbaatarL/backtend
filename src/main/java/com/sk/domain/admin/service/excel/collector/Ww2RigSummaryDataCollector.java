package com.sk.domain.admin.service.excel.collector;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Ww2RigSummaryDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 2; // 데이터 시작 행 인덱스
    
    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        if (criteriaDt == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, String.format("criteriaDt is null: %s", criteriaDt));
        }

        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            // 빈 문자열 제거
            // periodCodes.removeIf(code -> code == null || code.isEmpty());
            String dateStr = periodCodes.stream().findFirst().orElse(null);
            String yearMonth = null;
            if (dateStr != null) {
                LocalDate date = DateUtil.parseDate(dateStr);
                yearMonth = DateUtil.toYearMonth(date);
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = ExcelParsedUtil.parseExcelRow(row, actualColumns, criteriaDt);
                if (rowData.length > 0) {
                    if (yearMonth != null) {
                        Object[] extendedData = new Object[rowData.length + 1];
                        System.arraycopy(rowData, 0, extendedData, 0, rowData.length);
                        extendedData[rowData.length] = yearMonth;
                        result.add(extendedData);
                    } else {
                        result.add(rowData);
                    }
                }
            }            
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            // CSVParser csvParser = (CSVParser) data;
            // for (CSVRecord record : csvParser) {
            //     // 데이터 시작 행 인덱스 조건
            //     if (record.getRecordNumber() < DEFAULT_DATA_START_ROW_INDEX)
            //         continue;

            //     // 데이터 파싱
            //     Object[] rowData = ExcelParsedUtil.parseCsvRecord(record, actualColumns, criteriaDt);
            //     if (rowData.length > 0)
            //         result.add(rowData);
            // }
        }
        return result;
    }
}
