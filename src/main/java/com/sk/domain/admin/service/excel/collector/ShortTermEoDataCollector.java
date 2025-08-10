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

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShortTermEoDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더행 인덱스 (0-based index)
    private static final int DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스 (0-based index)
    private static final int HEADER_COLUMN_START_INDEX = 2; // 헤더 추출 후 날짜 Cell 추출 시작 열 (0-based index)
    private static final int VALUE_COLUMN_INDEX = 2; // 비어있는 Cell 제외한 Value 시작 열 (0-based index)
    private static final int YEAR_COLUMN_INDEX = 0; // A열 (0-based index)
    private static final int MONTH_COLUMN_INDEX = 1; // B열 (0-based index)


    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        if (category == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, String.format("category is null: %s", category));
        }

        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();

            // 헤더 파싱 (report item code)
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes.subList(HEADER_COLUMN_START_INDEX, periodCodes.size()) : Collections.emptyList();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DATA_START_ROW_INDEX) {
                    continue;
                }

                // 데이터 파싱
                Cell yearCell = row.getCell(YEAR_COLUMN_INDEX);
                Cell monthCell = row.getCell(MONTH_COLUMN_INDEX);

                // 날짜 타입 변환
                String yearStr = yearCell.getStringCellValue().trim();
                String monthStr = monthCell.getStringCellValue().trim();
                
                // 데이터 파싱
                for (int i = VALUE_COLUMN_INDEX; i < row.getLastCellNum() && i < periodCodes.size() + VALUE_COLUMN_INDEX; i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    double value = valueCell.getNumericCellValue();
                    String date = DateUtil.fromYyyyMmmToYyyyMm(String.format("%s-%s", yearStr, monthStr));
                    Object periodCode = periodCodes.get(i - VALUE_COLUMN_INDEX);
                    result.add(new Object[] {category, date, periodCode, value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            //TODO: 필요시 구현
        }
        return result;
    }
}
