package com.sk.domain.admin.service.collector;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PivotLiteDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int HEADER_COLUMN_INDEX = 1; // 헤더 열 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PIVOT_LITE;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet sheet) { // 엑셀 파일 파싱
            Iterator<Row> rowIterator = sheet.iterator();
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            if(periodCodes.size() < 2){
                throw new IllegalArgumentException("Data Column is not found. code size = " +periodCodes.size());
            }
            periodCodes = periodCodes.subList(HEADER_COLUMN_INDEX, periodCodes.size());
            log.debug("Excel period codes: {}", periodCodes);
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;
                // 데이터 파싱
                Cell dateCell = row.getCell(0);
                if (dateCell == null) continue;
                // 데이터 파싱
                LocalDate date = parseDate(dateCell.getStringCellValue());
                for (int i = 1; i < row.getLastCellNum() && i - 1 < periodCodes.size(); i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    double value = valueCell.getNumericCellValue();
                    result.add(new Object[] {date, periodCodes.get(i - 1), value});
                }
            }
        } else if (data instanceof CSVParser csvParser) { // CSV 파일 파싱
            if(actualColumns.size() < 2){
                throw new IllegalArgumentException("Data Column is not found. code size = " +actualColumns.size());
            }
            // 헤더 파싱
            List<String> periodCodes = actualColumns.subList(HEADER_COLUMN_INDEX, actualColumns.size());

            log.debug("CSV period codes: {}", periodCodes);
            for (CSVRecord record : csvParser) {
                // 데이터 파싱
                String firstCell = record.get(0) != null ? record.get(0).trim() : null;
                if (firstCell == null) {
                    log.debug("Skipping invalid year value is null");
                    continue;
                }
                // 데이터 파싱
                LocalDate date = parseDate(firstCell);
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
                    Object[] rowData = new Object[] {date, periodCodes.get(i - 1), value};
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

    private static LocalDate parseDate(String input){
        return LocalDate.parse(input, DATE_FORMATTER);
    }

}
