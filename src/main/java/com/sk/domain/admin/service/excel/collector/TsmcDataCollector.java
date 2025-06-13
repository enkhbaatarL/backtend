package com.sk.domain.admin.service.excel.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TsmcDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 6; // 7행 (0-based index)
    private static final int HEADER_COLUMN_START_INDEX = 1; // 헤더 추출 후 날짜 Cell 추출 시작 열 (0-based index)
    private static final int HEADER_COLUMN_END_INDEX = 1; // 헤더 추출 후 날짜 Cell 추출 끝 제외 열
    private static final int VALUE_COLUMN_INDEX = 1; // 비어있는 Cell 제외한 Value 시작 열 (0-based index)
    private static final int DATA_START_ROW_INDEX = 7; // 8행 (0-based index)
    private static final int DATE_COLUMN_INDEX = 0; // A열 (0-based index)


    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();

            // 헤더 파싱 (Pivor Value 넣을 값)
            List<String> headers = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            List<String> dateHeaders = headers.size() > 1 ? headers.subList(HEADER_COLUMN_START_INDEX, headers.size()  - HEADER_COLUMN_END_INDEX) : Collections.emptyList();
            log.debug("Excel period codes: {}", dateHeaders);

            int lastRowNum = -1; // 이전 행 번호 추적

            
            int currentRowNumTemp = sheet.getLastRowNum();
            log.debug("Current row num: {}", currentRowNumTemp);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int currentRowNum = row.getRowNum();

                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DATA_START_ROW_INDEX) {
                    continue;
                }
                
                // 행 번호가 연속적이지 않으면 비어 있는 행이 있다고 간주
                if (lastRowNum != -1 && currentRowNum > lastRowNum + 1) {
                    log.debug("Detected empty row(s) between row {} and row {}, stopping processing", lastRowNum, currentRowNum);
                    break;
                }

                // 행 번호 업데이트 (비어있는 행 처리 위함)
                lastRowNum = currentRowNum;

                // 데이터 파싱
                Cell dateCell = row.getCell(DATE_COLUMN_INDEX);
                if (dateCell == null || dateCell.getCellType() != CellType.NUMERIC) {
                    continue;
                }

                // 날짜 타입 변환
                String year = dateCell.getStringCellValue().trim();
                
                // 데이터 파싱
                for (int i = VALUE_COLUMN_INDEX; i < row.getLastCellNum() && i < dateHeaders.size() + VALUE_COLUMN_INDEX; i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    double value = valueCell.getNumericCellValue();
                    Object day = dateHeaders.get(i - VALUE_COLUMN_INDEX);
                    String date = DateUtil.fromYyyyMmmToYyyyMm(String.format("%s-%s", year, day));
                    result.add(new Object[] {date, value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            //TODO: 필요시 구현
        }
        return result;
    }

    // private List<String> createDefaultHeaders(int size) {
    //     return IntStream.range(0, Math.max(1, size))
    //             .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
    //             .collect(Collectors.toList());
    // }

    // private String truncatePeriodCode(String code) {
    //     return code.length() > MAX_MEMBER_NAME_LENGTH ? code.substring(0, MAX_MEMBER_NAME_LENGTH) : code;
    // }
}
