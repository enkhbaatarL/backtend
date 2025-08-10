package com.sk.domain.admin.service.excel.collector;

import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.rmi.registry.LocateRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

@Slf4j
public class GlobalCrudeOilDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 6; // 헤더 행 인덱스
    private static final int HEADER_COLUMN_DATA_INDEX = 1; // 헤더 열 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 7; // 데이터 시작 행 인덱스
    private static final DateTimeFormatter KOREAN_FORMATTER = DateTimeFormatter.ofPattern("yy년MM월dd일");


    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes.subList(HEADER_COLUMN_DATA_INDEX, periodCodes.size()) : createDefaultHeaders(periodCodes.size());
            log.debug("Excel period codes: {}", periodCodes);
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;
                // 데이터 파싱
                Cell dateCell = row.getCell(0);
                if (dateCell == null)
                    continue;
                // 데이터 파싱
                LocalDate date = parseKoreanDate(dateCell.getStringCellValue());
                for (int i = HEADER_COLUMN_DATA_INDEX; i < row.getLastCellNum() && i - HEADER_COLUMN_DATA_INDEX < periodCodes.size(); i++) {
                    Cell valueCell = row.getCell(i);
                    if (valueCell == null)
                        continue;
                    String valueString = valueCell.getStringCellValue();
                    Double value = null;
                    if(valueString != null && !valueString.trim().isEmpty()){
                        value = Double.parseDouble(valueString);
                        if(value == 0d){
                            value = null;
                        }
                    }
                    result.add(new Object[] {date, periodCodes.get(i - HEADER_COLUMN_DATA_INDEX), value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            throw AppErrorException.of(REQ_INVALID_FORMAT, "이 데이터는 CSV 를 지원하지 않습니다.");
        }
        return result;
    }

    private static LocalDate parseKoreanDate(String input){
        return LocalDate.parse(input, KOREAN_FORMATTER);
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }
}
