package com.sk.domain.admin.service.excel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.ExcelParsedUtil;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CsvFileParsed implements FileTypeStrategy {
    private final DataCollectorStrategyResolver dataCollectorStrategyResolver;

    /**
     * CSV 파일 파싱
     * 정형 데이터 엑셀 파일 검수 후 데이터 저장
     * 현재 정책 : 엑셀 1행 => 칼럼, 2행부터 데이터 저장
     * => 필요에 따라 조건이 들어갈 수 있음 (몇번째가 칼럼이고, 몇번째가 데이터인지 조건)
     * 엑셀의 시트는 한개만 사용
     *
     * 테이블 칼럼 리스트(그래프를 그리기 위한 칼럼, tableColumnListNm)와, 엑셀 칼럼 리스트(excelColumnListNm) 명이 다를 수 있음
     * 칼럼명은 서로 1:1 대응 된다는 가정으로 작업하며 테이블 칼럼 리스트(tableColumnListNm) 순서와 엑셀 칼럼 리스트(excelColumnListNm) 순서가 같아야 함
     * 
     * @param file 엑셀 파일
     * @param componentInfo 컴포넌트 정보
     * @param headerRowIndex 헤더 행 인덱스 (기본값 0)
     * @param dataStartRowIndex 데이터 시작 행 인덱스 (기본값 1)
     * @param criteriaDt 조건 일자
     * @return 파싱된 데이터
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Override
    public List<Object[]> processFile(MultipartFile file, ComponentInfo componentInfo, int headerRowIndex, int dataStartRowIndex, String criteriaDt, String category)
            throws IOException {
        char delimiter = ExcelParsedUtil.detectDelimiter(file);
        InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setDelimiter(delimiter)
                .setTrim(true)
                .setAllowMissingColumnNames(true)
                .setHeader() // 첫 번째 행을 헤더로 설정
                .setSkipHeaderRecord(true) // 헤더 행 건너뛰기
                .get();
        CSVParser csvParser = CSVParser.parse(reader, csvFormat);
        try {
            DataCollectorStrategy strategy = dataCollectorStrategyResolver.resolve(componentInfo.getDataStructureConvertNm());
            List<String> headers = csvParser.getHeaderNames();
            if (headers == null || headers.isEmpty()) {
                log.warn("No headers found in CSV. Using componentInfo headers or default.");
                headers = componentInfo.getExcelTableColumnListNm() != null
                        ? Arrays.asList(componentInfo.getExcelTableColumnListNm().split(","))
                        : List.of("column_0"); // 최소 1개 컬럼 보장
            }
            log.debug("CSV headers: {}", headers);
            List<Object[]> result = strategy.processData(csvParser, headers, criteriaDt, category);
            csvParser.close();
            reader.close();
            return result;
        } catch (IOException e) {
            csvParser.close();
            reader.close();
            throw e;
        } catch (Exception e) {
            csvParser.close();
            reader.close();
            log.error("Failed to process CSV file: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Failed to process CSV file: " + e.getMessage());
        }
    }
}
