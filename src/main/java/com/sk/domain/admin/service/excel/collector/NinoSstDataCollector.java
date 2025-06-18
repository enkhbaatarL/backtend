package com.sk.domain.admin.service.excel.collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.sk.domain.admin.service.excel.DataCollectorStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NinoSstDataCollector implements DataCollectorStrategy {
    private static final List<String> NINO_REGIONS = Arrays.asList("NINO1+2", "NINO3", "NINO4", "NINO3.4"); // NINO 지역 목록
    private static final int DEFAULT_DATA_START_ROW_INDEX = 1; // 데이터 시작 행 인덱스

    /**
     * 엑셀 파일 파싱
     * NINO SST 데이터를 수집
     * 
     * @param data 엑셀 파일 데이터
     * @param actualColumns 실제 컬럼 목록
     * @param criteriaDt 조건 일자
     * @return 변환된 데이터 리스트 (YR, MON, sst_pstn_spt_cd, sst_idx, sst_avg_dvat)
     */
    @Override
    public List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category) {
        List<Object[]> result = new ArrayList<>();
        if (data instanceof Sheet) { // 엑셀 파일 파싱
            Sheet sheet = (Sheet) data;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // 데이터 시작 행 인덱스 조건
                if (row.getRowNum() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;
                // 데이터 파싱
                Cell yrCell = row.getCell(0);
                Cell monCell = row.getCell(1);
                if (yrCell == null || monCell == null)
                    continue;
                // 데이터 파싱
                String yr = String.valueOf((int) yrCell.getNumericCellValue());
                String mon = String.valueOf((int) monCell.getNumericCellValue());
                for (int i = 0; i < NINO_REGIONS.size(); i++) {
                    
                    Cell sstCell = row.getCell(2 + i * 2);
                    Cell anomCell = row.getCell(3 + i * 2);
                    if (sstCell == null || anomCell == null)
                        continue;
                    
                    double sstValue = sstCell.getNumericCellValue();
                    String anomValue = String.valueOf(anomCell.getNumericCellValue());
                    String sstIdx = String.valueOf((int) (sstValue * 100));
                    result.add(new Object[] {yr, mon, NINO_REGIONS.get(i), sstIdx, anomValue});
                }
            }
        } else if (data instanceof CSVParser) { // CSV 파일 파싱
            CSVParser csvParser = (CSVParser) data;
            for (CSVRecord record : csvParser) {
                // 데이터 시작 행 인덱스 조건
                if (record.getRecordNumber() < DEFAULT_DATA_START_ROW_INDEX)
                    continue;
                // 데이터 파싱
                String yr = record.get(0);
                String mon = record.get(1);
                if (yr == null || mon == null)
                    continue;
                // 데이터 파싱
                for (int i = 0; i < NINO_REGIONS.size(); i++) {
                    String sstValue = record.get(2 + i * 2);
                    String anomValue = record.get(3 + i * 2);
                    if (sstValue == null || anomValue == null)
                        continue;
                    String sstIdx = String.valueOf((int) (Double.parseDouble(sstValue) * 100));
                    result.add(new Object[] {yr, mon, NINO_REGIONS.get(i), sstIdx, anomValue});
                }
            }
        }
        return result;
    }
}