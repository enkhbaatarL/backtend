package com.sk.domain.admin.service.excel.collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class OecdCliDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 6; // 7행, extractExcelHeader 에서 비어있는 6행은 count를 안함
    private static final int HEADER_COLUMN_INDEX = 2; // 헤더 추출 후 비어있는 Cell 제외한 CODE 시작 열
    private static final int VALUE_COLUMN_INDEX = 3; // 비어있는 Cell 제외한 Value 시작 열
    private static final int DATA_START_ROW_INDEX = 8; // 9행 (0-based index)
    private static final int DATE_COLUMN_INDEX = 1; // B열 (0-based index)
    private static final int MAX_MEMBER_NAME_LENGTH = 50; // oecd_mem_nm 최대 길이

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();

            // 헤더 파싱 (Pivor Value 넣을 값)
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes.subList(HEADER_COLUMN_INDEX, periodCodes.size()) : createDefaultHeaders(periodCodes.size());
            periodCodes = periodCodes.stream().map(this::truncatePeriodCode).collect(Collectors.toList());
            log.debug("Excel period codes: {}", periodCodes);

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
                if (dateCell == null || dateCell.getCellType() != CellType.STRING) {
                    continue;
                }

                // 날짜 타입 변환
                String date = DateUtil.fromYyyyMmmToYyyyMm(dateCell.getStringCellValue().trim());

                // 데이터 파싱
                for (int i = VALUE_COLUMN_INDEX; i < row.getLastCellNum() && i < periodCodes.size() + VALUE_COLUMN_INDEX; i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    double value = valueCell.getNumericCellValue();
                    Object periodCode = periodCodes.get(i - VALUE_COLUMN_INDEX);
                    result.add(new Object[] {date, periodCode, value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            //TODO: 필요시 구현
        }
        return result;
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }

    private String truncatePeriodCode(String code) {
        return code.length() > MAX_MEMBER_NAME_LENGTH ? code.substring(0, MAX_MEMBER_NAME_LENGTH) : code;
    }
}
