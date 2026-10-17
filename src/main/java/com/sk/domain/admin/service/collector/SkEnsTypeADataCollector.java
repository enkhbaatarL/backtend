package com.sk.domain.admin.service.collector;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.ImportService;
import com.sk.domain.admin.service.collector.dto.SkEnsDataAType;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkEnsTypeADataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final List<String> dateColumns = List.of("trade_date","strip","expiration_date");

    private final ImportService importService;

    // ,code,gas_in_storage_mt

    @Override
    public ComponentType getComponentType() {
        return ComponentType.SKENS_DATA_A;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet sheet) { // 엑셀 파일 파싱
            Iterator<Row> rowIterator = sheet.iterator();
            // 헤더 파싱
            List<String> periodCodes = ExcelParsedUtil.extractExcelHeader(sheet, HEADER_ROW_INDEX);
            periodCodes = periodCodes.size() > 1 ? periodCodes : createDefaultHeaders(periodCodes.size());
            log.debug("Excel period codes: {}", periodCodes);

            List<Integer> dateIndex = new ArrayList<>();
            for( int i = 0; i<actualColumns.size(); i++){
                if(dateColumns.contains(actualColumns.get(i))) dateIndex.add(i);
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;

                Object[] rowData = ExcelParsedUtil.parseExcelRow(row, actualColumns, null);
                rowData = transformAtIndices(rowData, dateIndex);
                if (rowData.length > 0) {
                  result.add(removeAtIndex(rowData,6));
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

    public static Object[] removeAtIndex(Object[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            throw new IllegalArgumentException("Invalid index");
        }
        Object[] result = new Object[array.length - 1];
        // 앞쪽 복사
        System.arraycopy(array, 0, result, 0, index);
        // 뒤쪽 복사
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    public static Object[] transformAtIndices(Object[] array, List<Integer> indices) {
        if (array == null) throw new IllegalArgumentException("array is null");
        for (Integer index : indices) {
          if (index < 0 || index >= array.length) {
            throw new IllegalArgumentException("Invalid index: " + index);
          }
        }

        // 원본 배열 복사
        Object[] result = Arrays.copyOf(array, array.length);
        for (int idx : indices) {
            result[idx] = parseDate((String)result[idx]);
        }

        return result;
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }

    private enum CONTRACT{
        H("NGc"), // Henry Hub
        B("LCOc"), // Brent
        T("CLc"); // WTI
        String prefix;
        CONTRACT(String prefix){
            this.prefix = prefix;
        }
    }

    @Override
    @Transactional
    public void postAction(List<Object[]> values){

        log.debug("SK ENS Post Action Start.");

        final List<SkEnsDataAType> hhList = new ArrayList<>();
        final List<SkEnsDataAType> brentList = new ArrayList<>();
        final List<SkEnsDataAType> wtiList = new ArrayList<>();

        values.stream().filter(v->{
            String contract = (String)v[4];
            for(CONTRACT c : CONTRACT.values()){
                if(contract.equals(c.name())){
                    return true;
                }
            }
            return false;
        }).map(
            v->{
                LocalDate date = (LocalDate)v[0];
                LocalDate strip = (LocalDate)v[3];
                String contract = (String)v[4];
                BigDecimal settlement = null;
                if(v[6] instanceof BigDecimal value){
                    settlement = value;
                }else if(v[6] instanceof Integer value){
                    settlement = BigDecimal.valueOf(value);
                }else if(v[6] instanceof Double value){
                    settlement = BigDecimal.valueOf(value);
                }else if(v[6] instanceof String value){
                    settlement = new BigDecimal(value);
                }
                return new SkEnsDataAType(date, strip, contract, settlement);
            }
        ).forEach(d->{
            if(d.contract().equals(CONTRACT.T.name())){
                wtiList.add(d);
            }else if(d.contract().equals(CONTRACT.H.name())){
                hhList.add(d);
            }else if(d.contract().equals(CONTRACT.B.name())){
                brentList.add(d);
            }
        });

        List<Object[]> result = new ArrayList<>();
        int order = 1;
        for(SkEnsDataAType t: wtiList.stream().sorted(Comparator.comparing(w->{return w.strip().toEpochDay();})).toList()){
            result.add(new Object[]{CONTRACT.valueOf(t.contract()).prefix + order, t.date(), t.settlement()});
            if(order >= 4){
                break;
            }
            order++;
        }
        order = 1;
        for(SkEnsDataAType t: hhList.stream().sorted(Comparator.comparing(w->{return w.strip().toEpochDay();})).toList()){
            result.add(new Object[]{CONTRACT.valueOf(t.contract()).prefix + order, t.date(), t.settlement()});
            if(order >= 4){
                break;
            }
            order++;
        }
        order = 1;
        for(SkEnsDataAType t: brentList.stream().sorted(Comparator.comparing(w->{return w.strip().toEpochDay();})).toList()){
            result.add(new Object[]{CONTRACT.valueOf(t.contract()).prefix + order, t.date(), t.settlement()});
            if(order >= 4){
                break;
            }
            order++;
        }

        log.debug("Data Update : {}", result.size());

        importService.saveBatchData("midb-skens", List.of("code", "date", "value"), result, "tif_skens_type_b");
        log.debug("SK ENS Post Action End.");
    }
}
