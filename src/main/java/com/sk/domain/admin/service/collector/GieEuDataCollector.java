package com.sk.domain.admin.service.collector;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GieEuDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int HEADER_COLUMN_GAS_IN_STORAGE = 2;
    private static final String CODE = "CURRENT";
    public static final BigDecimal MT_SCALE_RATIO = BigDecimal.valueOf(0.074d);

    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ,code,gas_in_storage_mt

    @Override
    public ComponentType getComponentType() {
        return ComponentType.GIE_EU;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes : createDefaultHeaders(periodCodes.size());
            log.debug("Excel period codes: {}", periodCodes);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                Object[] rowData = ExcelParsedUtil.parseExcelRow(row, actualColumns, null);

                if (rowData.length > 0) {
                  Object gasInStorage = rowData[HEADER_COLUMN_GAS_IN_STORAGE];
                  if(gasInStorage instanceof Integer gasInStorageInt){
                      gasInStorage = BigDecimal.valueOf(gasInStorageInt).multiply(MT_SCALE_RATIO);
                  }else if(gasInStorage instanceof BigDecimal gasInStorageBigDecimal){
                      gasInStorage = gasInStorageBigDecimal.multiply(MT_SCALE_RATIO);
                  }else{
                      gasInStorage = null;
                  }
                  result.add(addTwoObject(rowData, CODE, gasInStorage));
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            throw AppErrorException.of(REQ_INVALID_FORMAT, "이 데이터는 CSV 를 지원하지 않습니다.");
        }
        Collections.reverse(result);
        return result;
    }

    private static LocalDate parseDate(String input){
        return LocalDate.parse(input, DATE_FORMATTER);
    }

    public static Object[] addTwoObject(Object[] original, Object newObj1, Object newObj2) {
        Object[] newArray = Arrays.copyOf(original, original.length + 2);
        newArray[newArray.length-2] = newObj1;
        newArray[newArray.length-1] = newObj2;
        return newArray;
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }
}
