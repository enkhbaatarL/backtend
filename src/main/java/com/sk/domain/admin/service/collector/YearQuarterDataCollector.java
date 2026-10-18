package com.sk.domain.admin.service.collector;

import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YearQuarterDataCollector implements DataCollectorStrategy {

    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스

    @Override
    public ComponentType getComponentType() {
        return ComponentType.YEAR_QUARTER_ADD;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet sheet) { // 엑셀 파일 파싱
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = addObjects(ExcelParsedUtil.parseExcelRow(row, actualColumns, criteriaDt), conditionMap, dbColumns);
                if (rowData.length > 0)
                    result.add(rowData);
            }
        } else if (data instanceof CSVParser csvParser) { // CSV 파일 파싱
            for (CSVRecord record : csvParser) {
                // 데이터 시작 행 인덱스 조건
                if (record.getRecordNumber() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                // 데이터 파싱
                Object[] rowData = addObjects(ExcelParsedUtil.parseCsvRecord(record, actualColumns, criteriaDt), conditionMap, dbColumns);
                if (rowData.length > 0)
                    result.add(rowData);
            }
        }
        return result;
    }

    private static Object[] addQuarterDate(Object[] input) {
        // 1. 연도 파싱
        int year = Integer.parseInt(input[0].toString());

        // 2. "YYYY Qn" 에서 Qn 추출
        String quarterStr = input[1].toString();  // 예: "2025 Q1"
        int quarter = Integer.parseInt(quarterStr.split("Q")[1]);

        // 3. 분기 → 월 계산
        int month = switch (quarter) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            default -> throw new IllegalArgumentException("Invalid quarter: " + quarter);
        };

        // 4. LocalDateTime 생성
        LocalDateTime date = LocalDateTime.of(year, month, 15, 0, 0);

        // 5. 기존 배열 앞에 date 추가하여 새 배열 생성
        Object[] result = new Object[input.length + 1];
        result[0] = date;
        System.arraycopy(input, 0, result, 1, input.length);

        return result;
    }

    private static Object[] addObjects(Object[] originSource, Map<String, String> addData, List<String> dbColumns){
        // 1. 유효성 검사 (source가 dbColumns보다 크면 에러 혹은 그대로 반환 처리 필요)
        if (originSource == null || dbColumns == null) return originSource;

        Object[] source = addQuarterDate(originSource);

        int currentSize = source.length;
        int targetSize = dbColumns.size();

        // source가 이미 dbColumns보다 크거나 같다면 추가 작업 없이 반환 (상황에 따라 예외처리 가능)
        if (currentSize >= targetSize) return source;

        // 2. 새로운 배열 생성 (dbColumns 크기만큼)
        Object[] result = new Object[targetSize];

        // 3. 기존 source 데이터 복사 (0번 인덱스부터 source 길이만큼)
        System.arraycopy(source, 0, result, 0, currentSize);

        // 4. 나머지 부분 채우기
        // source의 길이부터 dbColumns의 끝까지 순회
        for (int i = currentSize; i < targetSize; i++) {
            String columnName = dbColumns.get(i); // 해당 인덱스의 컬럼명 가져오기

            // addData 맵에 해당 컬럼명이 key로 존재하면 값을 넣고, 없으면 null (기본값)
            if (addData != null && addData.containsKey(columnName)) {
                result[i] = addData.get(columnName);
            } else {
                result[i] = null; // 매칭되는 키가 없을 경우 처리
            }
        }

        return result;
    }
}
