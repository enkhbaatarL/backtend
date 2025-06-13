package com.sk.domain.admin.service.excel.collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultDataCollector implements DataCollectorStrategy {

    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = ExcelParsedUtil.parseExcelRow(row, actualColumns, criteriaDt);
                if (rowData.length > 0)
                    result.add(rowData);
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            CSVParser csvParser = (CSVParser) data;
            for (CSVRecord record : csvParser) {
                // 데이터 시작 행 인덱스 조건
                if (record.getRecordNumber() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = ExcelParsedUtil.parseCsvRecord(record, actualColumns, criteriaDt);
                if (rowData.length > 0)
                    result.add(rowData);
            }
        }
        return result;
    }
}
