package com.sk.domain.admin.service.excel.collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PivotDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int HEADER_COLUMN_INDEX = 1; // 헤더 열 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final int MAX_PERIOD_CODE_LENGTH = 10; // oni_anly_perd_spt_cd 최대 길이

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes.subList(HEADER_COLUMN_INDEX, periodCodes.size()) : createDefaultHeaders(periodCodes.size());
            periodCodes = periodCodes.stream().map(this::truncatePeriodCode).collect(Collectors.toList());
            log.debug("Excel period codes: {}", periodCodes);
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;
                // 데이터 파싱
                Cell yearCell = row.getCell(0);
                if (yearCell == null || yearCell.getCellType() != CellType.NUMERIC)
                    continue;
                // 데이터 파싱
                int year = (int) yearCell.getNumericCellValue();
                for (int i = 1; i < row.getLastCellNum() && i - 1 < periodCodes.size(); i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    double value = valueCell.getNumericCellValue();
                    result.add(new Object[] {year, periodCodes.get(i - 1), value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            CSVParser csvParser = (CSVParser) data;
            // 헤더 파싱
            List<String> periodCodes = actualColumns.size() > 1 ? actualColumns.subList(HEADER_COLUMN_INDEX, actualColumns.size())
                    : createDefaultHeaders(actualColumns.size());
            periodCodes = periodCodes.stream().map(this::truncatePeriodCode).collect(Collectors.toList());
            log.debug("CSV period codes: {}", periodCodes);
            for (CSVRecord record : csvParser) {
                // 데이터 파싱
                String firstCell = record.get(0) != null ? record.get(0).trim() : null;
                if (firstCell == null || !firstCell.matches("\\d+")) {
                    log.debug("Skipping invalid year value: {}", firstCell);
                    continue;
                }
                // 데이터 파싱
                int year;
                try {
                    year = Integer.parseInt(firstCell);
                } catch (NumberFormatException e) {
                    log.debug("Invalid year format: {}", firstCell);
                    continue;
                }
                for (int i = 1; i < record.size() && i - 1 < periodCodes.size(); i++) {
                    String valueStr = record.get(i) != null ? record.get(i).trim() : null;
                    if (valueStr == null || valueStr.isEmpty())
                        continue;
                    double value;
                    try {
                        value = Double.parseDouble(valueStr);
                    } catch (NumberFormatException e) {
                        log.debug("Invalid value format at column {}: {}", i, valueStr);
                        continue;
                    }
                    Object[] rowData = new Object[] {year, periodCodes.get(i - 1), value};
                    result.add(rowData);
                }
            }
        }
        return result;
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }

    private String truncatePeriodCode(String code) {
        return code.length() > MAX_PERIOD_CODE_LENGTH ? code.substring(0, MAX_PERIOD_CODE_LENGTH) : code;
    }
}