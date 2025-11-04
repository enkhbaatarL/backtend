package com.sk.domain.admin.service.collector;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.collector.dto.SkEnsData;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.admin.service.json.DataCollectorJsonStrategy;
import com.sk.domain.shared.enums.ComponentType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.opensaml.xmlsec.signature.P;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SkEnsTypeBDataCollector implements DataCollectorStrategy, DataCollectorJsonStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int HEADER_COLUMN_DATA_INDEX = 1; // 헤더 열 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Set<LocalDate> TTF_HOLIDAYS;
    static{
        TTF_HOLIDAYS = Set.of(
            parseDate("2025-12-25"),
            parseDate("2025-12-26"),
            parseDate("2026-01-01"),
            parseDate("2026-04-03"),
            parseDate("2026-04-06"),
            parseDate("2026-05-04"),
            parseDate("2026-05-25"),
            parseDate("2026-08-31"),
            parseDate("2026-12-25"),
            parseDate("2026-12-28"),
            parseDate("2027-01-01")
        );
    }


    @Override
    public ComponentType getComponentType() {
        return ComponentType.SKENS_DATA_B;
    }

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
                LocalDate date = parseDate(dateCell.getStringCellValue());
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
                    result.add(new Object[] {date, getCode(periodCodes.get(i - HEADER_COLUMN_DATA_INDEX)), value});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            throw AppErrorException.of(REQ_INVALID_FORMAT, "이 데이터는 CSV 를 지원하지 않습니다.");
        }
        return result;
    }

    private String getCode(String input){
        return input;
        /*

        try{
            return CODE.valueOf(input).tbCode;
        } catch (IllegalArgumentException e){
            log.warn("No Defined Code. {}", input);
            return input;
        }
         */
    }

    private static LocalDate parseDate(String input){
        return LocalDate.parse(input, DATE_FORMATTER);
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
                .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> processJsonData(JsonNode data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if(data instanceof ArrayNode arrayNode){
            arrayNode.forEach(n->{
              try {
                SkEnsData.Code codeData = objectMapper.treeToValue(n, SkEnsData.Code.class);
                for(SkEnsData.Data rawData : codeData.data()){
                    result.add(new Object[]{
                        handleDateExceptionCase(codeData.code(), parseDate(rawData.date())),
                        codeData.code(),
                        Double.parseDouble(rawData.value())
                    });
                }
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
        }
        return result;
    }

    private LocalDate handleDateExceptionCase(String code, LocalDate date){
        if(!"TFMBUSMBTMc1".equalsIgnoreCase(code) && !"TFMBUSMBTMc2".equalsIgnoreCase(code)){
            return date;
        }

        return previousBusinessDay(TTF_HOLIDAYS);
    }

    /**
     * 기준일(LocalDate.now(zone))의 "영업일 기준 어제"를 구한다.
     * 예: 2025-11-03(월) 기준 → 2025-10-31(금)이 휴일/주말이면 그 전날로 계속 이동
     */
    public static LocalDate previousBusinessDay(ZoneId zoneId, Set<LocalDate> holidays) {
        return previousBusinessDay(LocalDate.now(zoneId), holidays);
    }

    /**
     * 기준일을 명시해서 "영업일 기준 어제"를 구한다.
     * 즉 baseDate.minusDays(1)에서 시작해 휴일/주말이면 계속 뒤로 간다.
     */
    private static LocalDate previousBusinessDay(LocalDate baseDate, Set<LocalDate> holidays) {
        // holidays가 null이면 빈 Set로 간주
        Set<LocalDate> hs = holidays == null ? Set.of() : holidays;

        LocalDate d = baseDate.minusDays(1);
        while (isHolidayOrWeekend(d, hs)) {
            d = d.minusDays(1);
        }
        return d;
    }

    /** d가 휴일 또는 주말(토/일)이면 true */
    private static boolean isHolidayOrWeekend(LocalDate d, Set<LocalDate> holidays) {
        DayOfWeek dow = d.getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.SATURDAY) || (dow == DayOfWeek.SUNDAY);
        boolean holiday = holidays.contains(d);
        return weekend || holiday;
    }

    // 편의 오버로드: 시스템 기본 타임존 사용
    public static LocalDate previousBusinessDay(Set<LocalDate> holidays) {
        return previousBusinessDay(ZoneId.systemDefault(), holidays);
    }
}
