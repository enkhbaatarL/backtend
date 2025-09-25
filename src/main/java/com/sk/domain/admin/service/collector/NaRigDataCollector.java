package com.sk.domain.admin.service.collector;

import com.sk.domain.shared.enums.ComponentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NaRigDataCollector implements DataCollectorStrategy {

    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // poi DateUtil 사용으로 Custom DateUtil 사용 불가로 패턴 직접 사용
    public static final DateTimeFormatter MM = DateTimeFormatter.ofPattern("MM"); // poi DateUtil 사용으로 Custom DateUtil 사용 불가로 패턴 직접 사용
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final int MONTH_COLUMN_INDEX = 9; // 월 열 인덱스
    private static final int DATE_COLUMN_INDEX = 10; // 날짜 열 인덱스

    @Override
    public ComponentType getComponentType() {
        return ComponentType.NA_RIG_WEEKLY;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
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
                Object[] rowData = parseExcelRow(row, actualColumns, criteriaDt);
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

    /**
     * 엑셀 행 파싱
     * @param row
     * @param columnNames
     * @param criteriaDt
     * @return
     */
    public static Object[] parseExcelRow(Row row, List<String> columnNames, String criteriaDt) {
        List<Object> rowData = new ArrayList<>();
        if (criteriaDt != null) {
            rowData.add(criteriaDt);
        }
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Object value = null;
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (i == MONTH_COLUMN_INDEX) {
                    double numValue = cell.getNumericCellValue();
                    int intValue = numValue != 0 ? (int) numValue : 0;
                    LocalDate date = intValue != 0 ? LocalDate.of(2025, intValue, 1) : null;
                    value = date != null ? date.format(MM) : null;
                } else if (i == DATE_COLUMN_INDEX) {
                    String dateStr = cell.getStringCellValue();
                    String dateStrWithoutHyphen = dateStr != null && !dateStr.trim().isEmpty() ? dateStr.replaceAll("-", "") : null;
                    String dateStrWithoutSlash = dateStrWithoutHyphen != null && !dateStrWithoutHyphen.trim().isEmpty() ? dateStrWithoutHyphen.replaceAll("/", "") : null;
                    value = dateStrWithoutSlash != null ? Long.parseLong(dateStrWithoutSlash) : null;
                } else {
                    switch (cell.getCellType()) {
                        case STRING:
                            value = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                value = cell.getDateCellValue();
                            } else {
                                double numValue = cell.getNumericCellValue();
                                if (numValue == Math.floor(numValue)) {
                                    value = (int) numValue;
                                } else {
                                    value = BigDecimal.valueOf(numValue);
                                }
                            }
                            break;
                        case BOOLEAN:
                            value = cell.getBooleanCellValue();
                            break;
                        default:
                            value = cell.toString();
                    }
                }
            }
            rowData.add(value);
        }
        return rowData.toArray();
    }
}
