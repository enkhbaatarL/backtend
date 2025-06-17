package com.sk.domain.admin.service.excel.collector;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PmiManufactureDataCollector implements DataCollectorStrategy {
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final Pattern INPUT_PATTERN = Pattern.compile("^(\\w{3}\\s\\d{2},\\s\\d{4})\\s\\((\\w{3})\\)$");

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            // Sheet sheet = (Sheet) data;
            // Iterator<Row> rowIterator = sheet.iterator();
            // while (rowIterator.hasNext()) {
            //     Row row = rowIterator.next();
            //     // 데이터 시작 행 인덱스 조건
            //     if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
            //         continue;

            //     // 데이터 파
            //     Object[] rowData = ExcelParsedUtil.parseExcelRow(row, actualColumns, category);
            //     if (rowData.length > 0)
            //         result.add(rowData);
            // }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            CSVParser csvParser = (CSVParser) data;
            for (CSVRecord record : csvParser) {
                // 데이터 시작 행 인덱스 조건
                if (record.getRecordNumber() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = parseCsvPmiManufactureRecord(record, actualColumns, category);
                if (rowData.length > 0)
                    result.add(rowData);
            }
        }
        return result;
    }

    /**
     * CSV 레코드 파싱
     * 
     * @param record
     * @param columnNames
     * @param criteriaDt
     * @return
     */
    public static Object[] parseCsvPmiManufactureRecord(CSVRecord record, List<String> columnNames, String category) {
        List<Object> rowData = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {

            String value = i < record.size() ? record.get(i) : null;
            if (i == 0) { // 첫 열 데이터는 전처리 후 저장

                // 입력 파싱
                Matcher matcher = INPUT_PATTERN.matcher(value.trim());
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid input format: " + value);
                }

                String actualDateStr = matcher.group(1); // Jun 02, 2025
                String predictedMonthStr = matcher.group(2); // May

                LocalDate actualDate = DateUtil.fromYyyyMmDdEng(actualDateStr);
                String actualDateFormatted = DateUtil.toYmd(actualDate);

                // 예측 날짜 처리
                int actualMonth = actualDate.getMonthValue(); // 6 (Jun)
                int predictedMonth = parseMonth(predictedMonthStr); // 5 (May)
                int predictedYear = actualDate.getYear(); // 2025

                // 연도 조정: 예측 월 > 실제 월이면 연도 -1
                if (predictedMonth > actualMonth) {
                    predictedYear--;
                }

                LocalDate predictedDate = LocalDate.of(predictedYear, predictedMonth, 1);
                String predictedDateFormatted = DateUtil.toYearMonth(predictedDate);

                rowData.add(actualDateFormatted);
                rowData.add(predictedDateFormatted);

            } else if (i == 1) {
                int time = parseTime(value);
                rowData.add(time);
            } else {
                rowData.add(value != null && !value.trim().isEmpty() ? value.trim() : null);
            }
        }

        if (category != null) {
            rowData.add(category);
        }

        return rowData.toArray();
    }

    /**
     * 월 파싱(PMI 제조업 데이터 파싱 시 사용)
     * 
     * @param monthStr
     * @return
     */
    private static int parseMonth(String monthStr) {
        return LocalDate.parse("01 " + monthStr + " 2000",
                DateTimeFormatter.ofPattern("dd MMM yyyy"))
                .getMonthValue();
    }

    /**
     * 시간 파싱(PMI 제조업 데이터 파싱 시 사용)
     * 
     * @param timeInput
     * @return
     */
    private static int parseTime(String timeInput) {
        String cleanedTime = timeInput.replace(":", "");
        try {
            return Integer.parseInt(cleanedTime);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time format: " + timeInput, e);
        }
    }
}
