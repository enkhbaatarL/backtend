package com.sk.domain.admin.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pjfanning.xlsx.StreamingReader;
import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.request.ExcelReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataSaveAll;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.enums.ComponentType;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService {

    private final int DEFAULT_HEADER_ROW_INDEX = 0; // 기본 엑셀 헤더 행 인덱스 (파일에서 1행 => 인덱스 0)
    private final int DEFAULT_DATA_START_ROW_INDEX = 1; // 기본 엑셀 데이터 시작 행 인덱스 (파일에서 2행 => 인덱스 1)
    private final int MAX_CONSECUTIVE_EMPTY_ROWS = 5; // 최대 허용 빈 행 수 (엑셀 검수 시 연속 빈 행 수가 5개 이상이면 종료)
    private final String CRITERIA_DT_COLUMN_NAME = "crtr_dt"; // 기준일자 칼럼명 (엑셀에는 없지만 테이블에는 저장이 필요할 때 사용, 나중에 동적으로 받아야 한다면 수정 필요)

    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    // todo: 시트 2개 이상에서 데이타 담아서 내려주고 읽어오기
    /**
     * 엑셀 파일 다운로드
     * 
     * @param response
     * @param datas
     * @return
     * @throws IOException
     */
    public ByteArrayResource downloadExcelFile(HttpServletResponse response, List<List<ExcelReq>> datas)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {

            int sheetIndex = 0;
            for (List<ExcelReq> list : datas) {
                String sheetName = String.format("SheetName_%d", ++sheetIndex);

                Sheet sheet = workbook.createSheet(sheetName);

                Row headerRow = (Row) sheet.createRow(0);
                Cell headerCell1 = headerRow.createCell(0);
                headerCell1.setCellValue("이름");

                Cell headerCell2 = headerRow.createCell(1);
                headerCell2.setCellValue("레벨");

                IntStream.range(0, list.size())
                        .forEach(x -> {
                            ExcelReq data = list.get(x);

                            Row dataRow1 = sheet.createRow(x + 1);
                            Cell dataCell1 = dataRow1.createCell(0);
                            dataCell1.setCellValue(data.getName());

                            Cell dataCell2 = dataRow1.createCell(1);
                            dataCell2.setCellValue(data.getGrade());
                        });
            }
            String filename = "sample.xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    filename + "\"");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            return new ByteArrayResource(out.toByteArray());
        }
    }

    /**
     * 엑셀 파일 업로드
     * 
     * @param file
     * @throws IOException
     */
    public void uploadExcelFile(MultipartFile file) throws IOException {
        // HSSFWorkbook
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    // 엑셀의 A열(0번 셀) = name, B열(1번 셀) = grade 라고 가정
                    Cell nameCell = row.getCell(0);
                    Cell gradeCell = row.getCell(1);

                    log.info("-> {}, {}", nameCell, gradeCell);

                    String name = (nameCell == null) ? "" : nameCell.getStringCellValue();
                    String grade = (gradeCell == null) ? null : gradeCell.getStringCellValue();

                    log.info("{}, {}", name, grade);
                }
            }

        } catch (

        IOException e) {
            log.error("IOException", e);
            // 파일 처리를 실패한 경우
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            // .body("엑셀 파일을 읽는 도중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception", e);
            // 그 외 처리에서 발생할 수 있는 예외
            // return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            // .body("엑셀 데이터 처리 중 예외가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 컴포넌트 정보에 따른 정형 데이터 저장
     * 테이블 칼럼 리스트(그래프를 그리기 위한 칼럼, tableColumnListNm)와, 엑셀 칼럼 리스트(excelColumnListNm) 명이 다를 수 있음
     * 칼럼명은 서로 1:1 대응 된다는 가정으로 작업하며 테이블 칼럼 리스트(tableColumnListNm) 순서와 엑셀 칼럼 리스트(excelColumnListNm) 순서가 같아야 함
     * 
     * @param file
     * @param componentInfo
     * @param criteriaDt 기준일자 (엑셀에는 없지만 DB 테이블에는 저장이 필요할 때 따로 받는다, 현재는 crtr_dt 칼럼에만 저장한다)
     * @return
     * @throws IOException
     */
    @Transactional
    public boolean uploadStructuredData(MultipartFile file, ComponentInfo componentInfo, String criteriaDt) throws IOException {
        if (componentInfo.getTargetDbNm() == null || componentInfo.getTargetDbNm().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Target database name is null or blank");
        }
        try {
            String columnName = componentInfo.getExcelTableColumnListNm();

            if (columnName == null || columnName.isBlank()) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Column name is null or blank");
            }
            List<String> columnNames = Arrays.stream(columnName.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));

            int headerRowIndex = DEFAULT_HEADER_ROW_INDEX;
            int dataStartRowIndex = DEFAULT_DATA_START_ROW_INDEX;

            // 컴포넌트 조건절에 HEADER_ROW_INDEX, DATA_START_ROW_INDEX 값 있는지 확인
            // 조건절 형식 : HEADER_ROW_INDEX=1;DATA_START_ROW_INDEX=2 (엑셀파일의 1행이면 )
            // : 입력 예시 : HEADER_ROW_INDEX=0;DATA_START_ROW_INDEX=1;
            if (componentInfo.getExcelConditionCntt() != null && !componentInfo.getExcelConditionCntt().isBlank()) {
                String[] condition = componentInfo.getExcelConditionCntt().split(";");
                for (String cond : condition) {
                    if (cond.contains("HEADER_ROW_INDEX") || cond.contains("DATA_START_ROW_INDEX")) {
                        String[] keyValue = cond.split("=");
                        if (keyValue[0].equals("HEADER_ROW_INDEX")) {
                            int conditionHeaderRowIndex = Integer.parseInt(keyValue[1]); // 엑셀파일의 1행이면 0행
                            if (conditionHeaderRowIndex <= 0) {
                                headerRowIndex = 0; // headerRowIndex의 기본값은 0
                            } else {
                                headerRowIndex = conditionHeaderRowIndex - 1;
                            }
                        }
                        if (keyValue[0].equals("DATA_START_ROW_INDEX")) {
                            int conditionDataStartRowIndex = Integer.parseInt(keyValue[1]); // 엑셀파일의 2행이면 1행
                            if (conditionDataStartRowIndex <= 1) {
                                dataStartRowIndex = 1; // dataStartRowIndex의 기본값은 1
                            } else {
                                dataStartRowIndex = conditionDataStartRowIndex - 1;
                            }
                        }
                    }
                }
            }

            return uploadStructuredExcelData(file, componentInfo.getTargetDbNm(), componentInfo.getTargetTableNm(), componentInfo.getDataStructureConvertNm(), columnNames, headerRowIndex,
                    dataStartRowIndex, criteriaDt);

        } catch (Exception e) {
            log.error("Exception", e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    /**
     * 정형 데이터 엑셀 파일 검수 후 데이터 저장
     * 현재 정책 : 엑셀 1행 => 칼럼, 2행부터 데이터 저장
     *          => 필요에 따라 조건이 들어갈 수 있음 (몇번째가 칼럼이고, 몇번째가 데이터인지 조건)
     *          엑셀의 시트는 한개만 사용
     * 
     *          테이블 칼럼 리스트(그래프를 그리기 위한 칼럼, tableColumnListNm)와, 엑셀 칼럼 리스트(excelColumnListNm) 명이 다를 수 있음
     *          칼럼명은 서로 1:1 대응 된다는 가정으로 작업하며 테이블 칼럼 리스트(tableColumnListNm) 순서와 엑셀 칼럼 리스트(excelColumnListNm) 순서가 같아야 함
     * 
     * @param file 엑셀 파일
     * @param tartgetDataSource 대상 데이터베이스 이름
     * @param tableColumns 테이블 칼럼 리스트
     * @param expectedColumnNames 기대되는 칼럼명
     * @param mappingColumnMap 엑셀 칼럼명과 Insert 테이블 칼럼명이 다를 때 칼럼명 매핑하기 위한 MAP
     * @param tableName 테이블 이름
     * @param headerRowIndex 헤더 행 인덱스 (기본값 0)
     * @param dataStartRowIndex 데이터 시작 행 인덱스 (기본값 1)
     * @return 저장 성공 여부
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public boolean uploadStructuredExcelData(MultipartFile file, String tartgetDataSource, String tableName, ComponentType componentType, List<String> actualColumns,
            int headerRowIndex, int dataStartRowIndex, String criteriaDt) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "File name is null");
            }

            // 데이터 수집
            List<Object[]> values;

            // 파일 확장자 검증
            String extension = fileName.toLowerCase();
            // Excel Data 파싱
            if (extension.endsWith(".csv")) {
                // CSV 처리
                // TODO: 타입에 따른 분기 처리 필요 => 클래스 분리 + 전략패턴 적용 필요
                switch (componentType) {
                    case PIVOT:
                        values = collectPivotCsvData(file);
                        break;
                    case CPC_NINO_SST:
                        values = collectNinoRegionsSstCsvData(file, headerRowIndex, dataStartRowIndex);
                        break;
                    default:
                        values = processCsvData(file, headerRowIndex, dataStartRowIndex, criteriaDt);
                }
            } else if (extension.endsWith(".xlsx") || extension.endsWith(".xls")) {
                // xlsx xls 처리
                // TODO: 타입에 따른 분기 처리 필요
                switch (componentType) {
                    case PIVOT:
                        values = collectPivotExcelData(file, headerRowIndex, dataStartRowIndex);
                        break;
                    case CPC_NINO_SST:
                        values = collectNinoRegionsSstExcelData(file, headerRowIndex, dataStartRowIndex);
                        break;
                    default:
                        values = collectExcelData(file, actualColumns, dataStartRowIndex, criteriaDt);
                }
            } else {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported file type: " + fileName);
            }

            if (criteriaDt != null) {
                actualColumns.add(CRITERIA_DT_COLUMN_NAME);
            }

            // 공통 DB 배치 삽입
            return saveBatchData(tartgetDataSource, actualColumns, values, tableName);
        } catch (Exception e) {
            log.error("Exception during upload: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * 엑셀 파일의 구조를 검증하고 헤더 칼럼을 반환
     *
     * @param file 업로드된 엑셀 파일
     * @param expectedColumnNames 기대되는 칼럼명
     * @return 실제 칼럼 이름 리스트
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public List<String> validateExcelStructure(MultipartFile file, String[] expectedColumnNames, int headerRowIndex) {
        Workbook workbook = null;
        try (InputStream is = file.getInputStream()) {

            String fileName = file.getOriginalFilename();

            // 파일 확장자에 따라 처리 분기
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(is);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(is);
            } else {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported Excel file type: " + fileName);
            }

            // 1. 엑셀 확장자 파일 검증
            validateExcelFile(file);

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 1. 엑셀이 비어있는지 확인
            if (!rowIterator.hasNext()) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Excel file is empty");
            }

            // 2. 헤더 행까지 이동
            Row headerRow = null;
            for (int i = 0; i <= headerRowIndex && rowIterator.hasNext(); i++) {
                headerRow = rowIterator.next();
            }
            if (headerRow == null) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Header row not found at index " + headerRowIndex);
            }

            List<String> actualColumns = extractColumnNames(headerRow);

            // 3. 기대 칼럼과 실제 칼럼 검증
            validateColumns(actualColumns, expectedColumnNames);

            /**
             * 나머지 행 데이터 수집(데이터 검증 로직, 검증만 필요시 행 데이터를 검증하는 로직 사용)
                List<Map<String, String>> data = new ArrayList<>();
                while (rowIterator.hasNext()) { //TODO: 끊어진 행이 있으면 문제 발생 함 => 처리 필요
                    Row row = rowIterator.next();
                    if (row.getLastCellNum() > 0) { // 빈 행 제외
                        Map<String, String> rowData = parseRowToMap(row, actualColumns);
                        data.add(rowData);
                    }
                }
                return data; // 모든 데이터 반환
             */

            return actualColumns;
        } catch (Exception e) {
            log.error("Exception", e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("Failed to close workbook: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 엑셀 파일에서 데이터를 수집
     * 현 정책은 시트는 1개라고 가정
     *
     * @param file 업로드된 엑셀 파일
     * @param actualColumns 헤더 칼럼 이름 리스트
     * @return 수집된 데이터
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    private List<Object[]> collectExcelData(MultipartFile file, List<String> actualColumns, int dataStartRowIndex, String criteriaDt) {
        Workbook workbook = null;
        try (InputStream is = file.getInputStream()) {
            String fileName = file.getOriginalFilename();

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(is);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(is);
            } else {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Unsupported Excel file type: " + fileName);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 데이터 시작 행까지 이동
            for (int i = 0; i < dataStartRowIndex && rowIterator.hasNext(); i++) {
                rowIterator.next();
            }

            // 데이터 수집
            List<Object[]> data = new ArrayList<>();
            int consecutiveEmptyRows = 0; // 연속 빈 행 수

            // 5행 이상 연속 빈 행이면 종료
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getLastCellNum() > 0) { // 데이터가 있는 행
                    Object[] rowData = parseRowToArray(row, actualColumns, criteriaDt);
                    if (rowData.length == 0) {
                        log.warn("Parsed empty row data for row: {}", row.getRowNum());
                    } else {
                        data.add(rowData);
                        consecutiveEmptyRows = 0; // 데이터가 있으면 카운터 초기화
                    }
                } else {
                    // 빈 행 처리
                    consecutiveEmptyRows++;
                    log.debug("Empty row detected at row: {}, consecutive empty rows: {}", row.getRowNum(), consecutiveEmptyRows);
                    if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                        log.info("Terminating data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);

                        // 데이터 수집 종료
                        break;
                    }
                }
            }

            return data;
        } catch (Exception e) {
            log.error("Exception during data collection: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("Failed to close workbook: {}", e.getMessage());
                }
            }
        }
    }

    /**
    * 업로드된 파일이 엑셀 형식인지 검증
    * 
    * @param file 업로드된 파일
    * @throws AppErrorException 엑셀 파일이 아닌 경우
    */
    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "File is null or empty");
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (fileName == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "File name is null");
        }

        // 확장자 검증
        if (!fileName.matches("(?i).+\\.(xlsx|xls|csv)$")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                    "Invalid file extension. Only .xlsx, .xls, and .csv are allowed. File: " + fileName);
        }

        // MIME 타입 검증
        if (contentType == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "Content type is null");
        }

        boolean isValidMimeType = contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || // xlsx
                contentType.equals("application/vnd.ms-excel") || // xls
                contentType.equals("text/csv") || // csv
                contentType.equals("text/plain"); // csv (일부 클라이언트에서)

        if (!isValidMimeType) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                    "Invalid file type. Expected Excel or CSV file, but got: " + contentType);
        }
    }

    /**
     * 헤더 행에서 칼럼명 추출
     * 
     * @param headerRow
     * @return
     */
    private List<String> extractColumnNames(Row headerRow) {
        List<String> columnNames = new ArrayList<>();
        for (Cell cell : headerRow) {
            columnNames.add(cell.getStringCellValue().trim());
        }
        return columnNames;
    }

    /**
     * 칼럼명 검증
     * 
     * @param actualColumns
     * @param expectedColumns
     */
    private void validateColumns(List<String> actualColumns, String[] expectedColumns) {
        Set<String> expectedSet = Arrays.stream(expectedColumns).map(String::trim).collect(Collectors.toSet());
        Set<String> actualSet = new HashSet<>(actualColumns);

        // actualColumns에만 있는 칼럼(엑셀 칼럼에만 있는 경우) 체크
        List<String> extra = actualSet.stream()
                .filter(col -> !expectedSet.contains(col))
                .collect(Collectors.toList());

        if (!extra.isEmpty()) {
            String errorMessage = "Column mismatch: Extra columns found in Excel: " + extra;
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, errorMessage);
        }
    }

    /**
     * 행 데이터 파싱
     * 
     * @param row
     * @param columnNames
     * @param criteriaDt
     * @return
     */
    private Object[] parseRowToArray(Row row, List<String> columnNames, String criteriaDt) {
        int arraySize = criteriaDt == null ? columnNames.size() : columnNames.size() + 1;
        Object[] rowData = new Object[arraySize];

        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            Object value = null;

            if (cell == null || cell.getCellType() == CellType.BLANK) {
                value = null;
            } else {
                switch (cell.getCellType()) {
                    case STRING:
                        value = cell.getStringCellValue(); // VARCHAR
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            value = cell.getDateCellValue(); // DATETIME
                        } else {
                            double numValue = cell.getNumericCellValue();
                            if (numValue == Math.floor(numValue)) {
                                value = (int) numValue; // INT (소수점 없으면)
                            } else {
                                value = BigDecimal.valueOf(numValue); // DECIMAL (소수점 있으면)
                            }
                        }
                        break;
                    case BOOLEAN:
                        value = cell.getBooleanCellValue(); // DB에 BOOLEAN 매핑 가능
                        break;
                    default:
                        value = cell.toString(); // 기본 VARCHAR
                }
            }
            rowData[i] = value;
        }

        // criteriaDt가 null이 아니면 마지막에 값 추가
        if (criteriaDt != null) {
            rowData[columnNames.size()] = criteriaDt;
        }
        return rowData;
    }

    /**
     * CSV 데이터 처리 (데이터 수집)
     *
     * @param file
     * @param headerRowIndex
     * @param dataStartRowIndex
     * @param criteriaDt
     * @return List<Object[]> 데이터 리스트
     * @throws IOException
     */
    private List<Object[]> processCsvData(MultipartFile file, int headerRowIndex, int dataStartRowIndex, String criteriaDt)
            throws IOException {
        // 구분자 감지
        char delimiter = detectDelimiter(file);
        log.info("Detected CSV delimiter: '{}'", delimiter == ',' ? "comma" : delimiter == ';' ? "semicolon" : delimiter == '\t' ? "tab" : delimiter);

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser csvParser = CSVParser.parse(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT)
                        .setDelimiter(delimiter)
                        .setTrim(true)
                        .setAllowMissingColumnNames(true)
                        .get())) {

            List<Object[]> data = new ArrayList<>();
            int currentRow = 0;
            int consecutiveEmptyRows = 0;
            int columnCount = -1; // 첫 번째 데이터 행에서 열 개수를 동적으로 결정

            for (CSVRecord record : csvParser) {
                // 데이터 행 처리
                if (currentRow >= dataStartRowIndex) {
                    if (record.size() > 0) {
                        // 첫 번째 데이터 행에서 열 개수 결정
                        if (columnCount == -1) {
                            columnCount = record.size(); // 첫 데이터 행의 열 개수로 고정
                            log.debug("Determined column count from first data row: {}", columnCount);
                        }

                        // 열 개수에 따라 데이터 배열 크기 결정
                        int arraySize = criteriaDt == null ? columnCount : columnCount + 1;
                        Object[] rowData = new Object[arraySize];
                        for (int i = 0; i < columnCount; i++) {
                            String value = i < record.size() ? record.get(i) : null;
                            rowData[i] = value != null && !value.trim().isEmpty() ? value.trim() : null;
                        }
                        // criteriaDt가 null이 아니면 마지막에 값 추가
                        if (criteriaDt != null) {
                            rowData[columnCount] = criteriaDt;
                        }
                        data.add(rowData);
                        consecutiveEmptyRows = 0;
                    } else {
                        consecutiveEmptyRows++;
                        log.debug("Empty row detected, consecutive empty rows: {}", consecutiveEmptyRows);
                        if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                            log.info("Terminating data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);
                            break;
                        }
                    }
                }
                currentRow++;
            }

            if (data.isEmpty()) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "No data found starting at index " + dataStartRowIndex);
            }
            return data;

        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * 공통 DB 배치 삽입 로직
     * 
     * @param tartgetDataSource 대상 데이터베이스 소스
     * @param columns 테이블 칼럼명 리스트
     * @param values 데이터 리스트
     * @param tableName 테이블 이름
     * @return
     */
    private boolean saveBatchData(String tartgetDataSource, List<String> columns, List<Object[]> values, String tableName) {
        int batchSize = 500;
        int totalRows = values.size();
        int batchCount = (totalRows + batchSize - 1) / batchSize;

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, totalRows);
            List<Object[]> batchData = values.subList(fromIndex, toIndex);

            mybatisDynamicQueryService.saveAllDynamicData(DynamicDataSaveAll.of(tartgetDataSource, tableName, columns, batchData));
        }
        return Boolean.TRUE;
    }

    // 비표준 문자 제거
    private String cleanHeader(String header) {
        if (header == null) {
            return "";
        }
        // BOM, 공백, 줄바꿈, 비표준 공백 제거
        String cleaned = header.replaceAll("^[\\uFEFF\\u200B\\u00A0]+", "") // BOM, Zero-width space, NBSP 제거
                .replaceAll("[\\r\\n\\t\\u00A0]+", "") // 줄바꿈, 탭, NBSP 제거
                .trim(); // 양쪽 공백 제거
        if (cleaned.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Header cleaned to empty string. Original: '" + header + "'");
        }
        return cleaned;
    }

    private char detectDelimiter(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return ',';
            }
            // BOM 제거 후 구분자 감지
            firstLine = firstLine.replaceAll("^[\\uFEFF\\u200B]", "");
            int commaCount = firstLine.split(",").length;
            int semicolonCount = firstLine.split(";").length;
            int tabCount = firstLine.split("\t").length;
            if (semicolonCount > commaCount && semicolonCount > tabCount) {
                return ';';
            }
            if (tabCount > commaCount && tabCount > semicolonCount) {
                return '\t';
            }
            return ',';
        }
    }

    private List<Object[]> collectPivotExcelData(MultipartFile file, int headerRowIndex, int dataStartRowIndex) {
        Workbook workbook = null;
        List<Object[]> result = new ArrayList<>();
        List<String> periodCodes = new ArrayList<>();
        String headerFirstCell = null; // 헤더의 첫 번째 셀 값 저장 (칼럼 명이 여러번 들어오는 파일에 대한 예외 처리용)

        try (InputStream inputStream = file.getInputStream()) {
            String fileName = file.getOriginalFilename();

            // 엑셀 파일 형식에 따라 Workbook 생성
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(inputStream);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("Unsupported Excel file type: " + fileName);
            }

            // 첫 번째 시트 가져오기
            Sheet sheet = workbook.getSheetAt(0);

            // iterator를 사용하여 행 순회
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new IllegalStateException("Excel sheet is empty");
            }

            // 헤더 행까지 이동
            int currentRow = 0;
            Row headerRow = null;
            while (rowIterator.hasNext() && currentRow <= headerRowIndex) {
                headerRow = rowIterator.next();
                currentRow++;
            }

            if (headerRow == null) {
                throw new IllegalStateException("Header row not found at index " + headerRowIndex);
            }

            // 헤더 행에서 PeriodCode 추출
            headerFirstCell = headerRow.getCell(0) != null ? headerRow.getCell(0).getStringCellValue().trim() : null;
            for (int colIndex = 1; colIndex < headerRow.getLastCellNum(); colIndex++) {
                Cell headerCell = headerRow.getCell(colIndex);
                if (headerCell != null) {
                    periodCodes.add(headerCell.getStringCellValue().trim());
                }
            }

            // 데이터 시작 행까지 이동
            while (rowIterator.hasNext() && currentRow < dataStartRowIndex) {
                rowIterator.next();
                currentRow++;
            }

            // 데이터 행 처리
            int consecutiveEmptyRows = 0;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row == null || row.getLastCellNum() <= 1) { // 빈 행 체크
                    consecutiveEmptyRows++;
                    log.debug("Empty row detected, consecutive empty rows: {}", consecutiveEmptyRows);
                    if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                        log.info("Terminating data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);
                        break;
                    }
                    continue;
                }

                // 헤더 행인지 확인 (첫 번째 셀이 "Year"와 동일한 경우)
                Cell firstCell = row.getCell(0);
                if (firstCell != null && firstCell.getCellType() == CellType.STRING &&
                        firstCell.getStringCellValue().trim().equals(headerFirstCell)) {
                    log.debug("Skipping repeated header row: {}", firstCell.getStringCellValue());
                    continue; // 헤더 행이면 건너뜀
                }

                // 연속 빈 행 카운터 초기화
                consecutiveEmptyRows = 0;

                // 첫 번째 열(Year) 읽기
                Cell yearCell = row.getCell(0);
                if (yearCell == null || yearCell.getCellType() != CellType.NUMERIC)
                    continue;
                int year = (int) yearCell.getNumericCellValue();

                // 나머지 열(PeriodCode에 해당하는 값) 읽기
                for (int colIndex = 1; colIndex < row.getLastCellNum(); colIndex++) {
                    Cell valueCell = row.getCell(colIndex);
                    if (valueCell == null || colIndex - 1 >= periodCodes.size())
                        continue;

                    double value = valueCell.getNumericCellValue();
                    String periodCode = periodCodes.get(colIndex - 1);

                    // DB 저장을 위한 Object[] 생성: [Year, PeriodCode, Value]
                    Object[] rowData = new Object[] {year, periodCode, value};
                    result.add(rowData);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Exception during data collection: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse Excel file: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    log.warn("Failed to close workbook: {}", e.getMessage());
                }
            }
        }
    }

    public List<Object[]> collectPivotCsvData(MultipartFile file) {
        List<Object[]> result = new ArrayList<>();
        List<String> periodCodes = new ArrayList<>();
        String headerFirstCell = null;

        try (InputStream inputStream = file.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            // CSVFormat 설정 (헤더 자동 처리)
            CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader() // 첫 번째 행을 헤더로 설정
                    .setSkipHeaderRecord(true) // 헤더 행 건너뛰기
                    .get(); // build() 대신 get() 사용

            // CSVParser 생성
            try (CSVParser csvParser = CSVParser.parse(reader, csvFormat)) {
                // 헤더 추출
                List<String> headers = csvParser.getHeaderNames();
                if (headers == null || headers.isEmpty()) {
                    throw new IllegalStateException("CSV file is empty or has no headers");
                }

                headerFirstCell = headers.get(0) != null ? headers.get(0).trim() : null;
                periodCodes.addAll(headers.subList(1, headers.size()));

                // 데이터 행 처리
                int consecutiveEmptyRows = 0;
                for (CSVRecord row : csvParser) {
                    // 빈 행 체크
                    if (row.size() <= 1 || row.toList().stream().allMatch(cell -> cell == null || cell.trim().isEmpty())) {
                        consecutiveEmptyRows++;
                        log.debug("Empty row detected in CSV, consecutive empty rows: {}", consecutiveEmptyRows);
                        if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                            log.info("Terminating CSV data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);
                            break;
                        }
                        continue;
                    }

                    // 헤더 행인지 확인 (첫 번째 셀이 "Year"와 동일한 경우)
                    String firstCell = row.get(0) != null ? row.get(0).trim() : null;
                    if (firstCell != null && firstCell.equals(headerFirstCell)) {
                        log.debug("Skipping repeated header row in CSV: {}", firstCell);
                        continue;
                    }

                    // 연속 빈 행 카운터 초기화
                    consecutiveEmptyRows = 0;

                    // 첫 번째 열(Year) 읽기
                    if (firstCell == null || !firstCell.matches("\\d+"))
                        continue; // 숫자가 아닌 경우 건너뜀
                    int year = Integer.parseInt(firstCell);

                    // 나머지 열(PeriodCode에 해당하는 값) 읽기
                    for (int colIndex = 1; colIndex < row.size(); colIndex++) {
                        String valueStr = row.get(colIndex) != null ? row.get(colIndex).trim() : null;
                        if (valueStr == null || valueStr.isEmpty() || colIndex - 1 >= periodCodes.size())
                            continue;

                        double value = Double.parseDouble(valueStr);
                        String periodCode = periodCodes.get(colIndex - 1);

                        // DB 저장을 위한 Object[] 생성: [Year, PeriodCode, Value]
                        Object[] rowData = new Object[] {year, periodCode, value};
                        result.add(rowData);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Exception during CSV data collection: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse CSV file: " + e.getMessage());
        }
    }


    /**
     * CSV 파일에서 NINO SST 데이터를 수집하여 변환
     *
     * @param file CSV 파일
     * @param headerRowIndex 헤더 행 인덱스
     * @param dataStartRowIndex 데이터 시작 행 인덱스
     * @return 변환된 데이터 리스트 (YR, MON, sst_pstn_spt_cd, sst_idx, sst_avg_dvat)
     * @throws Exception 파일 처리 중 오류 발생 시
     */
    public List<Object[]> collectNinoRegionsSstCsvData(MultipartFile file, int headerRowIndex, int dataStartRowIndex) throws Exception {
        List<Object[]> result = new ArrayList<>();
        List<String> ninoRegions = List.of("NINO1+2", "NINO3", "NINO4", "NINO3.4");

        // 구분자 감지
        char delimiter = detectDelimiter(file);
        log.info("Detected CSV delimiter: '{}'", delimiter == ',' ? "comma" : delimiter == ';' ? "semicolon" : delimiter == '\t' ? "tab" : delimiter);

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = CSVParser.parse(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT)
                     .setDelimiter(delimiter)
                     .setTrim(true)
                     .setAllowMissingColumnNames(true)
                     .get())) {

            int currentRow = 0;
            int consecutiveEmptyRows = 0;

            for (CSVRecord record : csvParser) {
                // 헤더 행 건너뛰기
                if (currentRow < headerRowIndex) {
                    currentRow++;
                    continue;
                }

                // 데이터 시작 행까지 이동
                if (currentRow < dataStartRowIndex) {
                    currentRow++;
                    continue;
                }

                // 빈 행 체크
                if (record.size() <= 1 || record.toList().stream().allMatch(cell -> cell == null || cell.trim().isEmpty())) {
                    consecutiveEmptyRows++;
                    log.debug("Empty row detected in CSV, consecutive empty rows: {}", consecutiveEmptyRows);
                    if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                        log.info("Terminating CSV data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);
                        break;
                    }
                    currentRow++;
                    continue;
                }

                consecutiveEmptyRows = 0;

                // 데이터 파싱 (YR, MON, NINO1+2, ANOM, NINO3, ANOM, NINO4, ANOM, NINO3.4, ANOM)
                String yr = record.get(0);
                String mon = record.get(1);

                // 각 NINO 지역별로 데이터 변환
                for (int i = 0; i < ninoRegions.size(); i++) {
                    String ninoRegion = ninoRegions.get(i);
                    String sstValue = record.get(2 + i * 2); // NINO 값 (2, 4, 6, 8)
                    String anomValue = record.get(3 + i * 2); // ANOM 값 (3, 5, 7, 9)

                    // sst_idx 변환: 소수점 제거 (예: 24.28 -> 2428)
                    String sstIdx = String.valueOf((int) (Double.parseDouble(sstValue) * 100));

                    // 결과 행 생성: [YR, MON, sst_pstn_spt_cd, sst_idx, sst_avg_dvat]
                    Object[] rowData = new Object[]{yr, mon, ninoRegion, sstIdx, anomValue};
                    result.add(rowData);
                }

                currentRow++;
            }

            if (result.isEmpty()) {
                throw new IllegalStateException("No data found starting at index " + dataStartRowIndex);
            }

            return result;

        } catch (Exception e) {
            log.error("Exception during CSV data collection: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    /**
     * Excel 파일에서 NINO SST 데이터를 수집하여 변환
     *
     * @param file Excel 파일
     * @param headerRowIndex 헤더 행 인덱스
     * @param dataStartRowIndex 데이터 시작 행 인덱스
     * @return 변환된 데이터 리스트 (YR, MON, sst_pstn_spt_cd, sst_idx, sst_avg_dvat)
     * @throws Exception 파일 처리 중 오류 발생 시
     */
    public List<Object[]> collectNinoRegionsSstExcelData(MultipartFile file, int headerRowIndex, int dataStartRowIndex) throws Exception {
        List<Object[]> result = new ArrayList<>();
        List<String> ninoRegions = List.of("NINO1+2", "NINO3", "NINO4", "NINO3.4");

        Workbook workbook = null;
        try (InputStream is = file.getInputStream()) {
            String fileName = file.getOriginalFilename();

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(4096)
                        .open(is);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(is);
            } else {
                throw new IllegalStateException("Unsupported Excel file type: " + fileName);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 헤더 행까지 이동
            int currentRow = 0;
            while (rowIterator.hasNext() && currentRow < headerRowIndex) {
                rowIterator.next();
                currentRow++;
            }

            // 데이터 시작 행까지 이동
            while (rowIterator.hasNext() && currentRow < dataStartRowIndex) {
                rowIterator.next();
                currentRow++;
            }

            // 데이터 수집
            int consecutiveEmptyRows = 0;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row == null || row.getLastCellNum() <= 1) {
                    consecutiveEmptyRows++;
                    log.debug("Empty row detected, consecutive empty rows: {}", consecutiveEmptyRows);
                    if (consecutiveEmptyRows >= MAX_CONSECUTIVE_EMPTY_ROWS) {
                        log.info("Terminating data collection: {} consecutive empty rows reached", MAX_CONSECUTIVE_EMPTY_ROWS);
                        break;
                    }
                    continue;
                }

                consecutiveEmptyRows = 0;

                // 데이터 파싱 (YR, MON, NINO1+2, ANOM, NINO3, ANOM, NINO4, ANOM, NINO3.4, ANOM)
                Cell yrCell = row.getCell(0);
                Cell monCell = row.getCell(1);
                if (yrCell == null || monCell == null) {
                    continue;
                }

                String yr = String.valueOf((int) yrCell.getNumericCellValue());
                String mon = String.valueOf((int) monCell.getNumericCellValue());

                // 각 NINO 지역별로 데이터 변환
                for (int i = 0; i < ninoRegions.size(); i++) {
                    String ninoRegion = ninoRegions.get(i);
                    Cell sstCell = row.getCell(2 + i * 2); // NINO 값 (2, 4, 6, 8)
                    Cell anomCell = row.getCell(3 + i * 2); // ANOM 값 (3, 5, 7, 9)
                    if (sstCell == null || anomCell == null) {
                        continue;
                    }

                    double sstValue = sstCell.getNumericCellValue();
                    String anomValue = String.valueOf(anomCell.getNumericCellValue());

                    // sst_idx 변환: 소수점 제거 (예: 24.28 -> 2428)
                    String sstIdx = String.valueOf((int) (sstValue * 100));

                    // 결과 행 생성: [YR, MON, sst_pstn_spt_cd, sst_idx, sst_avg_dvat]
                    Object[] rowData = new Object[]{yr, mon, ninoRegion, sstIdx, anomValue};
                    result.add(rowData);
                }
            }

            if (result.isEmpty()) {
                throw new IllegalStateException("No data found starting at index " + dataStartRowIndex);
            }

            return result;

        } catch (Exception e) {
            log.error("Exception during data collection: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse Excel file: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    log.warn("Failed to close workbook: {}", e.getMessage());
                }
            }
        }
    }
}
