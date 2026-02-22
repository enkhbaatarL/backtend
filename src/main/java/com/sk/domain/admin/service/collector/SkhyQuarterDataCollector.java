package com.sk.domain.admin.service.collector;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SkhyQuarterDataCollector implements DataCollectorStrategy {

    private static final int DATE_START_ROW_INDEX = 0;
    private static final int DATE_START_COLUMN_INDEX = 3;
    private static final int CODE_COLUMN_INDEX = 2;
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final Pattern QUARTER_PATTERN = Pattern.compile("^([1-4])Q(\\d{2})$");


    @Override
    public ComponentType getComponentType() {
        return ComponentType.SKHY_DATA_QUARTER;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns) {
        List<Object[]> result = new ArrayList<>();
        Map<Integer, LocalDateTime> dateMap = new LinkedHashMap<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DATE_START_ROW_INDEX)
                    continue;
                if (row.getRowNum() == DATE_START_ROW_INDEX){
                    dateMap = extractQuarterDateTimeByColumnIndex(row);
                }
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                Cell codeCell = row.getCell(CODE_COLUMN_INDEX, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (codeCell == null) continue;
                String code = codeCell.getStringCellValue();
                if (code == null || code.trim().isEmpty()) continue;

                for (Map.Entry<Integer, LocalDateTime> entry : dateMap.entrySet()) {
                    int colIndex = entry.getKey();
                    LocalDateTime date = entry.getValue();
                    Cell valueCell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                    if (valueCell == null) continue;

                    Double value = null;
                    try {
                        switch (valueCell.getCellType()) {
                            case NUMERIC:
                                value = valueCell.getNumericCellValue();
                                break;
                            case STRING:
                                String cellValue = valueCell.getStringCellValue();
                                if (cellValue != null && !cellValue.trim().isEmpty()) {
                                    value = Double.parseDouble(cellValue.trim());
                                }
                                break;
                            default:
                                log.warn("Unsupported cell type at row {}, col {}: {}", row.getRowNum(), colIndex, valueCell.getCellType());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse numeric value at row {}, col {}: {}", row.getRowNum(), colIndex, e.getMessage());
                    }

                    if (value != null) {
                        result.add(new Object[]{date, code, value});
                    }
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            throw AppErrorException.of(REQ_INVALID_FORMAT, "이 데이터는 CSV 를 지원하지 않습니다.");
        }
        return result;
    }

    /**
     * DATE_START_COLUMN_INDEX부터 "값이 있는 마지막 셀"까지(끝 포함) 검색하여
     * (컬럼 인덱스 -> LocalDateTime) Map으로 반환한다.
     */
    private static Map<Integer, LocalDateTime> extractQuarterDateTimeByColumnIndex(Row row) {
        Map<Integer, LocalDateTime> result = new LinkedHashMap<>();
        if (row == null) return result;

        int endExclusive = lastNonEmptyCellEndExclusive(row);
        for (int i = DATE_START_COLUMN_INDEX; i < endExclusive; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                continue;
            }

            String raw = cell.getStringCellValue();
            if (raw == null) continue;

            String token = raw.trim();
            if (token.isEmpty()) continue;

            LocalDateTime ldt = parseQuarterTokenToLocalDateTime(token);
            result.put(i, ldt);
        }

        return result;
    }

    private static LocalDateTime parseQuarterTokenToLocalDateTime(String token) {
        java.util.regex.Matcher m = QUARTER_PATTERN.matcher(token);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid quarter token: " + token + " (expected like 1Q25, 4Q02)");
        }

        int quarter = Integer.parseInt(m.group(1));
        int yy = Integer.parseInt(m.group(2));
        int year = 2000 + yy; // "20XX 형태"

        int month = switch (quarter) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            default -> throw new IllegalArgumentException("Invalid quarter: " + quarter);
        };

        return LocalDateTime.of(year, month, 1, 0, 0);
    }

    private static int lastNonEmptyCellEndExclusive(Row row) {
        if (row == null) return SkhyQuarterDataCollector.DATE_START_COLUMN_INDEX;

        short lastCellNum = row.getLastCellNum(); // 마지막 인덱스 + 1, 없으면 -1
        if (lastCellNum < 0) return SkhyQuarterDataCollector.DATE_START_COLUMN_INDEX;

        int endExclusive = lastCellNum;

        for (int i = endExclusive - 1; i >= SkhyQuarterDataCollector.DATE_START_COLUMN_INDEX; i--) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                continue;
            }
            String s = cell.getStringCellValue();
            if (s != null && !s.trim().isEmpty()) {
                return i + 1;
            }
        }

        return SkhyQuarterDataCollector.DATE_START_COLUMN_INDEX;
    }
}
