package com.sk.domain.admin.service.collector;

import static com.sk.common.enums.ResultCode.Error.REQ_INVALID_FORMAT;

import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.entity.master.tif.KplerCargo;
import com.sk.domain.shared.entity.master.tif.KplerCargo.CODE;
import com.sk.domain.shared.enums.ComponentType;
import com.sk.domain.shared.repository.master.tif.KplerCargoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KplerUsExportDataCollector implements DataCollectorStrategy {
    private static final int HEADER_ROW_INDEX = 0; // 헤더 행 인덱스
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스

    private final KplerCargoRepository kplerCargoRepository;

    @Override
    public ComponentType getComponentType() {
        return ComponentType.KPLER_US_EXPORT;
    }

    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns) {
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
                    result.add(addObject(rowData, CODE.USA_Export.name()));
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            throw AppErrorException.of(REQ_INVALID_FORMAT, "이 데이터는 CSV 를 지원하지 않습니다.");
        }
        Collections.reverse(result);
        return result;
    }

    public static Object[] addObject(Object[] original, Object newObj1) {
        Object[] newArray = Arrays.copyOf(original, original.length + 1);
        newArray[newArray.length-1] = newObj1;
        return newArray;
    }

    private List<String> createDefaultHeaders(int size) {
        return IntStream.range(0, Math.max(1, size))
            .mapToObj(i -> "P" + i) // 짧은 코드 (P0, P1, ...)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void postAction(List<Object[]> values) {
        KplerCargo kplerCargo = kplerCargoRepository.findFirstByCodeOrderByDateDesc(CODE.USA_Export.name());

        BigDecimal total = kplerCargoRepository.findAllByCodeAndDateBetween(
                CODE.USA_Export.name(),
                kplerCargo.getDate().withDayOfMonth(1),
                kplerCargo.getDate().plusMonths(1).withDayOfMonth(1))
            .stream()
            .map(KplerCargo::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        KplerCargo kplerCargoMtd = kplerCargoRepository.findFirstByCodeOrderByDateDesc(CODE.USA_Export_mtd.name());
        if(kplerCargoMtd.getDate().getYear() == kplerCargo.getDate().getYear()
                && kplerCargoMtd.getDate().getMonthValue() == kplerCargo.getDate().getMonthValue()){
            kplerCargoMtd.setValue(total);
            kplerCargoMtd.setDate(kplerCargo.getDate());
            kplerCargoRepository.save(kplerCargoMtd);
        }else{
            kplerCargoRepository.save(KplerCargo.builder()
                .date(kplerCargo.getDate())
                .code(CODE.USA_Export_mtd.name())
                .value(total)
                .build()
            );
        }
    }
}
